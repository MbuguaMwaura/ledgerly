package app.ledgerly.shared.domain.usecase

import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.repository.BudgetRepository
import app.ledgerly.shared.domain.repository.DebtRepository
import app.ledgerly.shared.domain.repository.LogRepository
import app.ledgerly.shared.domain.repository.TransactionRepository
import app.ledgerly.shared.domain.rules.BudgetStatus
import app.ledgerly.shared.domain.rules.DueStatus
import app.ledgerly.shared.domain.rules.LeftToSpend
import app.ledgerly.shared.domain.rules.StreakState
import app.ledgerly.shared.domain.rules.budgetStatus
import app.ledgerly.shared.domain.rules.budgetsNeedingALook
import app.ledgerly.shared.domain.rules.calculateStreak
import app.ledgerly.shared.domain.rules.dueStatus
import app.ledgerly.shared.domain.rules.leftToSpend
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HomeSummary(
    val month: BudgetMonth,
    val leftToSpend: LeftToSpend,
    val income: Money,
    val budgets: List<BudgetStatus>,
    /** Budgets at 80% or more, most used first. */
    val needsALook: List<BudgetStatus>,
    val streak: StreakState,
    /** Unsettled debts owed to the user that are overdue or due within 3 days. */
    val debtsDueSoon: List<Pair<Debt, DueStatus>>,
)

/** Everything the Home tab shows for the current month, recomputed when any of it changes. */
class ObserveHomeSummary(
    private val transactions: TransactionRepository,
    private val budgets: BudgetRepository,
    private val logs: LogRepository,
    private val debts: DebtRepository,
    private val clock: AppClock,
    private val currency: CurrencyCode = CurrencyCode.KES,
) {
    operator fun invoke(): Flow<HomeSummary> {
        val today = clock.today()
        val month = BudgetMonth.of(today)
        return combine(
            transactions.observeBetween(month.firstDay, month.lastDay),
            budgets.observeMonth(month),
            logs.observeLoggedDays(),
            debts.observeAll(),
        ) { entries, monthBudgets, loggedDays, allDebts ->
            val zero = Money.zero(currency)
            val expenses = entries.filter { it.kind == EntryKind.Expense && it.amount.currency == currency }
            val spentByCategory = expenses.groupBy { it.categoryId }
                .mapValues { (_, list) -> list.fold(zero) { sum, t -> sum + t.amount } }
            val statuses = monthBudgets.filter { it.limit.currency == currency }
                .map { budgetStatus(it, spentByCategory[it.categoryId] ?: zero) }
            val totalLimit = statuses.fold(zero) { sum, s -> sum + s.budget.limit }
            val totalSpent = expenses.fold(zero) { sum, t -> sum + t.amount }
            val income = entries.filter { it.kind == EntryKind.Income && it.amount.currency == currency }
                .fold(zero) { sum, t -> sum + t.amount }
            val dueSoon = allDebts.filter { it.direction == DebtDirection.OwedToMe }
                .map { it to dueStatus(it, today) }
                .filter { (_, status) -> status is DueStatus.Overdue || status is DueStatus.DueSoon }

            HomeSummary(
                month = month,
                leftToSpend = leftToSpend(totalLimit, totalSpent, month, today),
                income = income,
                budgets = statuses,
                needsALook = budgetsNeedingALook(statuses),
                streak = calculateStreak(loggedDays, today),
                debtsDueSoon = dueSoon,
            )
        }
    }
}
