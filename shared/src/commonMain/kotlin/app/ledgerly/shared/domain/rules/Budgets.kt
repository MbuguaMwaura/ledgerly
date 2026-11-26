package app.ledgerly.shared.domain.rules

import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetPeriod
import app.ledgerly.shared.money.Money
import kotlinx.datetime.LocalDate

enum class BudgetTone {
    /** Below 80% of the limit. */
    OnTrack,

    /** 80% to 99%. */
    Caution,

    /** 100% or more. */
    Over,
}

data class BudgetStatus(
    val budget: Budget,
    val spent: Money,
    /** Whole percent spent, rounded down. Can pass 100. */
    val percentUsed: Int,
    val tone: BudgetTone,
    /** How far past the limit, when [tone] is [BudgetTone.Over]. Zero otherwise. */
    val overBy: Money,
    val left: Money,
)

const val CAUTION_PERCENT = 80

fun budgetStatus(budget: Budget, spent: Money): BudgetStatus {
    val limit = budget.limit.minorUnits
    val percent = when {
        limit > 0 -> (spent.minorUnits * 100 / limit).toInt()
        spent.minorUnits > 0 -> 100
        else -> 0
    }
    val tone = when {
        percent >= 100 -> BudgetTone.Over
        percent >= CAUTION_PERCENT -> BudgetTone.Caution
        else -> BudgetTone.OnTrack
    }
    val difference = budget.limit - spent
    val zero = Money.zero(budget.limit.currency)
    return BudgetStatus(
        budget = budget,
        spent = spent,
        percentUsed = percent,
        tone = tone,
        overBy = if (difference.isNegative) -difference else zero,
        left = if (difference.isNegative) zero else difference,
    )
}

data class LeftToSpend(
    val totalLimit: Money,
    val spent: Money,
    /** Can be negative when the month's budgets are overspent. */
    val left: Money,
    /** Days from today to the end of the month, today included. */
    val daysLeft: Int,
    /** What can be spent per remaining day, never below zero. */
    val perDay: Money,
)

/** The Home headline: what's left across every budget this month, and per day from today. */
fun leftToSpend(totalLimit: Money, spent: Money, period: BudgetPeriod, today: LocalDate): LeftToSpend {
    val left = totalLimit - spent
    val daysLeft = if (period.contains(today)) period.lengthInDays - period.dayNumber(today) + 1 else period.lengthInDays
    val perDay = if (left.isNegative) Money.zero(left.currency) else Money(left.minorUnits / daysLeft, left.currency)
    return LeftToSpend(totalLimit, spent, left, daysLeft, perDay)
}

/** Categories Home lists under the gauge: 80% or more, the most used first. */
fun budgetsNeedingALook(statuses: List<BudgetStatus>): List<BudgetStatus> =
    statuses.filter { it.tone != BudgetTone.OnTrack }.sortedByDescending { it.percentUsed }
