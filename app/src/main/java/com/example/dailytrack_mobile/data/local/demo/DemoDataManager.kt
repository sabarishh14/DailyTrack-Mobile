package com.example.dailytrack_mobile.data.local.demo

import android.content.Context
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.remote.dto.*
import com.example.dailytrack_mobile.data.repository.FullPortfolioData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class DemoStorageContainer(
    val accounts: List<AccountDto>,
    val transactions: List<TransactionDto>,
    val categories: List<String>,
    val snapshots: List<PortfolioSnapshotDto>,
    val equityHoldings: List<EquityHoldingDto>,
    val mutualFundHoldings: List<MutualFundHoldingDto>,
    val manualAssets: List<ManualAssetDto>,
    val physicalActivities: List<PhysicalActivityDto>,
    val mediaShows: List<MediaShowDto>
)

@Singleton
class DemoDataManager @Inject constructor(
    private val context: Context,
    private val demoModeManager: DemoModeManager
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val containerAdapter = moshi.adapter(DemoStorageContainer::class.java)
    private val dataFile = File(context.filesDir, "dailytrack_demo_storage.json")

    private var inMemoryData: DemoStorageContainer? = null
    private val idGenerator = AtomicLong(System.currentTimeMillis())

    private val _dataUpdateFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val dataUpdateFlow: SharedFlow<Unit> = _dataUpdateFlow.asSharedFlow()

    suspend fun isDemoModeEnabled(): Boolean {
        return demoModeManager.isDemoModeEnabled()
    }

    private suspend fun getOrLoadContainer(): DemoStorageContainer = withContext(Dispatchers.IO) {
        inMemoryData?.let { return@withContext it }

        if (dataFile.exists()) {
            try {
                val json = dataFile.readText()
                val parsed = containerAdapter.fromJson(json)
                if (parsed != null && parsed.accounts.isNotEmpty()) {
                    inMemoryData = parsed
                    return@withContext parsed
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Initialize with default seeds
        val initial = generateSeedData()
        inMemoryData = initial
        saveContainer(initial)
        initial
    }

    private suspend fun saveContainer(container: DemoStorageContainer) = withContext(Dispatchers.IO) {
        inMemoryData = container
        try {
            val json = containerAdapter.toJson(container)
            dataFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _dataUpdateFlow.tryEmit(Unit)
    }

    fun notifyDataUpdated() {
        _dataUpdateFlow.tryEmit(Unit)
    }

    suspend fun resetDemoData(): DemoStorageContainer = withContext(Dispatchers.IO) {
        val initial = generateSeedData()
        saveContainer(initial)
        initial
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Money / Accounts / Transactions
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun getAccounts(): List<AccountDto> {
        return getOrLoadContainer().accounts
    }

    suspend fun getTransactions(
        limit: Int = 100,
        offset: Int = 0,
        month: String? = null
    ): TransactionsResponseDto {
        val all = getOrLoadContainer().transactions
        val filtered = if (month != null) {
            all.filter { it.month == month || it.date.startsWith(month) }
        } else {
            all
        }
        val paged = filtered.drop(offset).take(limit)
        return TransactionsResponseDto(
            transactions = paged,
            total = filtered.size,
            limit = limit,
            offset = offset,
            hasMore = offset + limit < filtered.size
        )
    }

    suspend fun getCategories(): List<String> {
        return getOrLoadContainer().categories
    }

    suspend fun addTransaction(
        type: String, // "Debit" or "Credit"
        category: String,
        amount: Double,
        note: String?,
        accountName: String,
        date: String, // "yyyy-MM-dd"
        excludeAnalytics: Boolean
    ): TransactionDto = withContext(Dispatchers.IO) {
        val current = getOrLoadContainer()
        val monthStr = date.take(7) // "yyyy-MM"

        val newTx = TransactionDto(
            id = idGenerator.incrementAndGet(),
            account = accountName,
            date = date,
            month = monthStr,
            type = if (type.equals("Credit", ignoreCase = true) || type.equals("Income", ignoreCase = true)) "Credit" else "Debit",
            heading = category,
            description = note,
            amount = amount,
            excludeAnalytics = excludeAnalytics,
            split = null
        )

        // Update account balances
        val updatedAccounts = current.accounts.map { acc ->
            if (acc.account.equals(accountName, ignoreCase = true)) {
                val delta = if (newTx.type == "Credit") amount else -amount
                val newBal = acc.balance + delta
                val newRealBal = acc.realBalance?.let { it + delta } ?: newBal
                acc.copy(balance = newBal, realBalance = newRealBal)
            } else {
                acc
            }
        }

        val updatedTransactions = listOf(newTx) + current.transactions
        val updatedCategories = if (category.isNotBlank() && !current.categories.any { it.equals(category.trim(), ignoreCase = true) }) {
            current.categories + category.trim()
        } else {
            current.categories
        }
        saveContainer(
            current.copy(
                accounts = updatedAccounts,
                transactions = updatedTransactions,
                categories = updatedCategories
            )
        )
        newTx
    }

    suspend fun updateTransaction(
        id: Long,
        type: String,
        category: String,
        amount: Double,
        note: String?,
        accountName: String,
        date: String,
        excludeAnalytics: Boolean
    ): TransactionDto = withContext(Dispatchers.IO) {
        val current = getOrLoadContainer()
        val oldTx = current.transactions.find { it.id == id }
            ?: throw IllegalArgumentException("Transaction not found with id: $id")

        val monthStr = date.take(7)
        val normalizedType = if (type.equals("Credit", ignoreCase = true) || type.equals("Income", ignoreCase = true)) "Credit" else "Debit"

        val updatedTx = oldTx.copy(
            account = accountName,
            date = date,
            month = monthStr,
            type = normalizedType,
            heading = category,
            description = note,
            amount = amount,
            excludeAnalytics = excludeAnalytics
        )

        // 1. Revert old transaction effect on old account
        // 2. Apply new transaction effect on new account
        val updatedAccounts = current.accounts.map { acc ->
            var bal = acc.balance
            var realBal = acc.realBalance

            // Revert old
            if (acc.account.equals(oldTx.account, ignoreCase = true)) {
                val oldDelta = if (oldTx.type == "Credit") -oldTx.amount else oldTx.amount
                bal += oldDelta
                realBal = realBal?.let { it + oldDelta }
            }

            // Apply new
            if (acc.account.equals(accountName, ignoreCase = true)) {
                val newDelta = if (normalizedType == "Credit") amount else -amount
                bal += newDelta
                realBal = realBal?.let { it + newDelta }
            }

            acc.copy(balance = bal, realBalance = realBal)
        }

        val updatedTransactions = current.transactions.map { if (it.id == id) updatedTx else it }
        val updatedCategories = if (category.isNotBlank() && !current.categories.any { it.equals(category.trim(), ignoreCase = true) }) {
            current.categories + category.trim()
        } else {
            current.categories
        }

        saveContainer(
            current.copy(
                accounts = updatedAccounts,
                transactions = updatedTransactions,
                categories = updatedCategories
            )
        )
        updatedTx
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        val current = getOrLoadContainer()
        val oldTx = current.transactions.find { it.id == id } ?: return@withContext

        // Revert balance
        val updatedAccounts = current.accounts.map { acc ->
            if (acc.account.equals(oldTx.account, ignoreCase = true)) {
                val oldDelta = if (oldTx.type == "Credit") -oldTx.amount else oldTx.amount
                val newBal = acc.balance + oldDelta
                val newRealBal = acc.realBalance?.let { it + oldDelta } ?: newBal
                acc.copy(balance = newBal, realBalance = newRealBal)
            } else {
                acc
            }
        }

        val updatedTransactions = current.transactions.filterNot { it.id == id }
        saveContainer(
            current.copy(
                accounts = updatedAccounts,
                transactions = updatedTransactions
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Investments
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun getFullPortfolio(): FullPortfolioData {
        val container = getOrLoadContainer()
        return FullPortfolioData(
            snapshots = container.snapshots,
            equityHoldings = container.equityHoldings,
            mutualFundHoldings = container.mutualFundHoldings,
            manualAssets = container.manualAssets
        )
    }

    suspend fun addInvestment(
        name: String,
        category: String,
        amount: Double,
        frequency: String,
        note: String?
    ) = withContext(Dispatchers.IO) {
        val current = getOrLoadContainer()
        val nowStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        val newAsset = ManualAssetDto(
            id = idGenerator.incrementAndGet(),
            category = category,
            name = name,
            investedValue = amount,
            currentValue = amount,
            interestRate = null,
            startDate = nowStr,
            maturityDate = null,
            lastUpdated = nowStr
        )

        val updatedAssets = current.manualAssets + newAsset

        // Update latest snapshot total
        val updatedSnapshots = current.snapshots.mapIndexed { index, snapshot ->
            if (index == 0) {
                snapshot.copy(
                    grandTotalInv = (snapshot.grandTotalInv ?: 0.0) + amount,
                    grandTotalCurr = (snapshot.grandTotalCurr ?: 0.0) + amount
                )
            } else snapshot
        }

        saveContainer(current.copy(manualAssets = updatedAssets, snapshots = updatedSnapshots))
    }

    suspend fun addManualAsset(
        category: String,
        name: String,
        investedValue: Double,
        currentValue: Double,
        interestRate: Double? = null,
        startDate: String? = null,
        maturityDate: String? = null,
        isRecurring: Boolean = false,
        amountToAdd: Double? = null,
        intervalValue: Int? = null,
        intervalUnit: String? = null,
        nextRunDate: String? = null
    ) = withContext(Dispatchers.IO) {
        val current = getOrLoadContainer()
        val nowStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        val newAsset = ManualAssetDto(
            id = idGenerator.incrementAndGet(),
            category = category,
            name = name,
            investedValue = investedValue,
            currentValue = currentValue,
            interestRate = interestRate,
            startDate = startDate ?: nowStr,
            maturityDate = maturityDate,
            lastUpdated = nowStr
        )

        val updatedAssets = current.manualAssets + newAsset
        saveContainer(current.copy(manualAssets = updatedAssets))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Physical Activities
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun getPhysicalActivities(): List<PhysicalActivityDto> {
        return getOrLoadContainer().physicalActivities
    }

    suspend fun addPhysicalActivity(
        date: String,
        gym: Boolean,
        badminton: Boolean,
        tableTennis: Boolean,
        cricket: Boolean,
        others: Boolean,
        description: String?
    ): PhysicalActivityDto = withContext(Dispatchers.IO) {
        val current = getOrLoadContainer()
        val newActivity = PhysicalActivityDto(
            id = idGenerator.incrementAndGet(),
            date = date,
            gym = gym,
            badminton = badminton,
            tableTennis = tableTennis,
            cricket = cricket,
            others = others,
            description = description
        )

        // Replace if already exists on same date, or prepend
        val filtered = current.physicalActivities.filterNot { it.date == date }
        val updatedActivities = listOf(newActivity) + filtered
        saveContainer(current.copy(physicalActivities = updatedActivities))
        newActivity
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Sabdekho (Media Library)
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun getMediaLibrary(
        limit: Int = 60,
        offset: Int = 0,
        type: String = "all",
        status: String = "WATCHING"
    ): MediaLibraryResponseDto {
        val all = getOrLoadContainer().mediaShows
        val filtered = all.filter { show ->
            val statusMatches = status.equals("all", ignoreCase = true) || 
                    show.status.equals(status, ignoreCase = true)
            val typeMatches = type.equals("all", ignoreCase = true) || 
                    show.type.equals(type, ignoreCase = true)
            statusMatches && typeMatches
        }
        val paged = filtered.drop(offset).take(limit)
        return MediaLibraryResponseDto(
            success = true,
            shows = paged,
            totalCount = filtered.size
        )
    }

    suspend fun searchMedia(query: String): List<com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val all = getOrLoadContainer().mediaShows
        val localMatches = all.filter { it.name?.contains(trimmed, ignoreCase = true) == true }
            .map { show ->
                com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto(
                    id = show.tmdbId ?: show.id,
                    title = if (show.type == "movie") show.name else null,
                    name = if (show.type != "movie") show.name else null,
                    mediaType = show.type ?: "movie",
                    posterPath = show.posterPath,
                    releaseDate = "2024-01-01",
                    voteAverage = 8.0,
                    overview = "Tracked title: ${show.name}"
                )
            }
        return if (localMatches.isNotEmpty()) localMatches else listOf(
            com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto(
                id = 99901,
                title = trimmed,
                mediaType = "movie",
                posterPath = null,
                releaseDate = "2024-01-01",
                voteAverage = 8.5,
                overview = "Demo search result for $trimmed"
            )
        )
    }

    suspend fun addMediaShow(
        title: String,
        type: String, // "Movie", "Series", "Anime"
        status: String, // "WATCHING", "TO WATCH", "WATCHED", "DROPPED"
        posterPath: String? = null,
        tmdbId: Int? = null,
        platform: String? = null,
        rating: Float? = null,
        review: String? = null,
        date: String? = null
    ): MediaShowDto = withContext(Dispatchers.IO) {
        val current = getOrLoadContainer()
        val newShow = MediaShowDto(
            id = idGenerator.incrementAndGet().toInt(),
            tmdbId = tmdbId,
            name = title,
            posterPath = posterPath,
            type = if (type.equals("series", ignoreCase = true) || type.equals("tv", ignoreCase = true) || type.equals("anime", ignoreCase = true)) "tv" else "movie",
            status = status,
            addedOn = date ?: LocalDate.now().toString()
        )

        val updated = listOf(newShow) + current.mediaShows
        saveContainer(current.copy(mediaShows = updated))
        newShow
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Dummy Data Generator (Seed)
    // ─────────────────────────────────────────────────────────────────────────────

    private fun generateSeedData(): DemoStorageContainer {
        val now = LocalDate.now()
        val currentMonthStr = "%04d-%02d".format(now.year, now.monthValue)
        val prevMonth = now.minusMonths(1)
        val prevMonthStr = "%04d-%02d".format(prevMonth.year, prevMonth.monthValue)
        val twoMonthsAgo = now.minusMonths(2)
        val twoMonthsAgoStr = "%04d-%02d".format(twoMonthsAgo.year, twoMonthsAgo.monthValue)

        // 1. Accounts
        val accounts = listOf(
            AccountDto(account = "HDFC", balance = 145230.50, realBalance = 145230.50, balanceTracked = true),
            AccountDto(account = "ICICI", balance = 85400.00, realBalance = 85400.00, balanceTracked = true),
            AccountDto(account = "SBI", balance = 32150.00, realBalance = 32150.00, balanceTracked = true),
            AccountDto(account = "Axis", balance = 18750.00, realBalance = null, balanceTracked = false),
            AccountDto(account = "Kotak", balance = 12000.00, realBalance = null, balanceTracked = false),
            AccountDto(account = "Cash", balance = 4500.00, realBalance = 4500.00, balanceTracked = true),
            AccountDto(account = "CC-PINNACLE 6360", balance = -14200.00, realBalance = -14200.00, balanceTracked = true),
            AccountDto(account = "CC-SBI", balance = -6500.00, realBalance = -6500.00, balanceTracked = true)
        )

        // 2. Categories
        val categories = listOf(
            "Food", "Transport", "Shopping", "Entertainment", "Bills", "Health",
            "Education", "Cinema", "Daily Need", "Salary", "Freelance", "Investment",
            "Gift", "Other"
        )

        // 3. Transactions
        val transactions = mutableListOf<TransactionDto>()
        var txId = 1000L

        fun addTx(
            dayOffset: Int,
            monthStr: String,
            acc: String,
            type: String,
            heading: String,
            desc: String?,
            amt: Double,
            exclude: Boolean = false,
            split: SplitDto? = null
        ) {
            val dateStr = "%s-%02d".format(monthStr, (dayOffset % 28) + 1)
            transactions.add(
                TransactionDto(
                    id = txId++,
                    account = acc,
                    date = dateStr,
                    month = monthStr,
                    type = type,
                    heading = heading,
                    description = desc,
                    amount = amt,
                    excludeAnalytics = exclude,
                    split = split
                )
            )
        }

        // Current Month transactions
        addTx(1, currentMonthStr, "HDFC", "Credit", "Salary", "Monthly Salary Credit", 185000.0)
        addTx(2, currentMonthStr, "HDFC", "Debit", "Bills", "Broadband & WiFi", 1199.0)
        addTx(3, currentMonthStr, "ICICI", "Debit", "Food", "Swiggy - Gourmet Pizza", 740.0)
        addTx(4, currentMonthStr, "CC-PINNACLE 6360", "Debit", "Shopping", "Amazon - Ergonomic Mouse", 2499.0)
        addTx(5, currentMonthStr, "HDFC", "Debit", "Daily Need", "Nature's Basket Groceries", 2850.0)
        addTx(7, currentMonthStr, "ICICI", "Debit", "Transport", "Uber Premier to Office", 380.0)
        addTx(8, currentMonthStr, "HDFC", "Credit", "Freelance", "Consulting Milestone", 35000.0)
        addTx(10, currentMonthStr, "CC-PINNACLE 6360", "Debit", "Cinema", "PVR IMAX - Dune 2 Tickets", 1400.0)
        addTx(12, currentMonthStr, "SBI", "Debit", "Health", "Apollo Pharmacy Essentials", 950.0)
        addTx(14, currentMonthStr, "Cash", "Debit", "Food", "Blue Tokai Coffee & Croissant", 360.0)
        addTx(15, currentMonthStr, "HDFC", "Debit", "Investment", "Monthly Mutual Fund SIP", 25000.0, exclude = true)
        addTx(
            17, currentMonthStr, "ICICI", "Debit", "Food", "Dinner with Friends at Toit", 4800.0,
            split = SplitDto(
                id = 1L,
                totalAmount = 4800.0,
                members = listOf(
                    SplitMemberDto("You", 1200.0, true),
                    SplitMemberDto("Rohit", 1200.0, true),
                    SplitMemberDto("Ananya", 1200.0, false),
                    SplitMemberDto("Vikram", 1200.0, true)
                )
            )
        )
        addTx(19, currentMonthStr, "CC-SBI", "Debit", "Entertainment", "Netflix 4K Subscription", 649.0)
        addTx(21, currentMonthStr, "HDFC", "Debit", "Transport", "Shell Fuel Refill", 3200.0)
        addTx(23, currentMonthStr, "ICICI", "Debit", "Shopping", "Zara Linen Shirt", 3990.0)
        addTx(25, currentMonthStr, "SBI", "Debit", "Bills", "Electricity Bill (BESCOM)", 2450.0)
        addTx(26, currentMonthStr, "HDFC", "Savings", "Savings", "Emergency Fund Deposit", 15000.0)

        // Previous Month transactions
        addTx(1, prevMonthStr, "HDFC", "Credit", "Salary", "Monthly Salary Credit", 185000.0)
        addTx(2, prevMonthStr, "HDFC", "Debit", "Bills", "Rent & Maintenance", 32000.0)
        addTx(4, prevMonthStr, "ICICI", "Debit", "Food", "Zomato - Biryani Feast", 920.0)
        addTx(6, prevMonthStr, "CC-PINNACLE 6360", "Debit", "Shopping", "Decathlon Trekking Gear", 4600.0)
        addTx(8, prevMonthStr, "HDFC", "Credit", "Gift", "Birthday Gift from Family", 10000.0)
        addTx(10, prevMonthStr, "SBI", "Debit", "Health", "Annual Health Checkup", 3500.0)
        addTx(12, prevMonthStr, "CC-SBI", "Debit", "Entertainment", "Spotify Family Plan", 179.0)
        addTx(15, prevMonthStr, "HDFC", "Debit", "Investment", "Mutual Fund SIP", 25000.0, exclude = true)
        addTx(18, prevMonthStr, "ICICI", "Debit", "Food", "Third Wave Coffee", 420.0)
        addTx(22, prevMonthStr, "HDFC", "Debit", "Transport", "Metro Smart Card Recharge", 1000.0)
        addTx(26, prevMonthStr, "CC-PINNACLE 6360", "Debit", "Dining", "Sunday Brunch Buffet", 2900.0)

        // 2 Months ago
        addTx(1, twoMonthsAgoStr, "HDFC", "Credit", "Salary", "Monthly Salary Credit", 185000.0)
        addTx(3, twoMonthsAgoStr, "HDFC", "Debit", "Bills", "Electricity & Water", 2100.0)
        addTx(7, twoMonthsAgoStr, "CC-PINNACLE 6360", "Debit", "Shopping", "Nike Running Shoes", 6499.0)
        addTx(14, twoMonthsAgoStr, "ICICI", "Debit", "Food", "Weekend Dinner", 1800.0)
        addTx(15, twoMonthsAgoStr, "HDFC", "Debit", "Investment", "Mutual Fund SIP", 25000.0, exclude = true)

        // 4. Investment Snapshots (12 Historical Snapshots)
        val snapshots = mutableListOf<PortfolioSnapshotDto>()
        var baseInv = 850000.0
        var baseCurr = 920000.0
        for (i in 11 downTo 0) {
            val sDate = now.minusMonths(i.toLong()).withDayOfMonth(1).format(DateTimeFormatter.ISO_DATE)
            baseInv += 30000.0 + (i * 2000.0)
            baseCurr = baseInv * (1.08 + ((12 - i) * 0.015))

            snapshots.add(
                PortfolioSnapshotDto(
                    id = (12 - i).toLong(),
                    date = sDate,
                    invStocks = baseInv * 0.35,
                    currStocks = baseCurr * 0.38,
                    invMf = baseInv * 0.30,
                    currMf = baseCurr * 0.32,
                    invFixed = baseInv * 0.15,
                    currFixed = baseInv * 0.15 * 1.05,
                    invProv = baseInv * 0.12,
                    currProv = baseInv * 0.12 * 1.07,
                    invGold = baseInv * 0.08,
                    currGold = baseCurr * 0.08,
                    grandTotalInv = baseInv,
                    grandTotalCurr = baseCurr
                )
            )
        }
        snapshots.reverse() // latest first

        // 5. Equity Holdings
        val equityHoldings = listOf(
            EquityHoldingDto(symbol = "RELIANCE", quantity = 40.0, averagePrice = 2450.0, ltp = 2980.0, investedValue = 98000.0, currentValue = 119200.0),
            EquityHoldingDto(symbol = "TCS", quantity = 25.0, averagePrice = 3520.0, ltp = 4120.0, investedValue = 88000.0, currentValue = 103000.0),
            EquityHoldingDto(symbol = "INFY", quantity = 60.0, averagePrice = 1420.0, ltp = 1790.0, investedValue = 85200.0, currentValue = 107400.0),
            EquityHoldingDto(symbol = "HDFCBANK", quantity = 70.0, averagePrice = 1530.0, ltp = 1680.0, investedValue = 107100.0, currentValue = 117600.0),
            EquityHoldingDto(symbol = "TATAMOTORS", quantity = 110.0, averagePrice = 620.0, ltp = 960.0, investedValue = 68200.0, currentValue = 105600.0),
            EquityHoldingDto(symbol = "LT", quantity = 20.0, averagePrice = 3100.0, ltp = 3650.0, investedValue = 62000.0, currentValue = 73000.0)
        )

        // 6. Mutual Funds
        val mutualFundHoldings = listOf(
            MutualFundHoldingDto(symbol = "Parag Parikh Flexi Cap Fund - Direct (G)", quantity = 2450.0, averagePrice = 62.5, nav = 84.2, investedValue = 153125.0, currentValue = 206290.0),
            MutualFundHoldingDto(symbol = "Mirae Asset Large & Midcap Fund - Direct (G)", quantity = 1120.0, averagePrice = 95.0, nav = 138.4, investedValue = 106400.0, currentValue = 155008.0),
            MutualFundHoldingDto(symbol = "Quant Small Cap Fund - Direct (G)", quantity = 580.0, averagePrice = 175.0, nav = 265.0, investedValue = 101500.0, currentValue = 153700.0),
            MutualFundHoldingDto(symbol = "Nippon India Growth Fund - Direct (G)", quantity = 290.0, averagePrice = 280.0, nav = 390.0, investedValue = 81200.0, currentValue = 113100.0)
        )

        // 7. Manual Assets
        val manualAssets = listOf(
            ManualAssetDto(id = 1L, category = "FD", name = "HDFC 3-Yr Term Deposit", investedValue = 200000.0, currentValue = 232000.0, interestRate = 7.25, startDate = "2024-01-15", maturityDate = "2027-01-15", lastUpdated = "2026-08-01"),
            ManualAssetDto(id = 2L, category = "GOLD", name = "Sovereign Gold Bond 2028", investedValue = 150000.0, currentValue = 225000.0, interestRate = 2.50, startDate = "2023-06-20", maturityDate = "2028-06-20", lastUpdated = "2026-08-01"),
            ManualAssetDto(id = 3L, category = "EPF", name = "Employee Provident Fund (EPF)", investedValue = 420000.0, currentValue = 495000.0, interestRate = 8.25, startDate = "2022-04-01", maturityDate = null, lastUpdated = "2026-08-01"),
            ManualAssetDto(id = 4L, category = "PPF", name = "Public Provident Fund (SBI)", investedValue = 180000.0, currentValue = 218000.0, interestRate = 7.10, startDate = "2021-08-10", maturityDate = "2036-08-10", lastUpdated = "2026-08-01")
        )

        // 8. Physical Activities
        val physicalActivities = mutableListOf<PhysicalActivityDto>()
        var actId = 500L
        for (i in 1..28) {
            val dateStr = "%s-%02d".format(currentMonthStr, i)
            val isGym = (i % 2 == 1 && i % 7 != 0)
            val isBadminton = (i % 5 == 0)
            val isTableTennis = (i % 6 == 0)
            val isCricket = (i % 7 == 6)
            val isOthers = (i % 8 == 0)

            if (isGym || isBadminton || isTableTennis || isCricket || isOthers) {
                physicalActivities.add(
                    PhysicalActivityDto(
                        id = actId++,
                        date = dateStr,
                        gym = isGym,
                        badminton = isBadminton,
                        tableTennis = isTableTennis,
                        cricket = isCricket,
                        others = isOthers,
                        description = if (isGym) "Chest & Triceps workout" else if (isBadminton) "Match with friends" else null
                    )
                )
            }
        }
        // Also populate previous month activities
        for (i in listOf(2, 4, 6, 8, 11, 14, 16, 18, 20, 23, 25, 27)) {
            val dateStr = "%s-%02d".format(prevMonthStr, i)
            physicalActivities.add(
                PhysicalActivityDto(
                    id = actId++,
                    date = dateStr,
                    gym = true,
                    badminton = (i % 4 == 0),
                    tableTennis = (i % 3 == 0),
                    cricket = false,
                    others = (i % 5 == 0),
                    description = "Regular workout routine"
                )
            )
        }

        // 9. Media Library (Sabdekho)
        val mediaShows = listOf(
            MediaShowDto(id = 1, tmdbId = 110492, name = "Severance", posterPath = "https://image.tmdb.org/t/p/w500/1XddXPX8x241VbzgUwfg437Fcq8.jpg", type = "series", status = "WATCHING"),
            MediaShowDto(id = 2, tmdbId = 126308, name = "Shōgun", posterPath = "https://image.tmdb.org/t/p/w500/7O4iVfOMQmdCSxhOg1WnzG1AgYT.jpg", type = "series", status = "WATCHING"),
            MediaShowDto(id = 3, tmdbId = 76479, name = "The Boys", posterPath = "https://image.tmdb.org/t/p/w500/mY7SeH4HFFxW1hiI6cWuwCRKptN.jpg", type = "series", status = "WATCHING"),
            MediaShowDto(id = 4, tmdbId = 87108, name = "Chernobyl", posterPath = "https://image.tmdb.org/t/p/w500/hlLXt2tOPT6RRnjiUmoxyG1LTFi.jpg", type = "series", status = "WATCHING"),
            
            MediaShowDto(id = 5, tmdbId = 872585, name = "Oppenheimer", posterPath = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg", type = "movie", status = "TO WATCH"),
            MediaShowDto(id = 6, tmdbId = 693134, name = "Dune: Part Two", posterPath = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg", type = "movie", status = "TO WATCH"),
            MediaShowDto(id = 7, tmdbId = 157336, name = "Interstellar", posterPath = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", type = "movie", status = "TO WATCH"),
            MediaShowDto(id = 8, tmdbId = 105248, name = "Cyberpunk: Edgerunners", posterPath = "https://image.tmdb.org/t/p/w500/7jswOc6jWwMRipsqg8KLV1FQ1WW.jpg", type = "anime", status = "TO WATCH"),

            MediaShowDto(id = 9, tmdbId = 1396, name = "Breaking Bad", posterPath = "https://image.tmdb.org/t/p/w500/ggFHVNu6YYI5L9pCfOacjizRGt.jpg", type = "series", status = "WATCHED"),
            MediaShowDto(id = 10, tmdbId = 60059, name = "Better Call Saul", posterPath = "https://image.tmdb.org/t/p/w500/fC2HDm5t0kHsfNxPkUQIZyhioLZ.jpg", type = "series", status = "WATCHED"),
            MediaShowDto(id = 11, tmdbId = 155, name = "The Dark Knight", posterPath = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg", type = "movie", status = "WATCHED"),
            MediaShowDto(id = 12, tmdbId = 27205, name = "Inception", posterPath = "https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg", type = "movie", status = "WATCHED"),
            MediaShowDto(id = 13, tmdbId = 94605, name = "Arcane", posterPath = "https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn397FvFeNZ91.jpg", type = "series", status = "WATCHED"),

            MediaShowDto(id = 14, tmdbId = 124364, name = "The Idol", posterPath = null, type = "series", status = "DROPPED"),
            MediaShowDto(id = 15, tmdbId = 136283, name = "Velma", posterPath = null, type = "series", status = "DROPPED")
        )

        return DemoStorageContainer(
            accounts = accounts,
            transactions = transactions,
            categories = categories,
            snapshots = snapshots,
            equityHoldings = equityHoldings,
            mutualFundHoldings = mutualFundHoldings,
            manualAssets = manualAssets,
            physicalActivities = physicalActivities,
            mediaShows = mediaShows
        )
    }
}
