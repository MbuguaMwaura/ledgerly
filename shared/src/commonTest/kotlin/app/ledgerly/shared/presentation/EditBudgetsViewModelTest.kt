package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.rules.BudgetTone
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.presentation.budgets.EditBudgetsViewModel
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditBudgetsViewModelTest : ViewModelTest() {
    private val november = BudgetMonth(2026, 11)

    private fun viewModel() = EditBudgetsViewModel(categories, budgets, transactions, settings, clock)

    @Test
    fun listsEveryExpenseCategoryWithWhatWasSpent() = test {
        budgets.save(Budget("food", november, kes(10_000)), clock.now())
        LogEntry(transactions, clock)(EntryKind.Expense, kes(8_500), "food")
        val vm = viewModel()
        vm.uiState.test {
            val state = awaitState { !it.isLoading }
            assertTrue(state.rows.none { it.categoryId == "salary" })
            val food = state.rows.first { it.categoryId == "food" }
            assertEquals("10000", food.limitText)
            assertEquals("Spent KES 8,500 · KES 1,500 left", food.note)
            assertEquals(BudgetTone.Caution, food.tone)
            assertEquals("Spent KES 0 · no budget", state.rows.first { it.categoryId == "rent" }.note)
            assertEquals("KES 10,000 a month in 1 category.", state.totalText)
            assertFalse(state.canSave)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun stepsByFiveHundredAndSavesOnlyChanges() = test {
        budgets.save(Budget("food", november, kes(10_000)), clock.now())
        budgets.save(Budget("rent", november, kes(20_000)), clock.now())
        val vm = viewModel()
        vm.uiState.test {
            awaitState { !it.isLoading }
            vm.onStep("transport", up = true)
            vm.onStep("transport", up = true)
            assertEquals("1000", awaitState { s -> s.rows.first { it.categoryId == "transport" }.limitText == "1000" }
                .rows.first { it.categoryId == "transport" }.limitText)
            vm.onStep("food", up = false)
            vm.onLimitChange("rent", "")
            vm.onLimitChange("fun", "3,000.75")
            val edited = awaitState { s -> s.rows.first { it.categoryId == "fun" }.limitText == "3000" }
            assertEquals(kes(13_500), edited.total)
            assertTrue(edited.canSave)

            vm.onSave()
            awaitState { it.isSaved }
            cancelAndIgnoreRemainingEvents()
        }
        val saved = budgets.observeMonth(november).first().associate { it.categoryId to it.limit }
        assertEquals(mapOf("food" to kes(9_500), "transport" to kes(1_000), "fun" to kes(3_000)), saved)
    }

    @Test
    fun undoingAnEditLeavesNothingToSave() = test {
        budgets.save(Budget("food", november, kes(10_000)), clock.now())
        val vm = viewModel()
        vm.uiState.test {
            awaitState { !it.isLoading }
            vm.onStep("food", up = true)
            assertTrue(awaitState { it.hasChanges }.canSave)
            vm.onStep("food", up = false)
            assertFalse(awaitState { !it.hasChanges }.canSave)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
