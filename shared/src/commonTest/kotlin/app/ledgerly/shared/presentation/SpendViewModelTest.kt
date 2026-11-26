package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.presentation.spend.SpendViewModel
import app.ledgerly.shared.testing.nairobi
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpendViewModelTest : ViewModelTest() {
    private fun viewModel() = SpendViewModel(transactions, budgets, categories, settings, clock)

    private val today = LocalDate(2026, 11, 25)
    private val yesterday = LocalDate(2026, 11, 24)

    @Test
    fun drawsOneBarPerDayOfTheMonth() = test {
        LogEntry(transactions, clock)(EntryKind.Expense, kes(650), "food")
        viewModel().uiState.test {
            val state = awaitState { !it.isLoading && it.spentThisMonth == kes(650) }
            assertEquals(30, state.bars.size)
            val bar = state.bars.single { it.isToday }
            assertEquals(today, bar.date)
            assertEquals(kes(650), bar.spent)
            assertTrue(state.bars.filter { it.date > today }.all { it.isFuture })
        }
    }

    @Test
    fun groupsTheLogByDayNewestFirst() = test {
        val log = LogEntry(transactions, clock)
        log(EntryKind.Expense, kes(300), "transport", occurredAt = nairobi("2026-11-24T08:00"))
        log(EntryKind.Expense, kes(650), "food", note = "Lunch at Java")
        log(EntryKind.Income, kes(1_000), "salary")

        viewModel().uiState.test {
            val state = awaitState { !it.isLoading && it.days.size == 2 }
            assertEquals(listOf("Today", "Yesterday"), state.days.map { it.label })
            // Income shows in the log but doesn't count as spending.
            assertEquals(kes(650), state.days.first().spent)
            assertEquals(kes(950), state.spentThisMonth)
            val lunch = state.days.first().entries.single { it.kind == EntryKind.Expense }
            assertEquals("Lunch at Java", lunch.title)
            assertEquals(kes(-650), lunch.signedAmount)
        }
    }

    @Test
    fun tappingABarFiltersToThatDayAndTappingAgainClearsIt() = test {
        val log = LogEntry(transactions, clock)
        log(EntryKind.Expense, kes(300), "transport", occurredAt = nairobi("2026-11-24T08:00"))
        log(EntryKind.Expense, kes(650), "food")

        val vm = viewModel()
        vm.uiState.test {
            awaitState { !it.isLoading && it.days.size == 2 }
            vm.onSelectDay(yesterday)
            val filtered = awaitState { it.selectedDay == yesterday }
            assertEquals(listOf(yesterday), filtered.days.map { it.date })
            assertTrue(filtered.bars.single { it.date == yesterday }.isSelected)

            vm.onSelectDay(yesterday)
            val cleared = awaitState { it.selectedDay == null }
            assertNull(cleared.selectedDay)
            assertEquals(2, cleared.days.size)
        }
    }

    @Test
    fun showsBudgetsMostUsedFirst() = test {
        val month = BudgetMonth(2026, 11)
        budgets.save(Budget("food", month, kes(10_000)), clock.now())
        budgets.save(Budget("transport", month, kes(1_000)), clock.now())
        LogEntry(transactions, clock)(EntryKind.Expense, kes(900), "transport")

        viewModel().uiState.test {
            val state = awaitState { !it.isLoading && it.budgets.size == 2 && it.spentThisMonth == kes(900) }
            assertEquals(listOf("transport", "food"), state.budgets.map { it.categoryId })
            assertEquals(90, state.budgets.first().percentUsed)
        }
    }

    @Test
    fun deletingAnEntryRemovesIt() = test {
        val entry = LogEntry(transactions, clock)(EntryKind.Expense, kes(650), "food")
        val vm = viewModel()
        vm.uiState.test {
            awaitState { it.days.isNotEmpty() }
            vm.onDeleteEntry(entry.id)
            val state = awaitState { it.days.isEmpty() }
            assertEquals(kes(0), state.spentThisMonth)
        }
    }
}
