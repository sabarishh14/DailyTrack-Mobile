package com.example.dailytrack_mobile.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.util.Calendar
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Shared accent colours (theme-agnostic, same pattern as ActivitiesScreen)
// ─────────────────────────────────────────────────────────────────────────────
private val GainGreen   = Color(0xFF2ECC71)
private val LossRed     = Color(0xFFE74C3C)

// ─────────────────────────────────────────────────────────────────────────────
// Mock data models
// ─────────────────────────────────────────────────────────────────────────────
private data class BankAccount(
    val name: String,
    val accountMask: String,
    val balance: Double
)

private data class Investment(
    val name: String,
    val invested: Double,
    val current: Double
) {
    val returns: Double get() = current - invested
    val returnsPercent: Double get() = if (invested == 0.0) 0.0 else (returns / invested) * 100.0
    val isGain: Boolean get() = returns >= 0
}

private data class FlowBreakdown(
    val label: String,
    val icon: ImageVector,
    val amount: Double
)

// ─────────────────────────────────────────────────────────────────────────────
// Mock data
// ─────────────────────────────────────────────────────────────────────────────
private val bankAccounts = listOf(
    BankAccount("HDFC Savings",  "xxxx 4821", 1_24_500.75),
    BankAccount("SBI Current",   "xxxx 9034",  87_250.00),
    BankAccount("Axis Savings",  "xxxx 1173",  42_000.50),
    BankAccount("ICICI Salary",  "xxxx 6602", 2_18_340.00),
)

private val investments = listOf(
    Investment("Nifty 50 Index", 1_00_000.0, 1_14_320.0),
    Investment("Mid Cap Fund",     50_000.0,    54_800.0),
    Investment("US Tech ETF",      30_000.0,    27_150.0),
    Investment("Gold ETF",         20_000.0,    22_100.0),
)

private val totalInvested = investments.sumOf { it.invested }
private val totalCurrent  = investments.sumOf { it.current }
private val totalReturns  = totalCurrent - totalInvested
private val cashBalance   = 15_000.0
private val totalNetWorth = bankAccounts.sumOf { it.balance } + totalCurrent + cashBalance

private val incomeBreakdown = listOf(
    FlowBreakdown("Cash",         Icons.Default.Money,          15_000.0),
    FlowBreakdown("HDFC Savings", Icons.Default.AccountBalance, 45_000.0),
    FlowBreakdown("SBI Current",  Icons.Default.AccountBalance, 12_500.0),
    FlowBreakdown("Axis Savings", Icons.Default.AccountBalance,  8_000.0),
    FlowBreakdown("ICICI Salary", Icons.Default.AccountBalance, 85_000.0),
    FlowBreakdown("HDFC CC",      Icons.Default.CreditCard,          0.0),
    FlowBreakdown("SBI CC",       Icons.Default.CreditCard,          0.0),
)

private val expenseBreakdown = listOf(
    FlowBreakdown("Cash",         Icons.Default.Money,           3_500.0),
    FlowBreakdown("HDFC Savings", Icons.Default.AccountBalance, 12_400.0),
    FlowBreakdown("SBI Current",  Icons.Default.AccountBalance,  4_200.0),
    FlowBreakdown("Axis Savings", Icons.Default.AccountBalance,  1_800.0),
    FlowBreakdown("ICICI Salary", Icons.Default.AccountBalance,  9_100.0),
    FlowBreakdown("HDFC CC",      Icons.Default.CreditCard,     18_600.0),
    FlowBreakdown("SBI CC",       Icons.Default.CreditCard,      6_300.0),
)

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..11  -> "Good morning"
    in 12..16 -> "Good afternoon"
    else      -> "Good evening"
}

private fun formatCurrency(amount: Double): String {
    val abs = Math.abs(amount)
    return when {
        abs >= 1_00_000 -> "₹%.2fL".format(abs / 1_00_000)
        abs >= 1_000    -> "₹%.2fK".format(abs / 1_000)
        else            -> "₹%.0f".format(abs)
    }
}

private fun formatCurrencyFull(amount: Double): String {
    val prefix = if (amount < 0) "-" else ""
    return "$prefix₹%,.2f".format(Math.abs(amount))
}

