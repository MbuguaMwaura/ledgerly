package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.presentation.transaction.TransactionDetailViewModel
import app.ledgerly.shared.testing.nairobi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionDetailViewModelTest : ViewModelTest() {
    private fun viewModel() = TransactionDetailViewModel(transactions, categories, budgets, settings, clock)

    @Test
    fun showsTheEntryAndHowItFits() = test {
        budgets.save(Budget("food", BudgetMonth(2026, 11), kes(10_000)), clock.now())
        val log = LogEntry(transactions, clock)
        log(EntryKind.Expense, kes(3_000), "food", occurredAt = nairobi("2026-11-24T12:00"))
        val entry = log(EntryKind.Expense, kes(650), "food", "Lunch", occurredAt = nairobi("2026-11-25T13:05"))
        val vm = viewModel()
        vm.load(entry.id)
        vm.uiState.test {
            val state = awaitState { !it.isLoading }
            assertEquals("Expense", state.kindLabel)
            assertEquals("Lunch", state.title)
            assertEquals(-kes(650), state.signedAmount)
            assertEquals("Today · 13:05", state.whenText)
            assertEquals("650", state.amountText)
            assertTrue(state.categories.all { it.kind == EntryKind.Expense })
            assertEquals(kes(3_650), state.budget?.spent)
            assertEquals("Delete expense", state.deleteLabel)
            assertFalse(state.canSave)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savesEditsUnderTheSameEntry() = test {
        val entry = LogEntry(transactions, clock)(EntryKind.Expense, kes(650), "food", "Lunch")
        val vm = viewModel()
        vm.load(entry.id)
        vm.uiState.test {
            awaitState { !it.isLoading }
            vm.onAmountChange("700")
            vm.onSelectCategory("transport")
            vm.onNoteChange("")
            assertTrue(awaitState { it.note.isEmpty() }.canSave)
            vm.onSave()
            awaitState { it.isDone }
            cancelAndIgnoreRemainingEvents()
        }
        val saved = transactions.get(entry.id)!!
        assertEquals(kes(700), saved.amount)
        assertEquals("transport", saved.categoryId)
        assertNull(saved.note)
        assertEquals(entry.occurredAt, saved.occurredAt)
    }

    @Test
    fun deletesTheEntry() = test {
        val entry = LogEntry(transactions, clock)(EntryKind.Income, kes(4_000), "salary")
        val vm = viewModel()
        vm.load(entry.id)
        vm.uiState.test {
            assertEquals("Delete income", awaitState { !it.isLoading }.deleteLabel)
            vm.onDelete()
            val done = awaitState { it.isDone || it.notFound }
            assertTrue(done.isDone)
            assertEquals("Income", done.kindLabel)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(transactions.get(entry.id))
    }

    @Test
    fun reportsAMissingEntry() = test {
        val vm = viewModel()
        vm.load("nope")
        vm.uiState.test {
            assertTrue(awaitState { !it.isLoading }.notFound)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
