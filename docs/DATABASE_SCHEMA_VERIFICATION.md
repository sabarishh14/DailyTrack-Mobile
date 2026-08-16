# Database Schema & Mobile UI Verification Specification

This document outlines the complete **Neon PostgreSQL Database Schema** (derived from the active backend in `app.py`) and provides a comprehensive verification matrix confirming that all UI screens can successfully read (hydrate) and write (mutate) data.

---

## 1. Complete Database Schema (PostgreSQL / SQLAlchemy)

### 1.1 `accounts`
Tracks bank accounts, credit cards, and cash balances.
| Column | Type | Constraints | Description / UI Mapping |
| :--- | :--- | :--- | :--- |
| `account` | `VARCHAR(50)` | `PRIMARY KEY` | Account name (e.g. `HDFC`, `ICICI`, `CC-PINNACLE 6360`, `Cash`) |
| `balance` | `FLOAT` | `DEFAULT 0` | Calculated ledger balance based on transaction history |
| `real_balance` | `FLOAT` | `NULLABLE` | OCR / verified statement balance from bank screenshots |
| `balance_tracked` | `BOOLEAN` | `DEFAULT TRUE` | Whether this account updates net worth & totals |

---

### 1.2 `transactions`
Core financial ledger for income and expense transactions.
| Column | Type | Constraints | Description / UI Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY` | Timestamp-based ID (epoch milliseconds) |
| `account` | `VARCHAR(50)` | `FK -> accounts.account` | Bank or credit card account used |
| `date` | `DATE` | `NOT NULL`, `INDEXED` | Transaction date (`YYYY-MM-DD`) |
| `month` | `DATE` | `NOT NULL`, `INDEXED` | Month start date (`YYYY-MM-01`) for monthly aggregation |
| `type` | `VARCHAR(10)` | `NOT NULL` | `'Debit'`, `'Credit'`, or `'Savings'` |
| `heading` | `VARCHAR(100)` | `NOT NULL` | Category name (e.g. `Food`, `Bills`, `Shopping`, `Transport`) |
| `description` | `VARCHAR(255)` | `NULLABLE` | Merchant, payee, or note (e.g. `Swiggy Order`, `Uber`) |
| `amount` | `FLOAT` | `NOT NULL` | Transaction amount in INR |
| `synced` | `BOOLEAN` | `DEFAULT FALSE` | Sync status with Google Sheets |
| `exclude_analytics`| `BOOLEAN` | `DEFAULT FALSE` | If `true`, excluded from Spending Analyzer charts |
* *Constraint:* `UniqueConstraint('date', 'account', 'amount', 'heading', name='unique_tx')`

---

### 1.3 `splits`
Shared bill expense split among friends/members.
| Column | Type | Constraints | Description / UI Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY` | Unique split ID |
| `transaction_id` | `BIGINT` | `FK -> transactions.id`, `UNIQUE` | One-to-one link to parent transaction |
| `total_amount` | `FLOAT` | `NOT NULL` | Original bill total before split |
| `members` | `JSON` | `NOT NULL`, `DEFAULT []` | Array of objects: `[{"name": "...", "amount": 0.0, "paid": bool}]` |
| `created_at` | `DATETIME` | `DEFAULT utcnow` | Record creation timestamp |

---

