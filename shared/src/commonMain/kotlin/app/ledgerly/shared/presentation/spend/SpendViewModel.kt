package app.ledgerly.shared.presentation.spend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.repository.BudgetRepository
import app.ledgerly.shared.domain.repository.CategoryRepository
import app.ledgerly.shared.domain.repository.TransactionRepository
import app.ledgerly.shared.domain.rules.budgetStatus
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.common.BudgetRowState
import app.ledgerly.shared.presentation.common.EntryRowState
import app.ledgerly.shared.presentation.common.dayLabel
import app.ledgerly.shared.presentation.common.toRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** One bar in the Spend chart. */
data class DayBarState(
    val date: LocalDate,
    val spent: Money,
    val isToday: Boolean,
    /** Future days are drawn hatched. */
    val isFuture: Boolean,
    val isSelected: Boolean,
)

data class DayGroupState(
    val date: LocalDate,
    /** "Today", "Yesterday", "Wed, 25 Nov". */
    val label: String,
    val spent: Money,
    val entries: List<EntryRowState>,
)

data class SpendUiState(
    val isLoading: Boolean,
    val month: BudgetMonth,
    val spentThisMonth: Money,
    val bars: List<DayBarState>,
    /** Null shows the whole month; a date filters the log to that day. */
    val selectedDay: LocalDate?,
    val days: List<DayGroupState>,
    /** Every budget this month, most used first. */
    val budgets: List<BudgetRowState>,
)

class SpendViewModel(
    private val transactions: TransactionRepository,
    budgets: BudgetRepository,
    categories: CategoryRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val today = clock.today()
    private val month = BudgetMonth.of(today)
    private val currency = CurrencyCode.KES
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<SpendUiState> = combine(
        transactions.observeBetween(month.firstDay, month.lastDay),
        budgets.observeMonth(month),
        categories.observeAll(),
        selectedDay,
    ) { entries, monthBudgets, all, selected ->
        val zero = Money.zero(currency)
        val byId = all.associateBy { it.id }
        val expenses = entries.filter { it.kind == EntryKind.Expense && it.amount.currency == currency }
        val spentByDay = expenses.groupBy { it.logicalDate }.mapValues { (_, list) -> list.fold(zero) { s, t -> s + t.amount } }
        val spentByCategory = expenses.groupBy { it.categoryId }.mapValues { (_, list) -> list.fold(zero) { s, t -> s + t.amount } }

        val bars = (1..month.lengthInDays).map { d ->
            val date = LocalDate(month.year, month.month, d)
            DayBarState(date, spentByDay[date] ?: zero, isToday = date == today, isFuture = date > today, isSelected = date == selected)
        }
        val days = entries
            .filter { selected == null || it.logicalDate == selected }
            .groupBy { it.logicalDate }
            .entries
            .sortedByDescending { it.key }
            .map { (date, list) ->
                DayGroupState(
                    date = date,
                    label = dayLabel(date, today),
                    spent = spentByDay[date] ?: zero,
                    entries = list.map { it.toRow(byId, clock.timeZone()) },
                )
            }

        SpendUiState(
            isLoading = false,
            month = month,
            spentThisMonth = expenses.fold(zero) { s, t -> s + t.amount },
            bars = bars,
            selectedDay = selected,
            days = days,
            budgets = monthBudgets.filter { it.limit.currency == currency }
                .map { budgetStatus(it, spentByCategory[it.categoryId] ?: zero).toRow(byId) }
                .sortedByDescending { it.percentUsed },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SpendUiState(true, month, Money.zero(currency), emptyList(), null, emptyList(), emptyList()),
    )

    /** Tap a bar to see one day; tap it again, or pass null, to see the month. */
    fun onSelectDay(date: LocalDate?) {
        selectedDay.value = if (date == selectedDay.value) null else date
    }

    fun onDeleteEntry(id: String) {
        viewModelScope.launch { transactions.delete(id) }
    }
}
