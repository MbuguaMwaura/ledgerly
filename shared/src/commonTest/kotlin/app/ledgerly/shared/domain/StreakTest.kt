package app.ledgerly.shared.domain

import app.ledgerly.shared.domain.rules.calculateStreak
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreakTest {
    private val start = LocalDate(2026, 11, 1)

    private fun day(n: Int) = start.plus(n - 1, DateTimeUnit.DAY)

    private fun days(vararg n: Int) = n.map(::day).toSet()

    private fun range(from: Int, to: Int) = (from..to).map(::day).toSet()

    @Test
    fun noLogsMeansNoStreak() {
        val streak = calculateStreak(emptySet(), day(5))
        assertEquals(0, streak.current)
        assertFalse(streak.loggedToday)
    }

    @Test
    fun countsConsecutiveDaysIncludingToday() {
        val streak = calculateStreak(range(1, 5), today = day(5))
        assertEquals(5, streak.current)
        assertTrue(streak.loggedToday)
    }

    @Test
    fun todayNotLoggedYetDoesNotBreakIt() {
        val streak = calculateStreak(range(1, 4), today = day(5))
        assertEquals(4, streak.current)
        assertFalse(streak.loggedToday)
        assertNull(streak.milestoneToday)
    }

    @Test
    fun aMissedDayWithoutFreezeResets() {
        val streak = calculateStreak(range(1, 3) + days(5, 6), today = day(6))
        assertEquals(2, streak.current)
        assertEquals(3, streak.longest)
    }

    @Test
    fun sevenDaysEarnAFreezeThatSavesAMissedDay() {
        val streak = calculateStreak(range(1, 7) + range(9, 10), today = day(10))
        assertEquals(9, streak.current, "the frozen day keeps the streak but does not add to it")
        assertEquals(0, streak.freezesHeld)
        assertEquals(listOf(day(8)), streak.frozenDays)
    }

    @Test
    fun freezesCapAtTwo() {
        val streak = calculateStreak(range(1, 28), today = day(28))
        assertEquals(2, streak.freezesHeld)
    }

    @Test
    fun twoMissedDaysSpendTwoFreezes() {
        val streak = calculateStreak(range(1, 14) + days(17), today = day(17))
        assertEquals(15, streak.current)
        assertEquals(listOf(day(15), day(16)), streak.frozenDays)
        assertEquals(0, streak.freezesHeld)
    }

    @Test
    fun runningOutOfFreezesResets() {
        val streak = calculateStreak(range(1, 7) + days(10), today = day(10))
        assertEquals(1, streak.current)
        assertTrue(streak.frozenDays.isEmpty())
    }

    @Test
    fun milestoneOnlyOnTheDayItIsReached() {
        assertEquals(7, calculateStreak(range(1, 7), today = day(7)).milestoneToday)
        assertNull(calculateStreak(range(1, 8), today = day(8)).milestoneToday)
        assertEquals(3, calculateStreak(range(1, 3), today = day(3)).milestoneToday)
    }
}