// ─────────────────────────────────────────────────────────────────────────────
// Main composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen() {
    val dims = Dimens.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = dims.screenHorizontalPadding,
            end = dims.screenHorizontalPadding,
            top = dims.screenTopPadding,
            bottom = dims.screenBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing)
    ) {
        item { GreetingHeader() }
        item { NetWorthSection() }
        item { BankAccountsSection() }
        item { InvestmentPortfolioSection() }
        item { FlowSection(title = "TOTAL INCOME",   flows = incomeBreakdown,  isIncome = true)  }
        item { FlowSection(title = "TOTAL EXPENSES", flows = expenseBreakdown, isIncome = false) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Greeting header  +  Sheets action box
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GreetingHeader() {
    // Recomputes every minute so the greeting updates live as time passes
    val greetingText by produceState(initialValue = greeting()) {
        while (true) {
            delay(60_000L)   // wait 1 minute, then refresh
            value = greeting()
        }
    }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Left – greeting text
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = "$greetingText,",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = "Sabarish 👋",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Right – Sheets action box
        SheetsActionBox()
    }
}

/**
 * A fieldset-style box labelled "Sheets" on the top-left border,
 * containing a Sync button and an Upload button.
 */
@Composable
private fun SheetsActionBox() {
    val dims = Dimens.current
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val bgColor     = MaterialTheme.colorScheme.background   // matches page background

    // Outer Box positions the label on top of the border line
    Box(modifier = Modifier.wrapContentSize()) {

        // ── Border container ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(top = 8.dp)              // leave room above for the label
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Sync button
                IconButton(onClick = { /* TODO: sync */ }) {
                    Icon(
                        imageVector        = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(dims.iconSizeMedium)
                    )
                }
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(dims.iconSizeSmall)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                // Upload button
                IconButton(onClick = { /* TODO: upload */ }) {
                    Icon(
                        imageVector        = Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(dims.iconSizeMedium)
                    )
                }
            }
        }

        // ── "Sheets" label sitting on the top-left border line ───────────
        Text(
            text  = "Sheets",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 1.dp)    // sit right on the border line
                .background(bgColor)             // punch through the border visually
                .padding(horizontal = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 1 – Net Worth
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NetWorthSection() {
    val dims = Dimens.current
    SectionCard {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionLabel(text = "NET WORTH")
            Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
            Text(
                text  = formatCurrencyFull(totalNetWorth),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize   = dims.fontSizeDisplayLarge
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Banks · Cash · Investments",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 2 – Bank Accounts
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BankAccountsSection() {
    val dims = Dimens.current
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            SectionLabel(text = "BANK BALANCES")
            Spacer(modifier = Modifier.height(dims.itemSpacingLarge))
            bankAccounts.forEachIndexed { idx, account ->
                BankAccountRow(account = account)
                if (idx < bankAccounts.lastIndex) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = dims.itemSpacingMedium),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
            HorizontalDivider(
                modifier  = Modifier.padding(vertical = dims.itemSpacingLarge),
                thickness = 1.dp,
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "Total Balance",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = formatCurrencyFull(bankAccounts.sumOf { it.balance }),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BankAccountRow(account: BankAccount) {
    val dims = Dimens.current
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
        ) {
            Box(
                modifier         = Modifier
                    .size(dims.avatarSizeMedium)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier           = Modifier.size(dims.iconSizeSmall + 2.dp)
                )
            }
            Column {
                Text(
                    text  = account.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = account.accountMask,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text  = formatCurrencyFull(account.balance),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 3 – Investment Portfolio
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InvestmentPortfolioSection() {
    val dims = Dimens.current
    val isOverallGain = totalReturns >= 0
    val overallColor  = if (isOverallGain) GainGreen else LossRed

    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)) {
            SectionLabel(text = "INVESTMENT PORTFOLIO")

            // ── Overall summary banner ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dims.buttonCornerRadius))
                    .background(overallColor.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = overallColor.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(dims.buttonCornerRadius)
                    )
                    .padding(horizontal = dims.cardInnerPadding - 4.dp, vertical = dims.itemSpacingLarge),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Invested",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text  = formatCurrencyFull(totalInvested),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text  = formatCurrencyFull(totalCurrent),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Returns",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isOverallGain) Icons.Default.TrendingUp
                                          else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint     = overallColor,
                            modifier = Modifier.size(dims.iconSizeSmall)
                        )
                        Text(
                            text  = "${if (isOverallGain) "+" else ""}${formatCurrency(totalReturns)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = overallColor
                        )
                    }
                    val pctSign = if (totalReturns >= 0) "+" else ""
                    Text(
                        text  = "%s%.2f%%".format(pctSign, (totalReturns / totalInvested) * 100),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = overallColor
                    )
                }
            }

            // ── Individual holdings ───────────────────────────────────────
            SectionLabel(text = "HOLDINGS")
            investments.forEachIndexed { idx, inv ->
                InvestmentRow(investment = inv)
                if (idx < investments.lastIndex) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = dims.itemSpacingMedium),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestmentRow(investment: Investment) {
    val dims = Dimens.current
    val gainColor = if (investment.isGain) GainGreen else LossRed
    val pctSign   = if (investment.isGain) "+" else ""

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
            modifier              = Modifier.weight(1f)
        ) {
            Box(
                modifier         = Modifier
                    .size(dims.avatarSizeMedium)
                    .clip(RoundedCornerShape(10.dp))
                    .background(gainColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint               = gainColor,
                    modifier           = Modifier.size(dims.iconSizeSmall + 2.dp)
                )
            }
            Column {
                Text(
                    text  = investment.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "Invested: ${formatCurrency(investment.invested)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = formatCurrency(investment.current),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "%s%s (%.1f%%)".format(
                    pctSign,
                    formatCurrency(investment.returns),
                    investment.returnsPercent
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = gainColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sections 4 & 5 – Income / Expense breakdown
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FlowSection(
    title    : String,
    flows    : List<FlowBreakdown>,
    isIncome : Boolean
) {
    val dims = Dimens.current
    val accentColor = if (isIncome) GainGreen else LossRed
    val total       = flows.sumOf { it.amount }

    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SectionLabel(text = title)
                Text(
                    text  = formatCurrencyFull(total),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(dims.itemSpacingLarge))
            flows.forEachIndexed { idx, flow ->
                FlowRow(flow = flow, accentColor = accentColor)
                if (idx < flows.lastIndex) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = dims.itemSpacingMedium),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowRow(flow: FlowBreakdown, accentColor: Color) {
    val dims = Dimens.current
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
        ) {
            Box(
                modifier         = Modifier
                    .size(dims.avatarSizeSmall)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = flow.icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(dims.iconSizeSmall)
                )
            }
            Text(
                text  = flow.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text  = if (flow.amount == 0.0) "—" else formatCurrencyFull(flow.amount),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (flow.amount == 0.0) MaterialTheme.colorScheme.onSurfaceVariant
                    else accentColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable building blocks
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val dims = Dimens.current
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(dims.cardCornerRadius),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.cardInnerPadding),
            content  = content
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )
}
