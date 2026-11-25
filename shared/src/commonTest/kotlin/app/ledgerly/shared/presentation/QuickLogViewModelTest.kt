package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.domain.usecase.LogNothingSpent
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.presentation.quicklog.KeypadKey
import app.ledgerly.shared.presentation.quicklog.QuickLogViewModel
import app.ledgerly.shared.presentation.quicklog.applyKey
import app.ledgerly.shared.presentation.quicklog.formatTyped
import app.ledgerly.shared.presentation.quicklog.parseMinorUnits
import app.ledgerly.shared.testing.nairobi
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickLogViewModelTest : ViewModelTest() {
    private fun viewModel() = QuickLogViewModel(categories, logs, LogEntry(transactions, clock), clock)

    private fun QuickLogViewModel.type(text: String) = text.forEach { c ->
        onKey(
            when (c) {
                '.' -> KeypadKey.Decimal
                '<' -> KeypadKey.Backspace
                else -> KeypadKey.Digit(c.digitToInt())
            },
        )
    }

    @Test
    fun keypadKeepsAtMostTwoDecimalPlaces() {
        fun typed(keys: String) = keys.fold("") { acc, c ->
            applyKey(
                acc,
                when (c) {
                    '.' -> KeypadKey.Decimal
                    '<' -> KeypadKey.Backspace
                    else -> KeypadKey.Digit(c.digitToInt())
                },
                decimals = 2,
            )
        }
        assertEquals("1250.55", typed("1250.559"))
        assertEquals("0.5", typed(".5"))
        assertEquals("12.5", typed("12..5"))
        assertEquals("7", typed("07"))
        assertEquals("12", typed("123<"))
        assertEquals("", typed("1<<"))
        assertEquals("9999999999", typed("99999999999"))
    }

    @Test
    fun parsesAndFormatsWhatWasTyped() {
        assertEquals(125_050L, parseMinorUnits("1250.5", CurrencyCode.KES).minorUnits)
        assertEquals(100L, parseMinorUnits("1.", CurrencyCode.KES).minorUnits)
        assertEquals(0L, parseMinorUnits("", CurrencyCode.KES).minorUnits)
        assertEquals("1,250.5", formatTyped("1250.5"))
        assertEquals("125", formatTyped("125"))
        assertEquals("1,000,000", formatTyped("1000000"))
    }

    @Test
    fun showsCategoriesForTheChosenKind() = test {
        val vm = viewModel()
        vm.uiState.test {
            val expense = awaitState { it.categories.isNotEmpty() }
            assertTrue(expense.categories.all { it.kind == EntryKind.Expense })
            vm.onSelectCategory("food")
            awaitState { it.selectedCategoryId == "food" }

            vm.onKindChange(EntryKind.Income)
            val income = awaitState { it.kind == EntryKind.Income }
            assertTrue(income.categories.all { it.kind == EntryKind.Income })
            assertTrue(income.categories.any { it.id == "salary" })
            assertNull(income.selectedCategoryId)
        }
    }

    @Test
    fun savesOnlyWithAnAmountAndACategory() = test {
        val vm = viewModel()
        vm.uiState.test {
            assertFalse(awaitState { it.categories.isNotEmpty() }.canSave)
            vm.type("650")
            assertFalse(awaitState { it.amountText == "650" }.canSave)
            vm.onSelectCategory("food")
            assertTrue(awaitState { it.selectedCategoryId == "food" }.canSave)
            vm.onNoteChange("Lunch")

            vm.onSave()
            val saved = awaitState { it.savedMessage != null }
            assertEquals("Logged. Your streak starts today.", saved.savedMessage)
            assertFalse(saved.canSave)

            vm.reset()
            val cleared = awaitState { it.savedMessage == null }
            assertEquals("", cleared.amountText)
            assertNull(cleared.selectedCategoryId)
        }
        val entry = transactions.observeBetween(LocalDate(2026, 11, 25), LocalDate(2026, 11, 25)).first().single()
        assertEquals(kes(650), entry.amount)
        assertEquals("Lunch", entry.note)
    }

    @Test
    fun savedMessageCountsTheStreak() = test {
        val nothing = LogNothingSpent(logs, clock)
        clock.now = nairobi("2026-11-23T20:00")
        nothing()
        clock.now = nairobi("2026-11-24T20:00")
        nothing()
        clock.now = nairobi("2026-11-25T18:00")

        val vm = viewModel()
        vm.uiState.test {
            awaitState { it.categories.isNotEmpty() }
            vm.type("100")
            vm.onSelectCategory("transport")
            awaitState { it.canSave }
            vm.onSave()
            assertEquals("Logged. That's 3 days in a row.", awaitState { it.savedMessage != null }.savedMessage)
        }
    }
}
