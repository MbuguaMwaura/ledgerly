package app.ledgerly.shared.domain.rules

import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.time.logicalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

const val DUE_SOON_DAYS = 3
const val REMINDER_INTERVAL_DAYS = 7

sealed interface DueStatus {
    data object Settled : DueStatus

    data object NoDueDate : DueStatus

    data class Overdue(val days: Int) : DueStatus

    /** Due today or within [DUE_SOON_DAYS] days. `days == 0` means today. */
    data class DueSoon(val days: Int) : DueStatus

    data class Upcoming(val days: Int) : DueStatus
}

fun dueStatus(debt: Debt, today: LocalDate): DueStatus {
    if (debt.isSettled) return DueStatus.Settled
    val due = debt.dueDate ?: return DueStatus.NoDueDate
    val days = today.daysUntil(due)
    return when {
        days < 0 -> DueStatus.Overdue(-days)
        days <= DUE_SOON_DAYS -> DueStatus.DueSoon(days)
        else -> DueStatus.Upcoming(days)
    }
}

/**
 * Ledgerly never messages anyone itself; it suggests a reminder the user sends. A reminder is
 * suggested from the due date, then at most once every [REMINDER_INTERVAL_DAYS] days.
 * Only debts owed to the user get reminders.
 */
fun canSuggestReminder(debt: Debt, today: LocalDate, timeZone: TimeZone): Boolean {
    if (debt.direction != app.ledgerly.shared.domain.model.DebtDirection.OwedToMe || debt.isSettled) return false
    val due = debt.dueDate ?: return false
    if (today < due) return false
    val last = debt.lastRemindedAt?.let { logicalDate(it, timeZone) } ?: return true
    return last <= today.minus(REMINDER_INTERVAL_DAYS, DateTimeUnit.DAY)
}

/** "Reminded 4 days ago" on the row. Null if never reminded. */
fun daysSinceReminder(debt: Debt, today: LocalDate, timeZone: TimeZone): Int? =
    debt.lastRemindedAt?.let { logicalDate(it, timeZone).daysUntil(today) }
