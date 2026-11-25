package app.ledgerly.shared.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.repository.CategoryRepository
import app.ledgerly.shared.domain.rules.StreakState
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.domain.usecase.LogNothingSpent
import app.ledgerly.shared.domain.usecase.ObserveHomeSummary
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.common.BudgetRowState
import app.ledgerly.shared.presentation.common.DebtRowState
import app.ledgerly.shared.presentation.common.toRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean,
    /** The headline: what's left across every budget this month. Negative when overspent. */
    val leftToSpend: Money,
    /** "KES 3,075 a day for the last 6 days." */
    val perDay: Money,
    val daysLeft: Int,
    val spent: Money,
    val income: Money,
    val totalBudget: Money,
    /** For the Budgets gauge. Can pass 100. */
    val budgetPercentUsed: Int,
    /** Categories at 80% or more, most used first. */
    val needsALook: List<BudgetRowState>,
    val streak: StreakState,
    /** Money owed to the user that is overdue or due within 3 days. */
    val debtsDueSoon: List<DebtRowState>,
) {
    companion object {
        fun loading(currency: CurrencyCode = CurrencyCode.KES): HomeUiState {
            val zero = Money.zero(currency)
            return HomeUiState(
                isLoading = true, leftToSpend = zero, perDay = zero, daysLeft = 0, spent = zero, income = zero,
                totalBudget = zero, budgetPercentUsed = 0, needsALook = emptyList(),
                streak = StreakState(0, 0, loggedToday = false, freezesHeld = 0, frozenDays = emptyList(), milestoneToday = null),
                debtsDueSoon = emptyList(),
            )
        }
    }
}

class HomeViewModel(
    observeHomeSummary: ObserveHomeSummary,
    categories: CategoryRepository,
    private val logNothingSpent: LogNothingSpent,
    private val clock: AppClock,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(observeHomeSummary(), categories.observeAll()) { summary, all ->
        val byId = all.associateBy { it.id }
        val today = clock.today()
        val left = summary.leftToSpend
        HomeUiState(
            isLoading = false,
            leftToSpend = left.left,
            perDay = left.perDay,
            daysLeft = left.daysLeft,
            spent = left.spent,
            income = summary.income,
            totalBudget = left.totalLimit,
            budgetPercentUsed = if (left.totalLimit.minorUnits > 0) (left.spent.minorUnits * 100 / left.totalLimit.minorUnits).toInt() else 0,
            needsALook = summary.needsALook.map { it.toRow(byId) },
            streak = summary.streak,
            debtsDueSoon = summary.debtsDueSoon.map { (debt, _) -> debt.toRow(today, clock.timeZone()) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.loading())

    /** The "Nothing spent today" button on the streak card. */
    fun onNothingSpentToday() {
        viewModelScope.launch { logNothingSpent() }
    }
}
