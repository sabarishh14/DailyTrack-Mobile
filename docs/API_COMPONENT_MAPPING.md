# DailyTrack Mobile — API & Component Integration Specification

This document details every screen, UI component, user action, and submit button in the Android application along with the exact Flask/Neon backend API route, HTTP method, request payload, and expected response.

---

## 1. Money & Cash Flow (`MoneyScreen`)

### 1.1 Transactions Tab (`TransactionsTab.kt`)
* **Component:** `TransactionsTab`
* **Trigger:** Screen load / Pull-to-refresh / Month filter changed / Pagination scroll
* **API Route:** `GET /api/transactions`
* **Query Parameters:**
  * `limit`: `Int` (e.g. `50`, default `100`, max `500`)
  * `offset`: `Int` (e.g. `0`, `50`, `100`)
  * `month`: `String?` (Optional, format: `YYYY-MM`, e.g. `2026-07`)
* **Headers:** `X-API-KEY: <key>` or `Authorization: Bearer <token>`
* **Response Body:**
  ```json
  {
    "transactions": [
      {
        "id": 1721865600000,
        "account": "HDFC",
        "date": "2026-07-25",
        "month": "2026-07-01",
        "type": "Debit",
        "heading": "Food",
        "description": "Swiggy Order",
        "amount": 488.0,
        "exclude_analytics": false,
        "split": {
          "id": 12,
          "total_amount": 976.0,
          "members": [
            {"name": "Sai", "amount": 488.0, "paid": true},
            {"name": "Rahul", "amount": 488.0, "paid": false}
          ]
        }
      }
    ],
    "total": 1420,
    "limit": 50,
    "offset": 0,
    "hasMore": true
  }
  ```
* **UI Hydration:** Populates `MoneyState.transactions`, recalculates income/expense totals, and feeds `TransactionItem` list.

---

### 1.2 Spending Analyzer & Cash Flow (`AnalysisTab.kt`)
* **Component:** `AnalysisTab`
* **Trigger:** Tab selection / Active filters change
* **API Route:** Derived dynamically from `GET /api/transactions` (or cached local transactions)
* **Categories Query:** `GET /api/transactions/categories`
* **Response Body:**
  ```json
  {
    "success": true,
    "categories": ["Bills", "Cinema", "Daily Need", "Education", "Entertainment", "Food", "Health", "Investment", "Salary", "Shopping", "Transport"]
  }
  ```
* **UI Hydration:**
  * Populates `FilterBottomSheet` categories chip list.
  * Aggregates amounts by category for Pie / Donut Chart & Category breakdown bars.
  * Calculates `filteredTotalIncome`, `filteredTotalExpenses`, and Net Savings rate.

---

### 1.3 Bank & Accounts Bar (`MoneyScreen.kt` / `HomeScreen.kt`)
* **Component:** `AccountsCarousel` / `BalanceHeader`
* **Trigger:** Screen launch / Pull to refresh
* **API Route:** `GET /api/accounts`
* **Response Body:**
  ```json
  [
    {
      "account": "HDFC",
      "balance": 54230.50,
      "real_balance": 54230.50,
      "balance_tracked": true
    },
    {
      "account": "CC-PINNACLE 6360",
      "balance": 18450.00,
      "real_balance": null,
      "balance_tracked": true
    }
  ]
  ```
* **UI Hydration:** Renders bank balance cards and total liquid net worth.

---

### 1.4 Delete Transaction Action
* **Component:** `SwipeToDelete` / `TransactionDetailSheet`
* **Trigger:** User taps "Delete" icon on a transaction item
* **API Route:** `DELETE /api/transactions/<int:tid>`
* **Response Body:** `{"success": true}`
* **UI Handling:** Removes transaction from list, animates deletion, and updates cached account balance.

---

### 1.5 Bulk Delete & Bulk Edit
* **Component:** Multi-select Action Bar
* **Trigger:** User selects multiple transactions and taps "Delete Selected" or "Change Category"
* **API Routes:**
  * **Bulk Delete:** `POST /api/transactions/bulk-delete` with body: `[1721865600000, 1721865601000]`
  * **Bulk Edit:** `PUT /api/transactions/bulk-edit` with body: `[{"id": 1721865600000, "date": "2026-07-25", "type": "Debit", "heading": "Food", "account": "HDFC", "amount": 488.0}]`

---

## 2. Add / Edit Forms (`presentation/screens/forms/`)

### 2.1 Add Transaction Form (`AddMoneyScreen.kt`)
* **Submit Button:** `"Save Transaction"` (enabled when `amount.isNotBlank() && selectedCategory != null`)
* **API Route:** `POST /api/transactions`
* **Request Body (JSON):**
  ```json
  {
    "date": "2026-08-16",
    "type": "Debit",
    "heading": "Food",
    "description": "Lunch with team",
    "amount": 350.00,
    "account": "HDFC",
    "exclude_analytics": false
  }
  ```
  *(Supports single object or list `[ {...}, {...} ]` for multiple additions)*.
* **Response Body:**
  ```json
  {
    "success": true,
    "message": "Successfully added 1 transactions & updated balances!"
  }
  ```
