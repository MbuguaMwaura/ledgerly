package app.ledgerly.shared.domain

import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.model.DebtPayment
import app.ledgerly.shared.domain.rules.DueStatus
import app.ledgerly.shared.domain.rules.ReminderTone
import app.ledgerly.shared.domain.rules.canSuggestReminder
import app.ledgerly.shared.domain.rules.daysSinceReminder
import app.ledgerly.shared.domain.rules.draftReminder
import app.ledgerly.shared.domain.rules.dueStatus
import app.ledgerly.shared.money.CurrencyCode.KES
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.testing.NAIROBI
import app.ledgerly.shared.testing.nairobi
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebtRulesTest {
    private val today = LocalDate(2026, 11, 25)

    private fun debt(
        due: LocalDate?,
        principal: Long = 2_000,
        paid: List<Long> = emptyList(),
        direction: DebtDirection = DebtDirection.OwedToMe,
        remindedAt: String? = null,
    ) = Debt(
        id = "d1",
        direction = direction,
        counterparty = "Otieno Kamau",
        principal = Money.ofMajor(principal, KES),
        dueDate = due,
        note = null,
        createdAt = nairobi("2026-11-01T10:00"),
        lastRemindedAt = remindedAt?.let(::nairobi),
        payments = paid.mapIndexed { i, p -> DebtPayment("p$i", "d1", Money.ofMajor(p, KES), nairobi("2026-11-10T10:00")) },
    )

    @Test
    fun dueStatuses() {
        assertEquals(DueStatus.Overdue(2), dueStatus(debt(LocalDate(2026, 11, 23)), today))
        assertEquals(DueStatus.DueSoon(0), dueStatus(debt(today), today))
        assertEquals(DueStatus.DueSoon(3), dueStatus(debt(LocalDate(2026, 11, 28)), today))
        assertEquals(DueStatus.Upcoming(4), dueStatus(debt(LocalDate(2026, 11, 29)), today))
        assertEquals(DueStatus.NoDueDate, dueStatus(debt(null), today))
        assertEquals(DueStatus.Settled, dueStatus(debt(LocalDate(2026, 11, 1), paid = listOf(1_500, 500)), today))
    }

    @Test
    fun remainingNeverGoesBelowZero() {
        val overpaid = debt(today, principal = 1_000, paid = listOf(1_200))
        assertEquals(Money.zero(KES), overpaid.remaining)
        assertTrue(overpaid.isSettled)
    }

    @Test
    fun remindersStartAtTheDueDateThenWeekly() {
        assertFalse(canSuggestReminder(debt(LocalDate(2026, 11, 26)), today, NAIROBI), "not before it is due")
        assertTrue(canSuggestReminder(debt(today), today, NAIROBI))
        assertFalse(canSuggestReminder(debt(LocalDate(2026, 11, 10), remindedAt = "2026-11-21T09:00"), today, NAIROBI))
        assertTrue(canSuggestReminder(debt(LocalDate(2026, 11, 10), remindedAt = "2026-11-18T09:00"), today, NAIROBI))
        assertEquals(4, daysSinceReminder(debt(today, remindedAt = "2026-11-21T09:00"), today, NAIROBI))
    }

    @Test
    fun noRemindersForLoansOrSettledDebts() {
        assertFalse(canSuggestReminder(debt(today, direction = DebtDirection.IOwe), today, NAIROBI))
        assertFalse(canSuggestReminder(debt(today, paid = listOf(2_000)), today, NAIROBI))
    }

    @Test
    fun draftsUseTheFirstNameAndWhatIsLeft() {
        val owed = debt(LocalDate(2026, 11, 20), paid = listOf(500))
        assertEquals(
            "Hi Otieno, just a friendly nudge about the KES 1,500 that was due on 20 Nov. No stress, let me know when works for you.",
            draftReminder(owed, ReminderTone.Friendly, today),
        )
        assertEquals(
            "Hi Otieno, a reminder that KES 1,500 is still outstanding, due on 20 Nov. Could you let me know when you can send it?",
            draftReminder(owed, ReminderTone.Clear, today),
        )
        assertEquals(
            "Hi Otieno, the KES 1,500 was due on 20 Nov and is still unpaid. Please send it this week, or let me know today when you will.",
            draftReminder(owed, ReminderTone.Firm, today),
        )
    }
}
