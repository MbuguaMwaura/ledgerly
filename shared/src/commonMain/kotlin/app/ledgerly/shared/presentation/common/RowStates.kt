package app.ledgerly.shared.presentation.common

import app.ledgerly.shared.domain.model.Category
import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.model.Transaction
import app.ledgerly.shared.domain.rules.BudgetStatus
import app.ledgerly.shared.domain.rules.BudgetTone
import app.ledgerly.shared.domain.rules.DueStatus
import app.ledgerly.shared.domain.rules.canSuggestReminder
import app.ledgerly.shared.domain.rules.daysSinceReminder
import app.ledgerly.shared.domain.rules.dueStatus
import app.ledgerly.shared.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/*
 * Row models shared by several screens. They hold display-ready values, so each UI only
 * lays them out and styles them; no rule is evaluated twice.
 */

data class BudgetRowState(
    val categoryId: String,
    val categoryName: String,
    val icon: String,
    val spent: Money,
    val limit: Money,
    val percentUsed: Int,
    val tone: BudgetTone,
    val overBy: Money,
    val left: Money,
)

data class EntryRowState(
    val id: String,
    val kind: EntryKind,
    /** The note if there is one, else the category name. */
    val title: String,
    val categoryName: String,
    val icon: String,
    /** Local time, "14:05". */
    val time: String,
    /** Negative for expenses, positive for income: "− KES 650", "+ KES 4,000". */
    val signedAmount: Money,
)

data class DebtRowState(
    val id: String,
    val direction: DebtDirection,
    val counterparty: String,
    val principal: Money,
    val paid: Money,
    val remaining: Money,
    /** 0..100, for the paid-off bar on loans. */
    val percentPaid: Int,
    val dueDate: LocalDate?,
    val status: DueStatus,
    val canRemind: Boolean,
    /** For "Reminded 4 days ago". */
    val daysSinceReminder: Int?,
)

internal fun BudgetStatus.toRow(categories: Map<String, Category>): BudgetRowState {
    val category = categories[budget.categoryId]
    return BudgetRowState(
        categoryId = budget.categoryId,
        categoryName = category?.name ?: budget.categoryId,
        icon = category?.icon ?: "dots",
        spent = spent,
        limit = budget.limit,
        percentUsed = percentUsed,
        tone = tone,
        overBy = overBy,
        left = left,
    )
}

internal fun Transaction.toRow(categories: Map<String, Category>, timeZone: TimeZone): EntryRowState {
    val category = categories[categoryId]
    val local = occurredAt.toLocalDateTime(timeZone)
    return EntryRowState(
        id = id,
        kind = kind,
        title = note ?: category?.name ?: categoryId,
        categoryName = category?.name ?: categoryId,
        icon = category?.icon ?: "dots",
        time = "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}",
        signedAmount = if (kind == EntryKind.Expense) -amount else amount,
    )
}

internal fun Debt.toRow(today: LocalDate, timeZone: TimeZone): DebtRowState = DebtRowState(
    id = id,
    direction = direction,
    counterparty = counterparty,
    principal = principal,
    paid = paid,
    remaining = remaining,
    percentPaid = if (principal.minorUnits == 0L) 100 else (minOf(paid.minorUnits, principal.minorUnits) * 100 / principal.minorUnits).toInt(),
    dueDate = dueDate,
    status = dueStatus(this, today),
    canRemind = canSuggestReminder(this, today, timeZone),
    daysSinceReminder = daysSinceReminder(this, today, timeZone),
)

private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "Today", "Yesterday", or "Wed, 25 Nov". */
internal fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    LocalDate.fromEpochDays(today.toEpochDays() - 1) -> "Yesterday"
    else -> "${WEEKDAYS[date.dayOfWeek.ordinal]}, ${date.dayOfMonth} ${MONTHS[date.monthNumber - 1]}"
}
