package app.ledgerly.shared.domain

import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.rules.BudgetTone
import app.ledgerly.shared.domain.rules.budgetStatus
import app.ledgerly.shared.domain.rules.budgetsNeedingALook
import app.ledgerly.shared.domain.rules.leftToSpend
import app.ledgerly.shared.money.CurrencyCode.KES
import app.ledgerly.shared.money.Money
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetRulesTest {
    private val month = BudgetMonth(2026, 11)

    private fun kes(major: Long) = Money.ofMajor(major, KES)

    private fun budget(limit: Long, category: String = "food") = Budget(category, month, kes(limit))

    @Test
    fun tonesFollowTheThresholds() {
        assertEquals(BudgetTone.OnTrack, budgetStatus(budget(1_000), kes(799)).tone)
        assertEquals(BudgetTone.Caution, budgetStatus(budget(1_000), kes(800)).tone)
        assertEquals(BudgetTone.Caution, budgetStatus(budget(1_000), kes(999)).tone)
        assertEquals(BudgetTone.Over, budgetStatus(budget(1_000), kes(1_000)).tone)
    }

    @Test
    fun overBudgetWritesOutTheOverAmount() {
        val status = budgetStatus(budget(1_000), kes(1_250))
        assertEquals(125, status.percentUsed)
        assertEquals(kes(250), status.overBy)
        assertEquals(kes(0), status.left)
    }

    @Test
    fun zeroLimit() {
        assertEquals(BudgetTone.OnTrack, budgetStatus(budget(0), kes(0)).tone)
        assertEquals(BudgetTone.Over, budgetStatus(budget(0), kes(1)).tone)
    }

    @Test
    fun leftToSpendPerDayCountsToday() {
        val result = leftToSpend(kes(30_000), kes(11_550), month, today = LocalDate(2026, 11, 25))
        assertEquals(kes(18_450), result.left)
        assertEquals(6, result.daysLeft)
        assertEquals(kes(3_075), result.perDay)
    }

    @Test
    fun overspentMonthHasNothingPerDay() {
        val result = leftToSpend(kes(10_000), kes(12_000), month, today = LocalDate(2026, 11, 25))
        assertEquals(kes(-2_000), result.left)
        assertEquals(kes(0), result.perDay)
    }

    @Test
    fun onlyBudgetsAtEightyPercentOrMoreNeedALook() {
        val statuses = listOf(
            budgetStatus(budget(1_000, "food"), kes(500)),
            budgetStatus(budget(1_000, "transport"), kes(850)),
            budgetStatus(budget(1_000, "fun"), kes(1_100)),
        )
        assertEquals(listOf("fun", "transport"), budgetsNeedingALook(statuses).map { it.budget.categoryId })
    }

    @Test
    fun monthLengths() {
        assertEquals(29, BudgetMonth(2028, 2).lengthInDays)
        assertEquals(28, BudgetMonth(2100, 2).lengthInDays)
        assertEquals(30, BudgetMonth(2026, 11).lengthInDays)
    }
}
