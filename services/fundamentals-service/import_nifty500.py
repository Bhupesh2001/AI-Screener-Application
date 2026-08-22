import csv
import urllib.request
import jaydebeapi
import os
from pathlib import Path

# === Configuration ===
CSV_URL = "https://www.niftyindices.com/IndexConstituent/ind_nifty500list.csv"
DB_USER = "sa"
DB_PASSWORD = ""
DRIVER_CLASS = "org.h2.Driver"

# Auto-detect the backend directory
# This script is in services/fundamentals-service
# Project root is two levels up
script_dir = Path(__file__).parent.absolute()
project_root = script_dir.parent.parent  # go up two levels: services -> project root
backend_dir = project_root / "backend"
db_path = backend_dir / "data" / "stockresearch"
DB_URL = f"jdbc:h2:file:{db_path};AUTO_SERVER=TRUE"

print(f"Using database: {db_path}")

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
}

def find_h2_jar():
    home = Path.home()
    m2_repo = home / ".m2" / "repository" / "com" / "h2database" / "h2"
    if not m2_repo.exists():
        raise FileNotFoundError(f"Maven repository not found at {m2_repo}.")
    versions = [d for d in m2_repo.iterdir() if d.is_dir()]
    if not versions:
        raise FileNotFoundError(f"No H2 version found.")
    latest = sorted(versions, key=lambda d: d.name)[-1]
    jar_files = list(latest.glob("h2-*.jar"))
    if not jar_files:
        raise FileNotFoundError(f"No H2 JAR file found.")
    return str(jar_files[0])

def download_csv():
    print("Downloading Nifty 500 list...")
    try:
        req = urllib.request.Request(CSV_URL, headers=HEADERS)
        with urllib.request.urlopen(req, timeout=30) as response:
            return response.read().decode('utf-8')
    except Exception as e:
        print(f"Failed to download CSV: {e}")
        return None

def main():
    try:
        DRIVER_JAR = find_h2_jar()
        print(f"Using H2 JAR: {DRIVER_JAR}")
    except FileNotFoundError as e:
        print(f"ERROR: {e}")
        return

    csv_data = download_csv()
    if csv_data is None:
        return

    lines = csv_data.splitlines()
    reader = csv.reader(lines)
    try:
        header = next(reader)
    except StopIteration:
        print("CSV is empty.")
        return

    print("Connecting to H2 database...")
    try:
        conn = jaydebeapi.connect(DRIVER_CLASS, DB_URL, [DB_USER, DB_PASSWORD], DRIVER_JAR)
        cursor = conn.cursor()
    except Exception as e:
        print(f"Database connection failed: {e}")
        return

    # Check if table exists
    cursor.execute("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'COMPANY'")
    table_exists = cursor.fetchone()[0] > 0
    if not table_exists:
        print("ERROR: Table 'COMPANY' does not exist. Please run Spring Boot at least once to create the schema.")
        cursor.close()
        conn.close()
        return

    # Get existing symbols
    cursor.execute("SELECT symbol FROM company")
    existing_symbols = {row[0] for row in cursor.fetchall()}
    print(f"Found {len(existing_symbols)} existing companies.")

    companies = []
    for row in reader:
        if len(row) < 4:
            continue
        symbol = row[2].strip()
        if not symbol or symbol in existing_symbols:
            continue
        name = row[0].strip()
        industry = row[1].strip()
        sector = industry
        companies.append((symbol, name, "NSE", sector, industry))

    if not companies:
        print("No new companies to insert.")
        cursor.close()
        conn.close()
        return

    print(f"Preparing to insert {len(companies)} new companies...")

    batch_size = 100
    inserted = 0
    for i in range(0, len(companies), batch_size):
        batch = companies[i:i+batch_size]
        try:
            cursor.executemany(
                "INSERT INTO company (symbol, name, exchange, sector, industry, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                batch
            )
            inserted += len(batch)
            print(f"Inserted {inserted}/{len(companies)} companies...")
        except Exception as e:
            print(f"Batch insert failed at row {i}: {e}")
            conn.rollback()
            cursor.close()
            conn.close()
            return

    conn.commit()
    cursor.close()
    conn.close()
    print(f"Import complete: {inserted} new companies inserted.")

if __name__ == "__main__":
    main()