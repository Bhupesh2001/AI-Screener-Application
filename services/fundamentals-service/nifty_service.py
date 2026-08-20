from flask import Flask, jsonify
from flask_cors import CORS
import yfinance as yf
import requests
from bs4 import BeautifulSoup
import pandas as pd
from concurrent.futures import ThreadPoolExecutor, as_completed
import time
import logging
import re
from functools import lru_cache

app = Flask(__name__)
CORS(app)

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# === Nifty 500 List ===
NIFTY500_URL = "https://www.niftyindices.com/IndexConstituent/ind_nifty500list.csv"
SCREENER_BASE_URL = "https://www.screener.in/company/"

# Headers to mimic a browser
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
    "Connection": "keep-alive",
}

def get_nifty500_symbols():
    """Fetch the official Nifty 500 stock symbols from NSE."""
    try:
        df = pd.read_csv(NIFTY500_URL)
        symbols = df['Symbol'].tolist()
        logger.info(f"Fetched {len(symbols)} symbols from Nifty 500 list")
        return symbols
    except Exception as e:
        logger.error(f"Error fetching Nifty 500 list: {e}")
        return []

# === Screener.in Scraping (for shareholding) ===

@lru_cache(maxsize=1000)
def fetch_shareholding_from_screener(symbol):
    """
    Scrape Screener.in for Promoter and Institutional Holding percentages.
    Cached to avoid repeated calls for the same symbol in a batch.
    Returns (promoter_pct, institutional_pct) or (None, None) on failure.
    """
    try:
        url = f"{SCREENER_BASE_URL}{symbol}/"
        response = requests.get(url, headers=HEADERS, timeout=15)
        if response.status_code != 200:
            logger.warning(f"Screener.in returned {response.status_code} for {symbol}")
            return None, None

        soup = BeautifulSoup(response.text, 'html.parser')
        promoter_holding = None
        institutional_holding = None

        # Find the shareholding pattern table
        shareholding_section = soup.find('section', id='shareholding')
        if not shareholding_section:
            # Try alternative: find a table with shareholding-related text
            tables = soup.find_all('table')
            for table in tables:
                if 'shareholding' in str(table).lower():
                    shareholding_section = table
                    break

        if shareholding_section:
            # If it's a section, find the table inside; if it's already a table, use it
            table = shareholding_section.find('table') if hasattr(shareholding_section, 'find') and shareholding_section.name != 'table' else shareholding_section
            if table:
                rows = table.find_all('tr')
                for row in rows:
                    cols = row.find_all('td')
                    if len(cols) >= 2:
                        label = cols[0].text.strip().lower()
                        value_text = cols[-1].text.strip().replace('%', '').replace(',', '').strip()
                        try:
                            value = float(value_text) if value_text else None
                        except ValueError:
                            value = None

                        if 'promoter' in label:
                            promoter_holding = value
                        elif 'institutional' in label or 'foreign institutions' in label or 'dii' in label:
                            institutional_holding = value

        # If still None, try to find using common patterns in the page
        if promoter_holding is None:
            promoter_text = soup.find(string=re.compile(r'Promoter.*Holding', re.IGNORECASE))
            if promoter_text:
                parent = promoter_text.find_parent('td') or promoter_text.find_parent('th')
                if parent:
                    next_td = parent.find_next('td')
                    if next_td:
                        try:
                            promoter_holding = float(next_td.text.strip().replace('%', ''))
                        except:
                            pass

        if institutional_holding is None:
            inst_text = soup.find(string=re.compile(r'Institutional.*Holding|FII.*Holding|DII.*Holding', re.IGNORECASE))
            if inst_text:
                parent = inst_text.find_parent('td') or inst_text.find_parent('th')
                if parent:
                    next_td = parent.find_next('td')
                    if next_td:
                        try:
                            institutional_holding = float(next_td.text.strip().replace('%', ''))
                        except:
                            pass

        # If we still don't have promoter, maybe we can get it from the "Promoter & Promoter Group" row
        if promoter_holding is None:
            for row in soup.select('table tr'):
                cols = row.find_all('td')
                if len(cols) >= 2 and 'promoter' in cols[0].text.lower():
                    try:
                        promoter_holding = float(cols[-1].text.strip().replace('%', ''))
                        break
                    except:
                        pass

        return promoter_holding, institutional_holding

    except Exception as e:
        logger.error(f"Screener scrape failed for {symbol}: {e}")
        return None, None

