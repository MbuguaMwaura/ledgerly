package app.ledgerly.android.ui.spend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.Amount
import app.ledgerly.android.ui.components.BudgetRow
import app.ledgerly.android.ui.components.ButtonStyle
import app.ledgerly.android.ui.components.EmptyNote
import app.ledgerly.android.ui.components.EntryRow
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyCard
import app.ledgerly.android.ui.components.LinkButton
import app.ledgerly.android.ui.components.RowsCard
import app.ledgerly.android.ui.components.SegmentedControl
import app.ledgerly.android.ui.components.SpendBars
import app.ledgerly.android.ui.components.TabScreen
import app.ledgerly.android.ui.components.TabTitle
import app.ledgerly.android.ui.components.label
import app.ledgerly.android.ui.components.short
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.spend.SpendUiState
import app.ledgerly.shared.presentation.spend.SpendViewModel
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private enum class SpendView(val label: String) { Log("Daily log"), Budgets("Budgets") }

@Composable
fun SpendRoute(
    onOpenEntry: (String) -> Unit,
    onEditBudgets: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpendViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SpendScreen(state, viewModel::onSelectDay, onOpenEntry, onEditBudgets, modifier)
}

@Composable
fun SpendScreen(
    state: SpendUiState,
    onSelectDay: (LocalDate?) -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditBudgets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerlyTheme.colors
    var view by rememberSaveable { mutableStateOf(SpendView.Log) }
    val monthName = Month.of(state.month.month).getDisplayName(TextStyle.FULL, Locale.UK)
    val today = state.bars.firstOrNull { it.isToday }?.date
    val daysLeft = today?.let { state.month.lengthInDays - it.dayOfMonth + 1 }

    TabScreen(modifier) {
        item("title") { TabTitle("Spending") }

        item("hero") {
            LedgerlyCard {
                Text("Spent in $monthName", style = LedgerlyTextStyles.label, color = colors.inkMuted)
                Amount(state.spentThisMonth, style = LedgerlyTextStyles.amountHero, modifier = Modifier.padding(top = 6.dp))
                if (today != null && state.spentThisMonth.minorUnits > 0) {
                    val pace = Money(state.spentThisMonth.minorUnits / today.dayOfMonth * state.month.lengthInDays, state.spentThisMonth.currency)
                    Text(
                        buildAnnotatedString {
                            append("On pace for ")
                            withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.SemiBold)) { append(pace.label()) }
                            append(" by ${state.month.lastDay.short()}")
                        },
                        style = LedgerlyTextStyles.label,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                SpendBars(state.bars, Modifier.padding(top = 20.dp), onSelect = onSelectDay)
            }
        }

        if (state.budgets.isNotEmpty()) {
            item("view") {
                SegmentedControl(
                    options = SpendView.entries,
                    selected = view,
                    label = { it.label },
                    onSelect = { view = it },
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
        }

        if (view == SpendView.Log || state.budgets.isEmpty()) {
            val selected = state.selectedDay
            if (selected != null) {
                item("filter") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Showing ${selected.short()}", style = LedgerlyTextStyles.label, color = colors.ink)
                        LinkButton("Show all", { onSelectDay(null) })
                    }
                }
            }
            state.days.forEach { day ->
                item("day-${day.date}") {
                    Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(day.label, style = LedgerlyTextStyles.label, color = colors.ink)
                        Text(day.spent.label(), style = LedgerlyTextStyles.label, color = colors.inkMuted)
                    }
                    RowsCard(day.entries) { entry -> EntryRow(entry, onClick = { onOpenEntry(entry.id) }) }
                    }
                }
            }
            if (state.days.isEmpty() && !state.isLoading) {
                item("empty") {
                    EmptyNote(
                        if (selected != null) "Nothing logged on ${selected.short()}." else "Nothing logged this month yet. Tap + to log your first expense.",
                        Modifier.padding(top = 12.dp),
                    )
                }
            }
            if (state.budgets.isEmpty() && !state.isLoading) {
                item("set-budgets") {
                    LedgerlyCard(Modifier.padding(top = 24.dp)) {
                        Text("No budgets yet", style = LedgerlyTextStyles.heading, color = colors.ink)
                        Text(
                            "Set a monthly limit for the categories you want to watch. Home then shows what's left each day.",
                            style = LedgerlyTextStyles.label,
                            color = colors.inkMuted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        LedgerlyButton("Set budgets", onEditBudgets, style = ButtonStyle.Soft, small = true)
                    }
                }
            }
        } else {
            item("budgets-head") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${state.budgets.size} ${if (state.budgets.size == 1) "budget" else "budgets"} this month",
                        style = LedgerlyTextStyles.label,
                        color = colors.ink,
                    )
                    LinkButton("Edit budgets", onEditBudgets)
                }
            }
            item("budgets") { RowsCard(state.budgets) { BudgetRow(it, daysLeft) } }
        }
    }
}
