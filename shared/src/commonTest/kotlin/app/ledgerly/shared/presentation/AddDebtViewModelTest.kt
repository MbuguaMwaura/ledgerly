package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.rules.DueStatus
import app.ledgerly.shared.domain.usecase.AddDebt
import app.ledgerly.shared.presentation.adddebt.AddDebtViewModel
import app.ledgerly.shared.presentation.adddebt.DueChoice
import app.ledgerly.shared.presentation.adddebt.dueChoices
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddDebtViewModelTest : ViewModelTest() {
    private fun viewModel() = AddDebtViewModel(debts, AddDebt(debts, clock), settings, clock)

    @Test
    fun offersDueDatesFromToday() {
        // Wednesday 25 November 2026.
        val choices = dueChoices(LocalDate(2026, 11, 25)).associateBy { it.choice }
        assertEquals(LocalDate(2026, 11, 27), choices.getValue(DueChoice.ThisFriday).date)
        assertEquals(LocalDate(2026, 12, 2), choices.getValue(DueChoice.InAWeek).date)
        assertEquals(LocalDate(2026, 12, 15), choices.getValue(DueChoice.MidNextMonth).date)
        assertEquals("Mid December", choices.getValue(DueChoice.MidNextMonth).label)
        assertNull(choices.getValue(DueChoice.NoDate).date)

        // On a Friday, "This Friday" would be today, so it offers the next one.
        val friday = dueChoices(LocalDate(2026, 11, 27)).first { it.choice == DueChoice.ThisFriday }
        assertEquals("Next Friday", friday.label)
        assertEquals(LocalDate(2026, 12, 4), friday.date)
    }

    @Test
    fun needsAnAmountAndAName() = test {
        val vm = viewModel()
        vm.uiState.test {
            val empty = awaitItem()
            assertFalse(empty.canSave)
            assertNull(empty.preview)
            assertEquals("Enter an amount and a name", empty.saveLabel)

            vm.onAmountChange("2,000")
            assertFalse(awaitState { it.amountText == "2000" }.canSave)
            vm.onCounterpartyChange("Otieno")
            val ready = awaitState { it.canSave }
            assertEquals("Add KES 2,000", ready.saveLabel)
            assertEquals("Otieno", ready.preview?.counterparty)
            assertEquals("Due Wed 2 Dec.", ready.dueText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savesTheDebt() = test {
        val vm = viewModel()
        vm.uiState.test {
            vm.onDirectionChange(DebtDirection.IOwe)
            vm.onAmountChange("1500")
            vm.onCounterpartyChange("  Mama Njeri ")
            vm.onNoteChange("Groceries")
            vm.onDueChange(DueChoice.ThisFriday)
            val ready = awaitState { it.canSave && it.due == DueChoice.ThisFriday }
            assertEquals("Who you owe", ready.whoLabel)
            assertEquals(DueStatus.DueSoon(2), ready.preview?.status)

            vm.onSave()
            val saved = awaitState { it.savedMessage != null }
            assertEquals("Added Mama Njeri.", saved.savedMessage)
            assertFalse(saved.canSave)
            cancelAndIgnoreRemainingEvents()
        }
        val debt = debts.observeAll().first().single()
        assertEquals(DebtDirection.IOwe, debt.direction)
        assertEquals("Mama Njeri", debt.counterparty)
        assertEquals(kes(1_500), debt.principal)
        assertEquals(LocalDate(2026, 11, 27), debt.dueDate)
        assertEquals("Groceries", debt.note)
    }

    @Test
    fun suggestsPeopleAlreadyInDebts() = test {
        val add = AddDebt(debts, clock)
        add(DebtDirection.OwedToMe, "Otieno", kes(500), null)
        add(DebtDirection.OwedToMe, "otieno", kes(300), null)
        add(DebtDirection.IOwe, "Bank", kes(9_000), null)
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(1, awaitState { it.people.isNotEmpty() }.people.size)
            vm.onDirectionChange(DebtDirection.IOwe)
            assertEquals(listOf("Bank"), awaitState { it.direction == DebtDirection.IOwe }.people)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
