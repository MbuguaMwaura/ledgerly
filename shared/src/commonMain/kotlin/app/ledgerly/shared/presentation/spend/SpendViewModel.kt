package app.ledgerly.shared.presentation.spend

import app.ledgerly.shared.presentation.common.currency
import app.ledgerly.shared.domain.repository.SettingsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetPeriod
import app.ledgerly.shared.domain.model.Category
import app.ledgerly.shared.domain.model.Transaction
import app.ledgerly.shared.presentation.common.dayMonth
import app.ledgerly.shared.presentation.common.period
import app.ledgerly.shared.presentation.common.periodLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
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
    /** The budget month on screen: the calendar month, or payday to payday. */
    val period: BudgetPeriod,
    /** "Spent in November", or "Spent since 25 Oct" when months start on payday. */
    val title: String,
    val spentThisMonth: Money,
    /** Days from today to the end of the budget month, today included. */
    val daysLeft: Int,
    /** Where spending is heading by the month's end at today's rate. Null before anything is spent. */
    val pace: Money?,
    /** "24 Nov", the budget month's last day. */
    val endLabel: String,
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
    settings: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val today = clock.today()
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SpendUiState> = settings.period(clock).flatMapLatest { period ->
        combine(
            transactions.observeBetween(period.firstDay, period.lastDay),
            budgets.observeMonth(period.month),
            categories.observeAll(),
            selectedDay,
            settings.currency(),
        ) { entries, monthBudgets, all, selected, currency -> state(period, entries, monthBudgets, all, selected, currency) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BudgetPeriod.of(today).let { period ->
            SpendUiState(
                true, period, "", Money.zero(CurrencyCode.KES), daysLeft = period.lengthInDays, pace = null, endLabel = "",
                bars = emptyList(), selectedDay = null, days = emptyList(), budgets = emptyList(),
            )
        },
    )

    private fun state(
        period: BudgetPeriod,
        entries: List<Transaction>,
        monthBudgets: List<Budget>,
        all: List<Category>,
        selected: LocalDate?,
        currency: CurrencyCode,
    ): SpendUiState {
        val zero = Money.zero(currency)
        val byId = all.associateBy { it.id }
        val expenses = entries.filter { it.kind == EntryKind.Expense && it.amount.currency == currency }
        val spentByDay = expenses.groupBy { it.logicalDate }.mapValues { (_, list) -> list.fold(zero) { s, t -> s + t.amount } }
        val spentByCategory = expenses.groupBy { it.categoryId }.mapValues { (_, list) -> list.fold(zero) { s, t -> s + t.amount } }

        val bars = period.days.map { date ->
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

        val spent = expenses.fold(zero) { s, t -> s + t.amount }
        val dayNumber = if (period.contains(today)) period.dayNumber(today) else period.lengthInDays
        return SpendUiState(
            isLoading = false,
            period = period,
            title = if (period.startDay == 1) "Spent in ${periodLabel(period)}" else "Spent since ${dayMonth(period.firstDay)}",
            spentThisMonth = spent,
            daysLeft = period.lengthInDays - dayNumber + 1,
            pace = if (spent.minorUnits > 0 && period.contains(today)) Money(spent.minorUnits / dayNumber * period.lengthInDays, currency) else null,
            endLabel = dayMonth(period.lastDay),
            bars = bars,
            selectedDay = selected,
            days = days,
            budgets = monthBudgets.filter { it.limit.currency == currency }
                .map { budgetStatus(it, spentByCategory[it.categoryId] ?: zero).toRow(byId) }
                .sortedByDescending { it.percentUsed },
        )
    }

    /** Tap a bar to see one day; tap it again, or pass null, to see the month. */
    fun onSelectDay(date: LocalDate?) {
        selectedDay.value = if (date == selectedDay.value) null else date
    }

    fun onDeleteEntry(id: String) {
        viewModelScope.launch { transactions.delete(id) }
    }
}
