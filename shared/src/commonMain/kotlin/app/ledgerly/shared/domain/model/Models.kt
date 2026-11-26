package app.ledgerly.shared.domain.model

import app.ledgerly.shared.money.Money
import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class EntryKind { Expense, Income }

data class Category(
    val id: String,
    val name: String,
    /** A platform-neutral icon key, e.g. "food". Each UI maps it to its own icon set. */
    val icon: String,
    val kind: EntryKind,
)

/** One logged expense or income. [amount] is always positive; [kind] gives the direction. */
data class Transaction(
    val id: String,
    val kind: EntryKind,
    val amount: Money,
    val categoryId: String,
    val note: String?,
    val occurredAt: Instant,
    /** The day this entry counts for, after the 03:00 cutoff. Stored so queries by day stay simple. */
    val logicalDate: LocalDate,
)

/** "Nothing spent today": counts as a log for the streak without adding a transaction. */
data class NothingSpentDay(val date: LocalDate)

/** A monthly limit for one category. */
data class Budget(
    val categoryId: String,
    val month: BudgetMonth,
    val limit: Money,
)

data class BudgetMonth(val year: Int, val month: Int) : Comparable<BudgetMonth> {
    init {
        require(month in 1..12) { "month must be 1..12, was $month" }
    }

    val firstDay: LocalDate get() = LocalDate(year, month, 1)

    val lengthInDays: Int
        get() = when (month) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

    val lastDay: LocalDate get() = LocalDate(year, month, lengthInDays)

    fun contains(date: LocalDate): Boolean = date.year == year && date.monthNumber == month

    override fun compareTo(other: BudgetMonth): Int = compareValuesBy(this, other, { it.year }, { it.month })

    fun previous(): BudgetMonth = if (month == 1) BudgetMonth(year - 1, 12) else BudgetMonth(year, month - 1)

    fun next(): BudgetMonth = if (month == 12) BudgetMonth(year + 1, 1) else BudgetMonth(year, month + 1)

    companion object {
        fun of(date: LocalDate) = BudgetMonth(date.year, date.monthNumber)
    }
}

/** The days a budget month can start on. Capped at 28 so every month has the day. */
val MONTH_START_DAYS: IntRange = 1..28

/**
 * A budget month as the user counts it: from their payday to the day before the next one.
 * With [startDay] 1 it's the calendar month. Otherwise the period is named after the month it
 * ends in, so 25 Oct – 24 Nov is November's: moving the start day never moves a budget month
 * backwards, and budgets set for it keep applying.
 */
data class BudgetPeriod(val month: BudgetMonth, val startDay: Int = 1) {
    init {
        require(startDay in MONTH_START_DAYS) { "startDay must be 1..28, was $startDay" }
    }

    val firstDay: LocalDate =
        if (startDay == 1) month.firstDay else month.previous().firstDay.plus(startDay - 1, DateTimeUnit.DAY)

    val lastDay: LocalDate = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    val lengthInDays: Int get() = firstDay.daysUntil(lastDay) + 1

    fun contains(date: LocalDate): Boolean = date in firstDay..lastDay

    /** Which day of the period [date] is, from 1. */
    fun dayNumber(date: LocalDate): Int = firstDay.daysUntil(date) + 1

    val days: List<LocalDate> get() = (0 until lengthInDays).map { firstDay.plus(it, DateTimeUnit.DAY) }

    companion object {
        fun of(date: LocalDate, startDay: Int = 1): BudgetPeriod = when {
            startDay == 1 -> BudgetPeriod(BudgetMonth.of(date), 1)
            date.dayOfMonth >= startDay -> BudgetPeriod(BudgetMonth.of(date).next(), startDay)
            else -> BudgetPeriod(BudgetMonth.of(date), startDay)
        }
    }
}

enum class DebtDirection {
    /** Someone owes the user (a debtor). */
    OwedToMe,

    /** The user owes someone (a loan). */
    IOwe,
}

data class Debt(
    val id: String,
    val direction: DebtDirection,
    val counterparty: String,
    val principal: Money,
    val dueDate: LocalDate?,
    val note: String?,
    val createdAt: Instant,
    val lastRemindedAt: Instant?,
    val payments: List<DebtPayment>,
) {
    val paid: Money get() = payments.fold(Money.zero(principal.currency)) { sum, p -> sum + p.amount }

    /** Never below zero, even if more was paid than borrowed. */
    val remaining: Money
        get() = (principal - paid).let { if (it.isNegative) Money.zero(principal.currency) else it }

    val isSettled: Boolean get() = remaining.minorUnits == 0L
}

data class DebtPayment(
    val id: String,
    val debtId: String,
    val amount: Money,
    val paidAt: Instant,
)