* **Post-Submit Action:** Calls `onSaveSuccess()`, closes modal/sheet, refreshes transactions flow.

---

### 2.2 Add / Edit Split Form (`SplitModal`)
* **Submit Button:** `"Save Split"`
* **API Route:** `POST /api/splits`
* **Request Body (JSON):**
  ```json
  {
    "transaction_id": 1721865600000,
    "total_amount": 1000.0,
    "transaction_amount": 500.0,
    "members": [
      {"name": "Sai", "amount": 500.0, "paid": true},
      {"name": "Ravi", "amount": 500.0, "paid": false}
    ]
  }
  ```
* **Response Body:** `{"success": true, "split": {...}}`

---

### 2.3 Add Physical Activity Form (`AddActivityScreen.kt`)
* **Submit Button:** `"Log Activity"`
* **API Route:** `POST /api/physical`
* **Request Body (JSON):**
  ```json
  {
    "date": "2026-08-16",
    "gym": true,
    "badminton": false,
    "table_tennis": true,
    "cricket": false,
    "others": false,
    "description": "Chest & Triceps workout + 3 TT matches (Intense)"
  }
  ```
* **Response Body:** `{"success": true, "message": "Activity logged successfully"}`
* **Fetch Activity History:** `GET /api/physical`

---

### 2.4 Add Asset Form (`AddAssetScreen.kt`)
* **Submit Button:** `"Save Asset"`
* **API Route:** `POST /api/manual_assets`
* **Request Body (JSON):**
  ```json
  {
    "category": "FD",
    "name": "HDFC 1-Year Tax Saver FD",
    "invested_value": 100000.0,
    "current_value": 107500.0,
    "interest_rate": 7.5,
    "start_date": "2025-08-16",
    "maturity_date": "2026-08-16"
  }
  ```
* **Response Body:** `{"success": true, "id": 15}`
* **Fetch Manual Assets:** `GET /api/manual_assets`
* **Update Asset:** `PUT /api/manual_assets/<id>`
* **Delete Asset:** `DELETE /api/manual_assets/<id>`

---

### 2.5 Add Investment Form (`AddInvestmentScreen.kt`)
* **Submit Button:** `"Save Investment"`
* **API Routing Logic:**
  * If category is **Fixed Income / FD / PPF / NPS / Gold / Real Estate**: Routes to `POST /api/manual_assets`.
  * If recurring SIP (EPF / RD): Routes to `POST /api/cron/tasks` with body:
    ```json
    {
      "asset_name": "EPF",
      "amount_to_add": 3500.0,
      "interval_value": 1,
      "interval_unit": "months",
      "next_run_date": "2026-09-01"
    }
    ```

---

### 2.6 Add Movie / Show Log Form (`AddMovieScreen.kt`)
* **Submit Button:** `"Save to Diary"` / `"Add to Watchlist"`
* **API Routing Logic:**
  1. **Add Movie to Library:** `POST /api/movies`
     ```json
     {
       "tmdb_id": 157336,
       "name": "Interstellar",
       "poster_path": "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
       "status": "WATCHED",
       "year": 2014
     }
     ```
  2. **Add Diary Log (Review & Rating):** `POST /api/movies/diary`
     ```json
     {
       "movie_id": 42,
       "date": "2026-08-16",
       "rating": 5.0,
       "review": "Masterpiece rewatch in IMAX.",
       "liked": true,
       "rewatch": true,
       "tags": "overall-theatres, theatres-2026, imax"
     }
     ```
* **Fetch Movies Diary:** `GET /api/media/diary` or `GET /api/movies/diary`
* **Search TMDB Movies:** `GET /api/media/search?query=Interstellar`

---

## 3. Portfolio & Investments Screen (`InvestmentsScreen.kt`)

| Component | Trigger | API Route | Description |
| :--- | :--- | :--- | :--- |
| **Portfolio Breakdown Cards** | Screen Load | `GET /api/investments` | Aggregated holdings by asset class |
| **XIRR Metric Badge** | Screen Load | `GET /api/investments/xirr` | Annualized XIRR return calculation |
| **Net Worth Timeline Chart** | Screen Load | `GET /api/investments/history` | Historical timeline snapshots |
| **Equity Holdings List** | Equity Tab | `GET /api/equity` | Individual stock holdings & LTP |
| **Mutual Funds List** | MF Tab | `GET /api/investments/<date>/holdings` | NAV, invested vs current value |
| **Sync Kite Zerodha** | "Sync Kite" Button | `POST /api/sync/kite` | Fetches live Zerodha positions |
| **Run Auto-Compounding** | Pull-to-refresh | `POST /api/cron/process-recurring` | Calculates compound interest |

---

## 4. AI Assistant (`Nagapandi Chat`)

* **Component:** `ChatDialog` / FAB
* **Submit Button:** `"Send Query"`
* **API Route:** `POST /api/chat`
* **Request Body:**
  ```json
  {
    "query": "How much did I spend on food this month?"
  }
  ```
* **Response Body:**
  ```json
  {
    "success": true,
    "result": "You spent ₹4,800 on Food across 12 orders this month! 🍔"
  }
  ```
