package app.ledgerly.shared.data

import app.cash.turbine.test
import app.ledgerly.shared.db.LedgerlyDatabase
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.rules.BudgetTone
import app.ledgerly.shared.domain.rules.DueStatus
import app.ledgerly.shared.domain.usecase.AddDebt
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.domain.usecase.LogNothingSpent
import app.ledgerly.shared.domain.usecase.ObserveHomeSummary
import app.ledgerly.shared.domain.usecase.RecordDebtPayment
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.domain.model.UserSettings
import app.ledgerly.shared.money.CurrencyCode.KES
import app.ledgerly.shared.money.CurrencyCode.UGX
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.testing.FakeClock
import app.ledgerly.shared.testing.createTestDriver
import app.ledgerly.shared.testing.nairobi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepositoriesTest {
    private val dispatcher = StandardTestDispatcher()
    private val db = LedgerlyDatabase(createTestDriver())
    private val clock = FakeClock(nairobi("2026-11-25T18:00"))

    private val categories = LocalCategoryRepository(db, dispatcher)
    private val transactions = LocalTransactionRepository(db, dispatcher)
    private val logs = LocalLogRepository(db, dispatcher)
    private val budgets = LocalBudgetRepository(db, dispatcher)
    private val debts = LocalDebtRepository(db, dispatcher)
    private val settings = LocalSettingsRepository(db, dispatcher)

    private fun kes(major: Long) = Money.ofMajor(major, KES)

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun seedsDefaultCategories() = test {
        val all = categories.observeAll().first()
        assertTrue(all.any { it.id == "food" && it.kind == EntryKind.Expense })
        assertTrue(all.any { it.id == "salary" && it.kind == EntryKind.Income })
    }

    @Test
    fun savesEntriesUnderTheirLogicalDay() = test {
        val logEntry = LogEntry(transactions, clock)
        val late = logEntry(EntryKind.Expense, kes(650), "food", occurredAt = nairobi("2026-11-25T01:30"))
        assertEquals(LocalDate(2026, 11, 24), late.logicalDate)

        val day = transactions.observeBetween(LocalDate(2026, 11, 24), LocalDate(2026, 11, 24)).first()
        assertEquals(listOf(late), day)
        assertEquals(late, transactions.get(late.id))

        transactions.delete(late.id)
        assertEquals(null, transactions.get(late.id))
    }

    @Test
    fun loggedDaysIncludeNothingSpentDays() = test {
        LogEntry(transactions, clock)(EntryKind.Expense, kes(100), "food", occurredAt = nairobi("2026-11-24T12:00"))
        LogNothingSpent(logs, clock)()
        assertEquals(setOf(LocalDate(2026, 11, 24), LocalDate(2026, 11, 25)), logs.observeLoggedDays().first())
    }

    @Test
    fun debtsCarryTheirPayments() = test {
        val debt = AddDebt(debts, clock)(DebtDirection.OwedToMe, " Wanjiru ", kes(2_000), LocalDate(2026, 11, 20))
        RecordDebtPayment(debts, clock)(debt.id, kes(500))

        val saved = debts.observeAll().first().single()
        assertEquals("Wanjiru", saved.counterparty)
        assertEquals(kes(1_500), saved.remaining)

        debts.markReminded(debt.id, clock.now())
        assertEquals(clock.now(), debts.observeAll().first().single().lastRemindedAt)

        debts.delete(debt.id)
        assertTrue(debts.observeAll().first().isEmpty())
    }

    @Test
    fun homeSummaryCombinesTheMonth() = test {
        val month = BudgetMonth(2026, 11)
        budgets.save(Budget("food", month, kes(10_000)), clock.now())
        budgets.save(Budget("transport", month, kes(5_000)), clock.now())

        val logEntry = LogEntry(transactions, clock)
        logEntry(EntryKind.Expense, kes(8_500), "food", occurredAt = nairobi("2026-11-24T12:00"))
        logEntry(EntryKind.Expense, kes(1_000), "transport", occurredAt = nairobi("2026-11-25T08:00"))
        logEntry(EntryKind.Income, kes(40_000), "salary", occurredAt = nairobi("2026-11-25T09:00"))
        logEntry(EntryKind.Expense, kes(9_999), "food", occurredAt = nairobi("2026-10-31T12:00")) // last month
        AddDebt(debts, clock)(DebtDirection.OwedToMe, "Otieno", kes(2_000), LocalDate(2026, 11, 27))
        AddDebt(debts, clock)(DebtDirection.OwedToMe, "Akinyi", kes(3_000), LocalDate(2026, 12, 20))

        ObserveHomeSummary(transactions, budgets, logs, debts, settings, clock)().test {
            val summary = awaitItem()
            assertEquals(kes(15_000), summary.leftToSpend.totalLimit)
            assertEquals(kes(9_500), summary.leftToSpend.spent)
            assertEquals(kes(5_500), summary.leftToSpend.left)
            assertEquals(6, summary.leftToSpend.daysLeft)
            assertEquals(kes(40_000), summary.income)
            assertEquals(listOf("food"), summary.needsALook.map { it.budget.categoryId })
            assertEquals(BudgetTone.Caution, summary.needsALook.single().tone)
            assertEquals(2, summary.streak.current)
            assertEquals(listOf("Otieno" to DueStatus.DueSoon(2)), summary.debtsDueSoon.map { (d, s) -> d.counterparty to s })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetsCarryOverUntilChanged() = test {
        val october = BudgetMonth(2026, 10)
        val november = BudgetMonth(2026, 11)
        val december = BudgetMonth(2026, 12)
        budgets.save(Budget("food", october, kes(8_000)), clock.now())
        budgets.save(Budget("rent", october, kes(20_000)), clock.now())
        budgets.save(Budget("food", november, kes(10_000)), clock.now())
        budgets.save(Budget("rent", december, kes(0)), clock.now())

        assertEquals(
            setOf(Budget("food", november, kes(10_000)), Budget("rent", november, kes(20_000))),
            budgets.observeMonth(november).first().toSet(),
        )
        assertEquals(listOf(Budget("food", december, kes(10_000))), budgets.observeMonth(december).first())
        assertEquals(
            setOf(Budget("food", october, kes(8_000)), Budget("rent", october, kes(20_000))),
            budgets.observeMonth(october).first().toSet(),
        )
        assertTrue(budgets.observeMonth(BudgetMonth(2026, 9)).first().isEmpty())
    }

    @Test
    fun settingsStartAtDefaultsAndSaveChanges() = test {
        val fresh = settings.observe().first()
        assertEquals(UserSettings(), fresh)
        assertTrue(!fresh.isOnboarded)

        settings.update { it.copy(name = "Wanjiru Kamau", currency = UGX, theme = ThemeChoice.Dark, trackDebts = false, startedOn = LocalDate(2026, 11, 25)) }
        val saved = settings.observe().first()
        assertEquals("Wanjiru Kamau", saved.name)
        assertEquals(UGX, saved.currency)
        assertEquals(ThemeChoice.Dark, saved.theme)
        assertEquals(false, saved.trackDebts)
        assertTrue(saved.trackBudgets)
        assertTrue(saved.isOnboarded)
    }
}
