package app.ledgerly.shared.domain.rules

import app.ledgerly.shared.domain.model.Debt
import kotlinx.datetime.LocalDate

enum class ReminderTone { Friendly, Clear, Firm }

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun LocalDate.short() = "$dayOfMonth ${MONTHS[monthNumber - 1]}"

/**
 * The message the user can send from the share sheet. Written in the user's voice, polite in
 * every tone; Firm is direct, never rude.
 */
fun draftReminder(debt: Debt, tone: ReminderTone, today: LocalDate): String {
    val name = debt.counterparty.trim().substringBefore(' ').ifEmpty { "there" }
    val amount = debt.remaining.parts().format(withFraction = false)
    val due = debt.dueDate
    return when (tone) {
        ReminderTone.Friendly -> buildString {
            append("Hi $name, just a friendly nudge about the $amount")
            if (due != null) append(" that was due on ${due.short()}")
            append(". No stress, let me know when works for you.")
        }
        ReminderTone.Clear -> buildString {
            append("Hi $name, a reminder that $amount is still outstanding")
            if (due != null) append(if (due < today) ", due on ${due.short()}" else ", due ${due.short()}")
            append(". Could you let me know when you can send it?")
        }
        ReminderTone.Firm -> buildString {
            append("Hi $name, the $amount")
            if (due != null && due < today) append(" was due on ${due.short()} and")
            append(" is still unpaid. Please send it this week, or let me know today when you will.")
        }
    }
}
