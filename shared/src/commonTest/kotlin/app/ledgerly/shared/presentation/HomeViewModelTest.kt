package app.ledgerly.shared.presentation

import app.cash.turbine.test
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.rules.BudgetTone
import app.ledgerly.shared.domain.usecase.AddDebt
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.domain.usecase.LogNothingSpent
import app.ledgerly.shared.domain.usecase.ObserveHomeSummary
import app.ledgerly.shared.presentation.home.HomeViewModel
import app.ledgerly.shared.presentation.home.StreakMark
import app.ledgerly.shared.testing.nairobi
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeViewModelTest : ViewModelTest() {
    private fun viewModel() = HomeViewModel(
        ObserveHomeSummary(transactions, budgets, logs, debts, clock),
        categories,
        LogNothingSpent(logs, clock),
        clock,
    )

    @Test
    fun startsLoadingThenShowsAnEmptyMonth() = test {
        val vm = viewModel()
        assertTrue(vm.uiState.value.isLoading)
        vm.uiState.test {
            val state = awaitState { !it.isLoading }
            assertEquals(kes(0), state.leftToSpend)
            assertEquals(0, state.streak.current)
            assertTrue(state.needsALook.isEmpty())
        }
    }

    @Test
    fun showsWhatIsLeftAndWhatNeedsALook() = test {
        val month = BudgetMonth(2026, 11)
        budgets.save(Budget("food", month, kes(10_000)), clock.now())
        budgets.save(Budget("transport", month, kes(5_000)), clock.now())
        val log = LogEntry(transactions, clock)
        log(EntryKind.Expense, kes(8_500), "food", occurredAt = nairobi("2026-11-20T12:00"))
        log(EntryKind.Expense, kes(1_000), "transport")
        log(EntryKind.Income, kes(60_000), "salary", occurredAt = nairobi("2026-11-01T09:00"))

        viewModel().uiState.test {
            val state = awaitState { !it.isLoading }
            assertEquals(kes(5_500), state.leftToSpend)
            assertEquals(kes(9_500), state.spent)
            assertEquals(kes(60_000), state.income)
            assertEquals(kes(15_000), state.totalBudget)
            assertEquals(63, state.budgetPercentUsed)
            // 25th to 30th November, today included.
            assertEquals(6, state.daysLeft)
            val food = state.needsALook.single()
            assertEquals("Food", food.categoryName)
            assertEquals(BudgetTone.Caution, food.tone)
            assertEquals(1, state.streak.current)
        }
    }

    @Test
    fun listsMoneyOwedThatIsDueSoon() = test {
        val add = AddDebt(debts, clock)
        add(DebtDirection.OwedToMe, "Wanjiru Kamau", kes(2_000), LocalDate(2026, 11, 27))
        add(DebtDirection.OwedToMe, "Otieno", kes(500), LocalDate(2026, 12, 20))
        add(DebtDirection.IOwe, "Sacco", kes(9_000), LocalDate(2026, 11, 26))

        viewModel().uiState.test {
            val state = awaitState { !it.isLoading }
            assertEquals(listOf("Wanjiru Kamau"), state.debtsDueSoon.map { it.counterparty })
        }
    }

    @Test
    fun nothingSpentTodayStartsTheStreak() = test {
        val vm = viewModel()
        vm.uiState.test {
            assertFalse(awaitState { !it.isLoading }.streak.loggedToday)
            vm.onNothingSpentToday()
            val state = awaitState { it.streak.loggedToday }
            assertEquals(1, state.streak.current)
        }
    }

    @Test
    fun weekRowMarksLoggedMissedOpenAndFutureDays() = test {
        val nothing = LogNothingSpent(logs, clock)
        clock.now = nairobi("2026-11-23T20:00") // Monday
        nothing()
        clock.now = nairobi("2026-11-25T18:00") // Wednesday, not logged yet

        viewModel().uiState.test {
            val state = awaitState { !it.isLoading }
            assertEquals(
                listOf(StreakMark.Logged, StreakMark.Missed, StreakMark.Open, StreakMark.Future, StreakMark.Future, StreakMark.Future, StreakMark.Future),
                state.week.map { it.mark },
            )
            assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), state.week.map { it.letter })
            assertTrue(state.week[2].isToday)
            assertEquals(3, state.nextMilestone)
        }
    }

    @Test
    fun listsTheLatestEntriesUnderRecent() = test {
        val log = LogEntry(transactions, clock)
        log(EntryKind.Expense, kes(100), "transport", occurredAt = nairobi("2026-11-20T08:00"))
        log(EntryKind.Expense, kes(200), "food", note = "Java House", occurredAt = nairobi("2026-11-24T13:00"))
        log(EntryKind.Income, kes(4_000), "salary", occurredAt = nairobi("2026-11-25T09:00"))
        log(EntryKind.Expense, kes(300), "food", occurredAt = nairobi("2026-11-25T12:30"))

        viewModel().uiState.test {
            val state = awaitState { it.recent.size == 3 }
            assertEquals(listOf("Food", "Salary", "Java House"), state.recent.map { it.title })
            assertEquals("12:30", state.recent.first().time)
            assertEquals(kes(4_000), state.recent[1].signedAmount)
        }
    }
}