def fetch_one_stock(symbol):
    """
    Fetch ALL required data for a single stock.
    Financials from Yahoo Finance, shareholding from Screener.in.
    Returns dict with success, data_complete, missing_fields, and data.
    """
    result = {
        'symbol': symbol,
        'success': False,
        'data_complete': False,
        'missing_fields': [],
        'data': {},
        'error': None
    }

    try:
        # 1. Financials from Yahoo Finance
        ticker = yf.Ticker(f"{symbol}.NS")
        info = ticker.info

        revenue_growth = info.get('revenueGrowth')
        profit_growth = info.get('profitGrowth')
        operating_margin = info.get('operatingMargin')
        debt_to_equity = info.get('debtToEquity')
        roce = info.get('returnOnCapitalEmployed')
        roe = info.get('returnOnEquity')

        # 2. Shareholding from Screener.in
        promoter, institutional = fetch_shareholding_from_screener(symbol)

        # Convert percentages to values (Yahoo returns decimals for some fields)
        data = {
            'revenueGrowthPct': revenue_growth * 100 if revenue_growth else None,
            'profitGrowthPct': profit_growth * 100 if profit_growth else None,
            'operatingMarginPct': operating_margin * 100 if operating_margin else None,
            'debtToEquity': debt_to_equity,
            'roce': roce * 100 if roce else None,
            'roe': roe * 100 if roe else None,
            'promoterHoldingPct': promoter,
            'institutionalHoldingPct': institutional,
        }

        result['data'] = data
        result['success'] = True

        # Strict data completeness check (all 8 fields must be non-null)
        critical_fields = [
            ('revenueGrowthPct', data['revenueGrowthPct']),
            ('profitGrowthPct', data['profitGrowthPct']),
            ('operatingMarginPct', data['operatingMarginPct']),
            ('debtToEquity', data['debtToEquity']),
            ('roce', data['roce']),
            ('roe', data['roe']),
            ('promoterHoldingPct', data['promoterHoldingPct']),
            ('institutionalHoldingPct', data['institutionalHoldingPct'])
        ]

        missing = [field for field, value in critical_fields if value is None]
        if missing:
            result['missing_fields'] = missing
        else:
            result['data_complete'] = True

        return result

    except Exception as e:
        logger.error(f"Error processing {symbol}: {e}")
        result['error'] = str(e)
        return result

# === API Endpoints ===

@app.route('/api/fundamentals/<symbol>', methods=['GET'])
def get_fundamentals_single(symbol):
    """Single-stock endpoint for backward compatibility."""
    result = fetch_one_stock(symbol)
    if result['success']:
        return jsonify(result['data'])
    else:
        return jsonify({'error': result.get('error', 'Unknown error')}), 500

@app.route('/api/nifty500/refresh', methods=['GET'])
def refresh_nifty500():
    """
    Batch refresh all Nifty 500 stocks.
    Uses concurrency with rate limiting to avoid IP bans from Screener.in.
    """
    symbols = get_nifty500_symbols()
    if not symbols:
        return jsonify({'error': 'Failed to fetch Nifty 500 list'}), 500

    logger.info(f"Starting refresh for {len(symbols)} Nifty 500 stocks using Yahoo Finance + Screener.in...")
    results = []
    complete_count = 0

    # Use 5 concurrent workers to balance speed and rate limiting
    # Add a delay of 0.5 seconds per batch to avoid triggering Screener.in's rate limits
    with ThreadPoolExecutor(max_workers=5) as executor:
        future_to_symbol = {executor.submit(fetch_one_stock, sym): sym for sym in symbols}
        for future in as_completed(future_to_symbol):
            res = future.result()
            results.append(res)
            if res.get('data_complete'):
                complete_count += 1
            # Delay to stay friendly to Screener.in
            time.sleep(0.5)

    logger.info(f"Refresh complete. {complete_count} stocks passed the strict data gate out of {len(symbols)}.")
    return jsonify({
        'total_processed': len(symbols),
        'complete_data_stocks': complete_count,
        'pending_stocks': len(symbols) - complete_count,
        'data': results
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5004, threaded=True)