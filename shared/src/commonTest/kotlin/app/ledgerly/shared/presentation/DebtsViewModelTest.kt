package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.rules.ReminderTone
import app.ledgerly.shared.domain.usecase.AddDebt
import app.ledgerly.shared.domain.usecase.MarkReminded
import app.ledgerly.shared.domain.usecase.RecordDebtPayment
import app.ledgerly.shared.presentation.debts.DebtsViewModel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebtsViewModelTest : ViewModelTest() {
    private fun viewModel() = DebtsViewModel(debts, RecordDebtPayment(debts, clock), MarkReminded(debts, clock), settings, clock)

    private val add get() = AddDebt(debts, clock)

    @Test
    fun sortsMoneyOwedIntoOverdueDueSoonAndLater() = test {
        add(DebtDirection.OwedToMe, "Late Larry", kes(1_000), LocalDate(2026, 11, 20))
        add(DebtDirection.OwedToMe, "Later Lucy", kes(1_000), LocalDate(2026, 11, 10))
        add(DebtDirection.OwedToMe, "Soon Sam", kes(2_000), LocalDate(2026, 11, 27))
        add(DebtDirection.OwedToMe, "Someday Sue", kes(500), null)
        add(DebtDirection.IOwe, "Sacco", kes(9_000), LocalDate(2026, 12, 5))

        viewModel().uiState.test {
            val state = awaitState { !it.isLoading && it.overdue.size == 2 }
            // Longest overdue first.
            assertEquals(listOf("Later Lucy", "Late Larry"), state.overdue.map { it.counterparty })
            assertEquals(listOf("Soon Sam"), state.dueSoon.map { it.counterparty })
            assertEquals(listOf("Someday Sue"), state.later.map { it.counterparty })
            assertEquals(kes(4_500), state.totalOwedToMe)
            assertEquals(kes(9_000), state.totalIOwe)
        }
    }

    @Test
    fun switchingDirectionShowsWhatTheUserOwes() = test {
        add(DebtDirection.OwedToMe, "Wanjiru", kes(1_000), null)
        add(DebtDirection.IOwe, "Sacco", kes(9_000), LocalDate(2026, 12, 5))
        val vm = viewModel()
        vm.uiState.test {
            awaitState { !it.isLoading && it.later.isNotEmpty() }
            vm.onSelectDirection(DebtDirection.IOwe)
            val state = awaitState { it.direction == DebtDirection.IOwe }
            assertEquals(listOf("Sacco"), state.later.map { it.counterparty })
        }
    }

    @Test
    fun paymentInFullMovesTheDebtToSettled() = test {
        val debt = add(DebtDirection.OwedToMe, "Wanjiru", kes(2_000), LocalDate(2026, 11, 27))
        val vm = viewModel()
        vm.uiState.test {
            awaitState { it.dueSoon.isNotEmpty() }
            vm.onRecordPayment(debt.id, kes(500))
            assertEquals(kes(1_500), awaitState { it.dueSoon.firstOrNull()?.paid == kes(500) }.dueSoon.single().remaining)
            vm.onRecordPayment(debt.id, kes(1_500))
            val state = awaitState { it.settled.isNotEmpty() }
            assertTrue(state.dueSoon.isEmpty())
            assertEquals(kes(0), state.totalOwedToMe)
        }
    }

    @Test
    fun draftsAReminderAndRecordsWhenItWasSent() = test {
        val debt = add(DebtDirection.OwedToMe, "Wanjiru Kamau", kes(2_000), LocalDate(2026, 11, 20))
        val vm = viewModel()
        vm.uiState.test {
            assertTrue(awaitState { it.overdue.isNotEmpty() }.overdue.single().canRemind)
            val draft = assertNotNull(vm.reminderDraft(debt.id, ReminderTone.Friendly))
            assertTrue(draft.startsWith("Hi Wanjiru"), draft)
            assertTrue("2,000" in draft, draft)

            vm.onReminderSent(debt.id)
            val row = awaitState { it.overdue.single().daysSinceReminder == 0 }.overdue.single()
            assertEquals(0, row.daysSinceReminder)
        }
        assertNull(vm.reminderDraft("missing", ReminderTone.Firm))
    }
}
