package app.ledgerly.android.ui.home

import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.components.LedgerlyIcon
import app.ledgerly.android.ui.components.RoundIconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.Amount
import app.ledgerly.android.ui.components.BudgetRow
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.ButtonStyle
import app.ledgerly.android.ui.components.EmptyNote
import app.ledgerly.android.ui.components.EntryRow
import app.ledgerly.android.ui.components.Gauge
import app.ledgerly.android.ui.components.LedgerlyCard
import app.ledgerly.android.ui.components.LetterTile
import app.ledgerly.android.ui.components.LinkButton
import app.ledgerly.android.ui.components.RowsCard
import app.ledgerly.android.ui.components.SectionHeader
import app.ledgerly.android.ui.components.StreakCard
import app.ledgerly.android.ui.components.TabScreen
import app.ledgerly.android.ui.components.initials
import app.ledgerly.android.ui.components.label
import app.ledgerly.android.ui.components.short
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.rules.DueStatus
import app.ledgerly.shared.presentation.common.DebtRowState
import app.ledgerly.shared.presentation.home.HomeUiState
import app.ledgerly.shared.presentation.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeRoute(
    onLogExpense: () -> Unit,
    onOpenSpend: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditBudgets: () -> Unit,
    onOpenSettings: () -> Unit,
    amountsHidden: Boolean,
    onToggleAmounts: () -> Unit,
    firstName: String,
    trackBudgets: Boolean,
    trackDebts: Boolean,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onLogExpense = onLogExpense,
        onNothingSpent = viewModel::onNothingSpentToday,
        onOpenSpend = onOpenSpend,
        onOpenDebts = onOpenDebts,
        onOpenEntry = onOpenEntry,
        onEditBudgets = onEditBudgets,
        onOpenSettings = onOpenSettings,
        amountsHidden = amountsHidden,
        onToggleAmounts = onToggleAmounts,
        firstName = firstName,
        trackBudgets = trackBudgets,
        trackDebts = trackDebts,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLogExpense: () -> Unit,
    onNothingSpent: () -> Unit,
    onOpenSpend: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditBudgets: () -> Unit,
    onOpenSettings: () -> Unit,
    amountsHidden: Boolean,
    onToggleAmounts: () -> Unit,
    firstName: String,
    trackBudgets: Boolean,
    trackDebts: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerlyTheme.colors
    val now = LocalDateTime.now()
    val hasBudgets = trackBudgets && state.totalBudget.minorUnits > 0

    TabScreen(modifier) {
        item("header") {
            Row(Modifier.padding(top = 12.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(now.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.UK)), style = LedgerlyTextStyles.label, color = colors.inkMuted)
                    Text(
                        if (firstName.isEmpty()) greeting(now.hour) else "${greeting(now.hour)}, $firstName",
                        style = LedgerlyTextStyles.title,
                        color = colors.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundIconButton(onToggleAmounts, if (amountsHidden) "Show amounts" else "Hide amounts", size = 44.dp) {
                        LedgerlyIcon(if (amountsHidden) LedgerlyIcons.EyeOff else LedgerlyIcons.Eye, size = 20.dp)
                    }
                    RoundIconButton(onOpenSettings, "Settings", size = 44.dp) { LedgerlyIcon(LedgerlyIcons.Sliders, size = 20.dp) }
                }
            }
        }

        item("left") {
            LedgerlyCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (hasBudgets) "Left to spend" else "Spent this month", style = LedgerlyTextStyles.heading, color = colors.ink)
                    Text(now.month.getDisplayName(TextStyle.FULL, Locale.UK), style = LedgerlyTextStyles.label, color = colors.inkMuted)
                }
                Amount(if (hasBudgets) state.leftToSpend else state.spent, style = LedgerlyTextStyles.amountHero, modifier = Modifier.padding(top = 6.dp))
                Text(
                    buildAnnotatedString {
                        if (!hasBudgets && trackBudgets) {
                            append("Set budgets to see what's left each day.")
                        } else if (!hasBudgets) {
                            append("Across everything you logged this month.")
                        } else if (state.leftToSpend.isNegative) {
                            withStyle(SpanStyle(color = colors.loss, fontWeight = FontWeight.SemiBold)) { append("${(-state.leftToSpend).label()} over") }
                            append(" your budgets this month")
                        } else {
                            withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.SemiBold)) { append("${state.perDay.label()} a day") }
                            append(" for the last ${state.daysLeft} ${if (state.daysLeft == 1) "day" else "days"}")
                        }
                    },
                    style = LedgerlyTextStyles.label,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (trackBudgets && !hasBudgets && !state.isLoading) {
                    LedgerlyButton("Set budgets", onEditBudgets, style = ButtonStyle.Soft, small = true, modifier = Modifier.padding(top = 12.dp))
                }
                Row(
                    Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LedgerlyRadius.radiusMd))
                        .background(colors.surfaceSunk),
                ) {
                    SplitCell("Spent", if (hasBudgets) "${state.budgetPercentUsed}% of budget" else "This month", Modifier.weight(1f)) {
                        Amount(state.spent, style = LedgerlyTextStyles.amount.copy(fontSize = LedgerlyTextStyles.amount.fontSize * 0.86f), showFraction = false)
                    }
                    Box(
                        Modifier
                            .padding(vertical = 12.dp)
                            .width(1.dp)
                            .height(64.dp)
                            .background(colors.line),
                    )
                    SplitCell("Income", "This month", Modifier.weight(1f)) {
                        Amount(state.income, style = LedgerlyTextStyles.amount.copy(fontSize = LedgerlyTextStyles.amount.fontSize * 0.86f), showFraction = false)
                    }
                }
            }
        }

        item("streak") {
            StreakCard(
                streak = state.streak,
                week = state.week,
                nextMilestone = state.nextMilestone,
                onLog = onLogExpense,
                onNothingSpent = onNothingSpent,
                modifier = Modifier.padding(top = LedgerlySpacing.space3),
            )
        }

        if (hasBudgets) {
            item("budgets") {
                LedgerlyCard(Modifier.padding(top = LedgerlySpacing.space3)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Budgets", style = LedgerlyTextStyles.heading, color = colors.ink)
                        LinkButton("Edit", onEditBudgets)
                    }
                    Gauge(state.budgetPercentUsed / 100f, Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Budget used", style = LedgerlyTextStyles.caption, color = colors.inkMuted)
                        Text("${state.budgetPercentUsed}%", style = LedgerlyTextStyles.display, color = colors.ink)
                        Text("of ${state.totalBudget.label()}", style = LedgerlyTextStyles.caption, color = colors.inkMuted)
                    }
                    Text(
                        when (val n = state.needsALook.size) {
                            0 -> "Every budget is on track."
                            1 -> "1 category needs a look."
                            else -> "$n categories need a look."
                        },
                        style = LedgerlyTextStyles.caption,
                        color = colors.inkMuted,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 6.dp),
                    )
                    state.needsALook.forEach { BudgetRow(it, daysLeft = state.daysLeft) }
                }
            }
        }

        item("recent-head") { SectionHeader("Recent", action = "Daily log", onAction = onOpenSpend) }
        item("recent") {
            if (state.recent.isEmpty()) {
                LedgerlyCard { EmptyNote(if (state.isLoading) "" else "Nothing logged this month yet. Tap + to log your first expense.") }
            } else {
                RowsCard(state.recent) { EntryRow(it, onClick = { onOpenEntry(it.id) }) }
            }
        }

        if (trackDebts && state.debtsDueSoon.isNotEmpty()) {
            item("owed-head") { SectionHeader("Owed to you, due soon", action = "All debts", onAction = onOpenDebts) }
            state.debtsDueSoon.forEach { debt ->
                item("owed-${debt.id}") { DueSoonRow(debt, onOpenDebts, Modifier.padding(bottom = LedgerlySpacing.space3)) }
            }
        }
    }
}

private fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun SplitCell(title: String, caption: String, modifier: Modifier, amount: @Composable () -> Unit) {
    Column(modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = LedgerlyTextStyles.label, color = LedgerlyTheme.colors.ink)
        amount()
        Text(caption, style = LedgerlyTextStyles.caption, color = LedgerlyTheme.colors.inkMuted)
    }
}

/** A compact debtor row on Home. Tapping it opens Debts, where reminders and payments live. */
@Composable
private fun DueSoonRow(row: DebtRowState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LedgerlyTheme.colors
    val overdue = row.status is DueStatus.Overdue
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LedgerlyRadius.radiusMd))
            .background(if (overdue) colors.lossSoft else colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(LedgerlySpacing.space5),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterTile(initials(row.counterparty), colors.surface, colors.ink, border = colors.line)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.counterparty, style = LedgerlyTextStyles.body, color = colors.ink)
            val due = when (val s = row.status) {
                is DueStatus.Overdue -> "Overdue · was due ${row.dueDate?.short()}"
                is DueStatus.DueSoon -> if (s.days == 0) "Due today" else "Due ${row.dueDate?.short()}"
                else -> ""
            }
            Text(due, style = LedgerlyTextStyles.caption, color = if (overdue) colors.loss else colors.caution)
        }
        Text(row.remaining.label(), style = LedgerlyTextStyles.amountRow, color = colors.ink)
    }
}
