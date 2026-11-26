package app.ledgerly.shared.domain

import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.BudgetPeriod
import app.ledgerly.shared.domain.rules.leftToSpend
import app.ledgerly.shared.money.CurrencyCode.KES
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.common.ordinal
import app.ledgerly.shared.presentation.common.periodLabel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetPeriodTest {
    @Test
    fun startingOnTheFirstIsTheCalendarMonth() {
        val period = BudgetPeriod.of(LocalDate(2026, 2, 14))
        assertEquals(BudgetMonth(2026, 2), period.month)
        assertEquals(LocalDate(2026, 2, 1), period.firstDay)
        assertEquals(LocalDate(2026, 2, 28), period.lastDay)
        assertEquals(28, period.lengthInDays)
        assertEquals("February", periodLabel(period))
    }

    @Test
    fun aPaydayMonthIsNamedAfterTheMonthItEndsIn() {
        val beforePayday = BudgetPeriod.of(LocalDate(2026, 11, 20), startDay = 25)
        assertEquals(BudgetMonth(2026, 11), beforePayday.month)
        assertEquals(LocalDate(2026, 10, 25), beforePayday.firstDay)
        assertEquals(LocalDate(2026, 11, 24), beforePayday.lastDay)
        assertEquals("25 Oct – 24 Nov", periodLabel(beforePayday))

        val onPayday = BudgetPeriod.of(LocalDate(2026, 11, 25), startDay = 25)
        assertEquals(BudgetMonth(2026, 12), onPayday.month)
        assertEquals(LocalDate(2026, 11, 25), onPayday.firstDay)
        assertEquals(LocalDate(2026, 12, 24), onPayday.lastDay)
        assertEquals(30, onPayday.lengthInDays)
        assertTrue(onPayday.contains(LocalDate(2026, 12, 24)))
        assertFalse(onPayday.contains(LocalDate(2026, 12, 25)))
    }

    @Test
    fun crossesTheYear() {
        val period = BudgetPeriod.of(LocalDate(2026, 12, 28), startDay = 25)
        assertEquals(BudgetMonth(2027, 1), period.month)
        assertEquals(LocalDate(2026, 12, 25), period.firstDay)
        assertEquals(LocalDate(2027, 1, 24), period.lastDay)
        assertEquals(31, period.days.size)
    }

    @Test
    fun daysLeftCountToPayday() {
        val period = BudgetPeriod.of(LocalDate(2026, 11, 20), startDay = 25)
        val left = leftToSpend(Money.ofMajor(10_000, KES), Money.ofMajor(5_000, KES), period, LocalDate(2026, 11, 20))
        assertEquals(5, left.daysLeft)
        assertEquals(Money.ofMajor(1_000, KES), left.perDay)
    }

    @Test
    fun ordinals() {
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "28th"), listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 28).map(::ordinal))
    }
}