### 1.4 `physical_activity`
Daily workout, sports, and fitness tracking.
| Column | Type | Constraints | Description / UI Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY` | Unique ID |
| `date` | `DATE` | `UNIQUE`, `NOT NULL` | Activity date (one record per day) |
| `gym` | `BOOLEAN` | `DEFAULT FALSE` | Gym session indicator |
| `badminton` | `BOOLEAN` | `DEFAULT FALSE` | Badminton indicator |
| `table_tennis` | `BOOLEAN` | `DEFAULT FALSE` | Table Tennis indicator |
| `cricket` | `BOOLEAN` | `DEFAULT FALSE` | Cricket indicator |
| `others` | `BOOLEAN` | `DEFAULT FALSE` | Other physical activities |
| `description` | `VARCHAR(255)` | `NULLABLE` | Notes (e.g. workout intensity, hours, sets) |

---

### 1.5 `manual_assets`
Fixed income, retirement, and custom assets.
| Column | Type | Constraints | Description / UI Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY` | Unique asset ID |
| `category` | `VARCHAR(50)` | `NOT NULL` | `FD`, `EPF`, `PPF`, `NPS`, `SGB`, `RSU`, `RealEstate`, `Cash` |
| `name` | `VARCHAR(100)` | `NOT NULL` | Asset title (e.g. `HDFC 1-Year FD`, `Company EPF`) |
| `invested_value`| `FLOAT` | `DEFAULT 0.0` | Principal invested amount |
| `current_value` | `FLOAT` | `DEFAULT 0.0` | Current valuation (compounded or manual) |
| `interest_rate` | `FLOAT` | `NULLABLE` | Annual interest rate percentage (e.g. `7.5`) |
| `start_date` | `DATE` | `NULLABLE` | Investment start date |
| `maturity_date` | `DATE` | `NULLABLE` | Investment maturity date |
| `last_updated` | `DATE` | `NOT NULL` | Date of last compounding or value update |

---

### 1.6 `recurring_tasks`
Automated monthly SIP / EPF recurring additions.
| Column | Type | Constraints | Description / UI Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY` | Unique task ID |
| `asset_name` | `VARCHAR(100)` | `NOT NULL` | Name matching `manual_assets.name` |
| `amount_to_add` | `FLOAT` | `NOT NULL` | Recurring addition amount (e.g. `3500.0`) |
| `interval_value`| `INTEGER` | `DEFAULT 1` | Interval frequency multiplier |
| `interval_unit` | `VARCHAR(10)` | `DEFAULT 'months'` | `'days'`, `'months'`, or `'years'` |
| `next_run_date` | `DATE` | `NOT NULL` | Scheduled date for next run |
| `is_active` | `BOOLEAN` | `DEFAULT TRUE` | Active toggle status |

---

### 1.7 `equity_holdings` & `mf_holdings`
Stock market and mutual fund investments.
| Column | Type | Constraints | Description / UI Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY` | Unique holding ID |
| `date` | `DATE` | `NOT NULL`, `INDEXED` | Holding snapshot date |
| `symbol` | `VARCHAR(100)` | `NOT NULL` | Stock ticker (e.g. `INFY`) or Scheme name |
| `quantity` | `FLOAT` | `NOT NULL` | Quantity of shares / units |
| `average_price` | `FLOAT` | `NOT NULL` | Weighted average purchase price |
| `ltp` / `nav` | `FLOAT` | `NOT NULL` | Last Traded Price (Equity) or NAV (Mutual Fund) |
| `invested_value`| `FLOAT` | `NOT NULL` | Total cost basis (`quantity * average_price`) |
| `current_value` | `FLOAT` | `NOT NULL` | Current market value (`quantity * ltp`) |

---

