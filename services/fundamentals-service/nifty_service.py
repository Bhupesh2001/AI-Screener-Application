from flask import Flask, jsonify
from flask_cors import CORS
import yfinance as yf

app = Flask(__name__)
CORS(app)

@app.route('/api/fundamentals/<symbol>')
def get_fundamentals(symbol):
    try:
        ticker = yf.Ticker(f"{symbol}.NS")
        info = ticker.info
        print(f"{symbol} debtToEquity: {info.get('debtToEquity')}")
        data = {
            'revenueGrowthPct': info.get('revenueGrowth'),
            'profitGrowthPct': info.get('profitGrowth'),
            'operatingMarginPct': info.get('operatingMargin'),
            'debtToEquity': info.get('debtToEquity'),
            'roce': info.get('returnOnCapitalEmployed'),
            'roe': info.get('returnOnEquity'),
            'promoterHoldingPct': None,          # yfinance doesn't provide
            'institutionalHoldingPct': None,     # yfinance doesn't provide
        }
        return jsonify(data)
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5004)