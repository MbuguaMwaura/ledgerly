package app.ledgerly.shared.domain

import app.ledgerly.shared.domain.rules.reminderTimeLabel
import app.ledgerly.shared.domain.rules.upcomingReminders
import app.ledgerly.shared.testing.NAIROBI
import app.ledgerly.shared.testing.nairobi
import kotlin.test.Test
import kotlin.test.assertEquals

class RemindersTest {
    @Test
    fun aWeekOfDailyRemindersFromToday() {
        val times = upcomingReminders(20 * 60, nairobi("2026-11-25T18:00"), NAIROBI, loggedToday = false)
        assertEquals(7, times.size)
        assertEquals(nairobi("2026-11-25T20:00"), times.first())
        assertEquals(nairobi("2026-12-01T20:00"), times.last())
    }

    @Test
    fun todaysIsSkippedOnceLogged() {
        val times = upcomingReminders(20 * 60, nairobi("2026-11-25T18:00"), NAIROBI, loggedToday = true, count = 2)
        assertEquals(listOf(nairobi("2026-11-26T20:00"), nairobi("2026-11-27T20:00")), times)
    }

    @Test
    fun aTimeAlreadyPassedStartsTomorrow() {
        val times = upcomingReminders(8 * 60, nairobi("2026-11-25T09:00"), NAIROBI, loggedToday = false, count = 1)
        assertEquals(listOf(nairobi("2026-11-26T08:00")), times)
    }

    @Test
    fun afterMidnightTheLoggedDayIsStillYesterday() {
        // 01:30 belongs to the 24th, which is logged, so a 02:00 reminder that night is skipped.
        val times = upcomingReminders(2 * 60, nairobi("2026-11-25T01:30"), NAIROBI, loggedToday = true, count = 1)
        assertEquals(listOf(nairobi("2026-11-26T02:00")), times)
    }

    @Test
    fun labels() {
        assertEquals("08:00", reminderTimeLabel(8 * 60))
        assertEquals("19:30", reminderTimeLabel(19 * 60 + 30))
    }
}