### 1.8 `portfolio_snapshots`
Historical net worth snapshots.
| Column | Type | Constraints | Description / UI Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY` | Snapshot ID |
| `date` | `DATE` | `NOT NULL`, `INDEXED` | Snapshot date |
| `total_equity_inv`, `total_equity_curr` | `FLOAT` | `DEFAULT 0.0` | Equity cost & valuation |
| `total_mf_inv`, `total_mf_curr` | `FLOAT` | `DEFAULT 0.0` | Mutual fund cost & valuation |
| `total_fixed_income_inv`, `total_fixed_income_curr` | `FLOAT` | `DEFAULT 0.0` | FD / Fixed income valuation |
| `total_provident_inv`, `total_provident_curr` | `FLOAT` | `DEFAULT 0.0` | EPF / PPF valuation |
| `total_gold_inv`, `total_gold_curr` | `FLOAT` | `DEFAULT 0.0` | Gold investment valuation |
| `grand_total_inv`, `grand_total_curr` | `FLOAT` | `DEFAULT 0.0` | Total portfolio valuation |

---

### 1.9 `movies` & `movie_diary_logs`
Entertainment library, diary, ratings, reviews, and theater visits.
* **`movies`:** `id`, `tmdb_id`, `name`, `poster_path`, `status` (`'WATCHED'`, `'TO WATCH'`), `runtime`, `release_year`, `added_on`.
* **`movie_diary_logs`:** `id`, `movie_id` (`FK -> movies.id`), `date`, `rating` (0.5 to 5.0), `review`, `liked` (bool), `rewatch` (bool), `tags` (e.g. `overall-theatres, theatres-2026, imax`), `created_at`.

---

## 2. UI Hydration & Mutation Verification Matrix

| Screen / Feature | UI Component | DB Tables Read (Hydration) | DB Tables Written (Mutation) | Verification Status |
| :--- | :--- | :--- | :--- | :---: |
| **Transactions List** | `TransactionsTab.kt` | `transactions`, `splits`, `accounts` | `transactions`, `splits`, `accounts` (balance auto-adjust) | ✅ Fully Supported |
| **Spending Analyzer** | `AnalysisTab.kt` | `transactions` (excluding `exclude_analytics=true`) | N/A (Read-only analytics) | ✅ Fully Supported |
| **Category Filters** | `FilterBottomSheet.kt` | `transactions.heading` (Distinct query) | N/A | ✅ Fully Supported |
| **Account Balances** | `MoneyScreen.kt` | `accounts` (`balance`, `real_balance`) | `accounts.balance` | ✅ Fully Supported |
| **Add Transaction** | `AddMoneyScreen.kt` | `accounts`, `transactions/categories` | `transactions` (insert), `accounts` (balance update) | ✅ Fully Supported |
| **Split Bill** | `SplitModal` | `splits`, `transactions` | `splits` (insert/update), `transactions.amount` (adjusted) | ✅ Fully Supported |
| **Physical Activity** | `ActivitiesScreen.kt` | `physical_activity` | `physical_activity` (upsert for selected date) | ✅ Fully Supported |
| **Portfolio Breakdown**| `InvestmentsScreen.kt`| `manual_assets`, `equity_holdings`, `mf_holdings`, `portfolio_snapshots` | `manual_assets`, `recurring_tasks` | ✅ Fully Supported |
| **Entertainment Diary**| `SabdekhoScreen.kt` | `movies`, `movie_diary_logs` | `movies`, `movie_diary_logs` | ✅ Fully Supported |
| **Nagapandi AI Chat** | `ChatDialog` | Read-only access across all tables via safe SQL | None | ✅ Fully Supported |

---

## 3. Data Transformations Handled on Mobile Client

To ensure clean interoperability between the Backend/PostgreSQL schema and Jetpack Compose UI:

1. **Date Formats:**
   * **Database:** `YYYY-MM-DD` strings (e.g. `2026-07-25`)
   * **Mobile UI:** Formats to `"25 Jul 2026"` for display, and passes `timestampMillis` for date range slider filtering.
2. **Transaction Types:**
   * **Database:** String values `"Debit"`, `"Credit"`, `"Savings"`
   * **Mobile UI:** Kotlin Enum `TransactionType.DEBIT`, `TransactionType.CREDIT`.
3. **Category Emojis & Accent Colors:**
   * **Database:** Stores heading string (e.g. `"Food"`, `"Bills"`, `"Transport"`).
   * **Mobile UI:** Maps headings to `ChartColors` palette (`ChartColors.Food`, `ChartColors.Bills`, etc.) and default emojis (`🍔`, `📺`, `⛽`).
4. **Account Balances:**
   * **Real Balance Priority:** When `real_balance` exists from OCR verification, mobile displays the verified balance with a badge; otherwise displays the ledger `balance`.
