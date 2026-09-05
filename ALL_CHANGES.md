# DT-Mobile: Complete Project Changes Log

This document provides a comprehensive, chronological record of all changes, architectural improvements, and UI/UX refinements implemented in **DT-Mobile** throughout this entire pair programming session.

---

## Table of Contents
1. [Project Configuration & App Branding](#1-project-configuration--app-branding)
2. [Theme & Startup Flicker Fix](#2-theme--startup-flicker-fix)
3. [Home Screen Dashboard Overhaul](#3-home-screen-dashboard-overhaul)
4. [Add Money Screen UI/UX Improvements](#4-add-money-screen-uiux-improvements)
5. [Money Screen: Cash Flow & Spending Analyzer](#5-money-screen-cash-flow--spending-analyzer)
6. [Advanced Analysis Filter Bottom Sheet & Month-Year Picker](#6-advanced-analysis-filter-bottom-sheet--month-year-picker)
7. [Bulk Transaction Management (Multi-Select, Edit & Delete)](#7-bulk-transaction-management-multi-select-edit--delete)
8. [Single Transaction Edit Modal Sheet Revamp](#8-single-transaction-edit-modal-sheet-revamp)
9. [Files Modified & Added Summary](#9-files-modified--added-summary)

---

## 1. Project Configuration & App Branding

### User Requirement:
- Fix Android Studio run configuration error when opening project.
- Rename the application from `DailyTrack-Mobile` to `DT`.

### Key Changes:
- **`settings.gradle.kts` & `.idea/misc.xml`**: Fixed the root project mapping and module definitions so Android Studio detects the runnable app module out-of-the-box.
- **`app/src/main/res/values/strings.xml`**: Updated `app_name` from `"DailyTrack"` to `"DT"`.

---

## 2. Theme & Startup Flicker Fix

### User Requirement:
- When the app starts, prevent the brief flash of the default theme before switching to the user's custom theme (`dt_og`, `midnight`, `forest`, etc.).

### Key Changes:
- **[ThemeManager.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/data/local/datastore/ThemeManager.kt)**:
  - Improved theme initialization by pre-warming theme preference in synchronous memory cache before the first Compose composition occurs.
  - Aligned Android window background colors to match theme surface to eliminate transition flash.

---

## 3. Home Screen Dashboard Overhaul

### User Requirements:
- On app launch, default all dashboard sections to a clean minimized state.
- Obfuscate total balance on start (`₹XXXX`) and allow tap-to-reveal.
- Remove legacy "verified" badge/indicator logic completely; only display accounts where `balance_tracked == true`.
- Fix expanding bank balance cards that looked like they were breaking out of their container.
- Add shimmer skeleton loading cards for Investment Portfolio on startup.

### Key Changes:
- **[HomeScreen.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/home/HomeScreen.kt)**:
  - Total balance card defaults to hidden (`₹XXXX`) on initial render with tap-to-toggle eye icon and smooth text swap animation.
  - Dashboard sections start minimized by default.
  - Filtered bank accounts list to strictly include accounts where `balanceTracked == true`.
  - Fixed card expansion animation with proper clipping and layout bounds inside the container.
  - Added skeleton shimmer loader to `Investment Portfolio` card matching the total bank balance loader.

---

## 4. Add Money Screen UI/UX Improvements

### User Requirements:
- Overhaul account dropdown with modern card design.
- Enforce specific account ordering (`KOTAK`, `IDBI`, `FEDERAL`, `CUB`, `INDIAN`, `ICICI`, `HDFC`, `SBI`, `Axis`, `Cash`, `CC-PINNACLE 6360`).
- For the `dt_og` theme, style the selected Income type button with an organic green-ish tint (`#2ECC71`).
- Add smart floating description suggestions chips that sit above the soft keyboard based on recent description history.

### Key Changes:
- **[AddMoneyScreen.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/forms/AddMoneyScreen.kt)**:
  - Redesigned Account selector using `ExposedDropdownMenuBox` with leading bank icons and custom sorting logic.
  - Styled `Income` toggle with vibrant green `#2ECC71` highlights.
  - Integrated `WindowInsets.ime` padding and floating description suggestion chips right above the keyboard for one-tap autocomplete.
  - Enhanced category chip layout and search filtering.

---

## 5. Money Screen: Cash Flow & Spending Analyzer

### User Requirements:
- Default analysis filter to **"This Month"** and **"Expenses Only"** (removed "Last 30 Days").
- Database type mapping: Exclude `Savings` from Expenses Only (Debit transactions that are savings transfers shouldn't pollute pure expense data).
- Top 10 categories shown in the Donut Pie Chart; others grouped cleanly.
- Categories list below Donut: Remove clutter, add clean "Tap to show >" toggle, adjust spacing and compact padding.
- When tapping a category pill, make that category filter appear as the **FIRST** chip in Cash Flow.
- Added **"Last Month"** quick filter shortcut pill.
- Display exact figures with short format (`k`, `L`) and tap-to-toggle full values.
- Card loading shimmer while calculating spending stats.

### Key Changes:
- **[AnalysisTab.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/AnalysisTab.kt)**:
  - Filter chips row starts with the user-selected category pill first, followed by date and type chips.
  - Compact category breakdown layout with refined vertical spacing and tap-to-expand.
  - Formatted currency values with `k`/`L` abbreviations and click-to-expand exact values.
  - Donut chart supports up to top 10 categories with custom palette.
  - Loading shimmer state for analyzer cards.

---

## 6. Advanced Analysis Filter Bottom Sheet & Month-Year Picker

### User Requirements:
- Support selecting any historic month/year directly (e.g. June 2026) without having to manually pick start/end dates in a range picker.
- Include all 4 transaction types (`Debit`, `Credit`, `Savings`, `Investment`) in the filter modal.
- Display most-used categories as quick-select pills at the top of the category section.

### Key Changes:
- **[FilterBottomSheet.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/FilterBottomSheet.kt)**:
  - Top rounded modal bottom sheet (`topStart = 24.dp, topEnd = 24.dp`) with scrollable filter sections.
  - Type selector supporting Debit, Credit, Savings, Investment.
  - Quick-select pills for recent/top categories with emoji icons.
- **[MonthYearPickerDialog.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/components/MonthYearPickerDialog.kt)** (New Component):
  - Created a fast Month/Year grid dialog to jump directly to any month and year.

---

## 7. Bulk Transaction Management (Multi-Select, Edit & Delete)

### User Requirements:
- Long-press any transaction to enter selection mode.
- Allow multi-selecting transactions with a docked bottom action bar showing selected count, Select All / Deselect, Bulk Edit, and Bulk Delete.
- Integrate with Python backend `/api/transactions/bulk` and `/api/transactions/bulk/delete`.
- Bulk edit modal sheet with 2 tabs:
  1. **"Apply to All"**: Bulk modify category, account, type, or exclude analytics for all selected transactions simultaneously.
  2. **"Per Transaction"**: Review and fine-tune individual transaction details one-by-one.

### Key Changes:
- **Backend & Network Layer**:
  - **[DailyTrackApi.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/data/remote/api/DailyTrackApi.kt)**: Added `@POST("/api/transactions/bulk")` and `@POST("/api/transactions/bulk/delete")`.
  - **[TransactionDto.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/data/remote/dto/TransactionDto.kt)**: Added `BulkUpdateTransactionsRequestDto`, `TransactionUpdateItemDto`, `BulkDeleteTransactionsRequestDto`, `BulkActionResponseDto`.
  - **[MoneyRepository.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/data/repository/MoneyRepository.kt)**: Implemented `bulkUpdateTransactions` and `bulkDeleteTransactions`.
- **State & ViewModel**:
  - **[MoneyState.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/MoneyState.kt)**: Added `isSelectionMode`, `selectedTransactionIds`, `showBulkEditSheet`, `showBulkDeleteConfirm`, `isBulkUpdating`, `isBulkDeleting`.
  - **[MoneyAction.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/MoneyAction.kt)**: Added selection toggle, select all, clear selection, execute bulk edit/delete actions.
  - **[MoneyVM.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/MoneyVM.kt)**: Managed selection state and asynchronous bulk API calls with auto-refresh.
- **UI Components**:
  - **[TransactionsTab.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/TransactionsTab.kt)** & **[TransactionItem.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/TransactionItem.kt)**:
    - Long-press triggers selection mode.
    - Animated checkbox indicators on each transaction item.
    - Docked `BulkSelectionActionBar` flush with navigation bar.
  - **[BulkEditTransactionsSheet.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/BulkEditTransactionsSheet.kt)** (New Component):
    - Full-featured modal bottom sheet with tab switcher (`Apply to All` vs `Per Transaction`).
  - **[DeleteConfirmationDialog.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/DeleteConfirmationDialog.kt)**:
    - Added both single and bulk deletion confirmation alerts.

---

## 8. Single Transaction Edit Modal Sheet Revamp

### User Requirements:
- Replace the old basic dialog with a modern `ModalBottomSheet`.
- **Center the amount value**: Amount digits and `₹` currency symbol must be horizontally centered under the `AMOUNT` header, without shifting to the left.
- **Sheet starts from bottom of app**: The modal bottom sheet must extend all the way down to the bottom of the screen like `FilterBottomSheet`, without transparent gaps at the bottom.

### Key Changes:
- **[EditTransactionDialog.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/EditTransactionDialog.kt)**:
  - **Amount Centering**: Added `Modifier.width(IntrinsicSize.Min)` and `.defaultMinSize(minWidth = if (amount.isEmpty()) 64.dp else 16.dp)` to `BasicTextField` inside a `fillMaxWidth()` row with `Arrangement.Center`. The currency `₹` and the digits sit tightly side-by-side dead-center under `AMOUNT`.
  - **Card Tap-to-Focus**: Added `amountFocusRequester` so tapping anywhere on the hero amount card focuses the input and opens the numeric keypad.
  - **Full Edge-to-Edge Bottom Sheet**: Configured `ModalBottomSheet(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))` with no outer height constraints (inner `Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f))`), matching `FilterBottomSheet` so it extends all the way to the bottom edge of the screen.
  - **Restored Navigation Bar Stability**: Removed outer layout hacks in `MainScreen.kt` and `MoneyScreen.kt` so the navigation bar remains seamless when sheets open and close.

---

## 9. Bulk Edit Navbar & Selection Animation Fixes

### User Requirements:
- Fix navbar bleed-through / transparency underneath the Bulk Edit modal bottom sheet.
- Fix selection and deselection animation in the transactions list: the emoji moving back and forth to the left corner was not smooth and felt odd/jerky.

### Key Changes:
- **[BulkEditTransactionsSheet.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/BulkEditTransactionsSheet.kt)**:
  - Removed `modifier = Modifier.fillMaxHeight(0.92f)` directly from `ModalBottomSheet`, which was constraining the sheet surface and exposing the bottom navbar.
  - Added `shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)` to `ModalBottomSheet`.
  - Applied `.fillMaxWidth().fillMaxHeight(0.92f).navigationBarsPadding().imePadding()` to the inner `Column`.
  - Container now anchors edge-to-edge covering the navbar completely, matching `FilterBottomSheet` and `EditTransactionDialog`.
- **[TransactionItem.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/TransactionItem.kt)**:
  - Eliminated the 12dp layout snap on enter/exit by removing `Arrangement.spacedBy` across the animated boundary and embedding the trailing padding inside `AnimatedVisibility`.
  - Replaced bouncy spring animations with calibrated `FastOutSlowInEasing` and `LinearOutSlowInEasing` horizontal expansion/shrinkage anchored at `Alignment.Start`.
  - Added micro-animated checkmark pop (`scaleIn` / `scaleOut`) and animated container colors (`animateColorAsState`).
  - Added explicit spacers for the emoji, text column, and amount to ensure silky, 120Hz gliding without layout shifts.
- **[MoneyScreen.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/MoneyScreen.kt)**:
  - Added `BackHandler(enabled = state.isSelectionMode)` to allow users to smoothly dismiss multi-selection using the system back gesture.

---

## 10. Bulk Edit Modal Enhancements: Pickers & Keyboard Suggestions

### User Requirements:
- In "Apply to All" (Tab 0), categories were limited to a fixed set. Add the `+ More` chip with a search & custom creation popup dialog (matching `FilterBottomSheet` and `AddMoneyScreen`).
- In "Per Transaction" (Tab 1), replace the odd/clunky desktop `ExposedDropdownMenuBox` dropdown menus for Category and Account with native, mobile-first pickers consistent with the app's overall design system.
- Category, Account, and Date buttons in "Per Transaction" were unevenly sized. Make them equal-sized, uniform, and balanced.
- Replace the vertical list Account picker in both Bulk Edit and `AddMoneyScreen` with the modern `FlowRow` pill dialog style (search bar, custom creation, compact pill flow layout).
- For Description / Note fields (both in Tab 0 batch and Tab 1 individual cards), display contextual description suggestions docked directly above the soft keyboard with 1-tap autocomplete, exactly like `AddMoneyScreen`.

### Key Changes:
- **[BulkEditTransactionsSheet.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/BulkEditTransactionsSheet.kt)**:
  - **CategoryPickerDialog & AccountPickerDialog**:
    - Built reusable native Material 3 alert pickers with instant search filtering, clear buttons, category emojis, bank balance icons, dynamic custom item creation (`Add "<query>"`), and compact `FlowRow` pill grid.
    - Replaced vertical account row lists with modern flow chips matching the category picker dialog.
  - **Tab 0 ("Apply to All") Enhancements**:
    - Added `+ More` chip to Category `FlowRow` opening `CategoryPickerDialog` with full category list.
    - Added `+ More` chip to Account `FlowRow` opening `AccountPickerDialog` with full bank accounts list.
    - Added dedicated **Batch Description / Note** card allowing one-tap bulk note updates.
  - **Tab 1 ("Per Transaction") Redesign & Uniform Button Sizing**:
    - Replaced `ExposedDropdownMenuBox` with sleek clickable `Surface` buttons showing emoji/icon, label, and subtle down-arrows.
    - **Uniform Sizing**: Set strictly equal weights (`Modifier.weight(1f)` for a balanced 1:1:1 horizontal split), uniform height (`36.dp`), and matching chevron icons across Category, Account, and Date buttons.
  - **Docked Keyboard Suggestion Accessory Bar**:
    - Wired `onFocusChanged` to both Tab 0 batch description and Tab 1 note fields.
    - Container `.imePadding()` floats the suggestions bar seamlessly right above the keyboard keys.
    - Displays `SUGGESTIONS • 1-tap to fill`, a `Done` dismiss button, and horizontally scrollable suggestion chips with `Schedule` icons.
    - Tapping any suggestion instantly populates the active note and clears focus cleanly.
- **[AddMoneyScreen.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/forms/AddMoneyScreen.kt)**:
  - Replaced the legacy `ModalBottomSheet` vertical account list with the modern `FlowRow` pill-based `AlertDialog` featuring search filtering, custom account creation, and bank balance icons.
- **[MoneyScreen.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/MoneyScreen.kt)**:
  - Extracted recent transaction notes and descriptions from `state.transactions` and passed them down to `BulkEditTransactionsSheet` as `recentDescriptions`.

---

## 11. Single Transaction Edit Dialog Modernization (Account & Category)

- **[EditTransactionDialog.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/EditTransactionDialog.kt)**:
  - **Account Selector Upgrade**:
    - Replaced the clunky legacy `ExposedDropdownMenuBox` with a sleek, clickable `Surface` showing the bank balance icon (`Icons.Default.AccountBalance`), active account name, and subtle down-arrow dropdown indicator.
    - Added a horizontal quick-select row with the top accounts and an explicit `+ More` chip.
    - Tapping either the account box or the `+ More` chip launches the unified `AccountPickerDialog` (searchable `FlowRow` pill dialog with bank balance icons and custom account creation).
  - **Category Selector Upgrade**:
    - Replaced the legacy `ExposedDropdownMenuBox` with an elegant clickable `Surface` displaying the category emoji, active category name, and subtle down-arrow indicator.
    - Added a `+ More` chip to the horizontal recent categories row.
    - Tapping either the category surface or `+ More` launches the unified `CategoryPickerDialog` (searchable `FlowRow` pill dialog with emojis and custom category creation).
  - **Description / Note Autocomplete**:
    - Added instant horizontally scrollable suggestion chips (`Lunch`, `Dinner`, `Snacks`, `Coffee`, `Groceries`, `Uber`, `Fuel`, `Shopping`, `Subscription`, `Bill`) above the description field.
    - Added a trailing clear button (`Icons.Default.Clear`) to the description text field when not empty.

---

## 12. Unified Account Ordering Across the App

### User Requirement:
- Enforce the exact same specific account order established in Add Money across:
  1. Single Transaction Edit (`EditTransactionDialog`)
  2. Bulk Transaction Edit (`BulkEditTransactionsSheet` Tab 0, Tab 1, and `AccountPickerDialog`)
  3. Filters: Accounts & Banks section (`FilterBottomSheet` & `AdvancedFilterSection`)
  4. Active account filter chips (`TransactionsTab` & `AnalysisTab`)

### Canonical Order Enforced:
1. `Cash`
2. `KOTAK`
3. `IDBI`
4. `FEDERAL`
5. `CUB`
6. `INDIAN`
7. `ICICI`
8. `HDFC`
9. `SBI`
10. `Axis`
11. `CC-PINNACLE 6360`
*(Followed alphabetically by any custom or additional accounts)*

### Key Changes:
- **[MoneyState.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/MoneyState.kt)**:
  - Added centralized `DEFAULT_CANONICAL_ACCOUNTS` and `sortAccountsCanonical(accounts: List<String>)` helper.
  - Updated `allAvailableAccounts` getter to always return accounts sorted by the canonical order.
- **[EditTransactionDialog.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/EditTransactionDialog.kt)**:
  - Switched `defaultAccounts` to `DEFAULT_CANONICAL_ACCOUNTS`.
  - Updated `accountsList` to sort using `sortAccountsCanonical(list)`. Quick-select horizontal chips and the picker dialog now display accounts in the exact canonical order.
- **[BulkEditTransactionsSheet.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/BulkEditTransactionsSheet.kt)**:
  - Switched `defaultAccounts` to `DEFAULT_CANONICAL_ACCOUNTS`.
  - Updated `accountsList` to sort using `sortAccountsCanonical(list)`.
  - In `AccountPickerDialog`, wrapped the accounts list with `sortAccountsCanonical(...)` ensuring all dialog instances (batch and per-transaction) follow the exact canonical order.
- **[FilterBottomSheet.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/FilterBottomSheet.kt)**:
  - Passed `sortAccountsCanonical(allAccounts)` to `AdvancedFilterSection(title = "Accounts & Banks", ...)` so that both the quick filter chips and the search/filter modal display accounts in the exact canonical order.
- **[TransactionsTab.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/TransactionsTab.kt) & [AnalysisTab.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/AnalysisTab.kt)**:
  - Sorted active account filter chips in horizontal strips using `sortAccountsCanonical`.

---

## 14. Modal Sheet Height Reduction & Swipe-Up Glitch Fix

### User Requirement:
- When swiping up `EditTransactionDialog` or `BulkEditTransactionsSheet` to the top of the screen, the sheets started glitching/jittering.
- In contrast, `FilterBottomSheet` felt very nice and firmly anchored without over-scrolling past a comfortable point ("we can't scroll upon a point").
- Reduce the height of both `EditTransactionDialog` and `BulkEditTransactionsSheet` to match `FilterBottomSheet`.

### Root Cause Analysis:
1. **Status Bar Collision Loop**: At tall heights (`0.86f` - `0.92f`), dragging the sheet upward caused its top edge to push into the status bar region. On edge-to-edge Android devices, `WindowInsets` re-measured repeatedly on each drag frame, creating an inset layout oscillation loop (rapid visual glitch/flicker).
2. **Duplicate Inset Padding**: `BulkEditTransactionsSheet` had `.navigationBarsPadding()` applied on its inner content container in addition to `ModalBottomSheet`'s native window insets, exacerbating the jitter during drag gestures.

### Key Changes:
- **[EditTransactionDialog.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/EditTransactionDialog.kt)**:
  - Reduced content height from `0.86f` to strictly `fillMaxHeight(0.79f)`, perfectly matching `FilterBottomSheet.kt` and `TransactionDetailBottomSheet.kt`.
  - The sheet now firmly anchors at 79% of the screen height with ~21% top headroom, entirely preventing status bar collision and nested drag glitching.
- **[BulkEditTransactionsSheet.kt](file:///d:/SB/DEV/DT-Mobile/app/src/main/java/com/example/dailytrack_mobile/presentation/screens/money/components/BulkEditTransactionsSheet.kt)**:
  - Reduced container height from `0.92f` to `fillMaxHeight(0.79f)`.
  - Removed redundant `.navigationBarsPadding()`, while preserving `.imePadding()` for soft keyboard typing and the docked suggestion accessory bar.
  - When swiping up, both tabs ("Apply to All" and "Per Transaction") scroll internally with butter-smooth physics while the sheet stays solidly anchored at 0.79f without over-scrolling.

---

## 15. Files Modified & Added Summary

| File Path | Status | Key Focus Area |
|---|---|---|
| `settings.gradle.kts` | Modified | Root project mapping and IDE sync |
| `.idea/misc.xml` | Modified | Android Studio run configuration |
| `app/src/main/res/values/strings.xml` | Modified | Updated app name to "DT" |
| `data/local/datastore/ThemeManager.kt` | Modified | Eliminated theme splash flicker |
| `presentation/screens/home/HomeScreen.kt` | Modified | Minimized start, hidden balance (`₹XXXX`), verified badge removal |
| `presentation/screens/forms/AddMoneyScreen.kt` | Modified | Account order, green income toggle, FlowRow AccountPickerDialog |
| `presentation/screens/money/components/AnalysisTab.kt` | Modified | First active filter chip, top 10 donut, compact category list, canonical account chips |
| `presentation/screens/money/components/FilterBottomSheet.kt` | Modified | Type selector, recent category pills, canonical Accounts & Banks order, 0.79f height |
| `presentation/components/MonthYearPickerDialog.kt` | **New** | Direct Month & Year picker dialog |
| `data/remote/api/DailyTrackApi.kt` | Modified | Added bulk update & bulk delete endpoints |
| `data/remote/dto/TransactionDto.kt` | Modified | Added bulk update/delete request & response DTOs |
| `data/repository/MoneyRepository.kt` | Modified | Implemented bulk update & bulk delete API calls |
| `presentation/screens/money/MoneyState.kt` | Modified | Added selection mode & bulk dialog states; Centralized canonical accounts & sorting |
| `presentation/screens/money/MoneyAction.kt` | Modified | Added multi-selection and bulk actions |
| `presentation/screens/money/MoneyVM.kt` | Modified | Added selection logic and bulk network execution |
| `presentation/screens/money/components/TransactionItem.kt` | Modified | Butter-smooth emoji glide, checkmark scale animation, zero layout snaps |
| `presentation/screens/money/components/TransactionsTab.kt` | Modified | Multi-select state, docked `BulkSelectionActionBar`, canonical active account chips |
| `presentation/screens/money/components/BulkEditTransactionsSheet.kt` | **New** | 2-tab bulk edit sheet; Category & Account dialog pickers; Docked keyboard suggestions; Canonical account order; Height reduced to 0.79f & anti-glitch swipe stabilization |
| `presentation/screens/money/components/DeleteConfirmationDialog.kt` | Modified | Single & bulk transaction deletion confirmation dialog |
| `presentation/screens/money/components/EditTransactionDialog.kt` | Modified | Single edit sheet with FlowRow dialog pickers for Account and Category, note suggestions, canonical account order; Height reduced to 0.79f |
| `presentation/screens/money/MoneyScreen.kt` | Modified | BackHandler for selection, recent descriptions extraction, coordinated sheets |
| `presentation/screens/main/MainScreen.kt` | Modified | Cleaned up navigation and FAB coordination |

---
*Generated for Sabarish — DT-Mobile Android Workspace*
