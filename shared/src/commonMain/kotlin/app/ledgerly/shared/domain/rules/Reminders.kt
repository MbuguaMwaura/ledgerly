package app.ledgerly.shared.domain.rules

import app.ledgerly.shared.domain.time.logicalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** How many daily reminders each app keeps scheduled, so they keep coming if the app isn't opened. */
const val REMINDER_DAYS_AHEAD = 7

/** The choices offered for the reminder time, in minutes after midnight. */
val REMINDER_TIME_CHOICES = listOf(8 * 60, 13 * 60, 19 * 60, 20 * 60, 21 * 60)

/**
 * When the next daily reminders should fire: one a day at [minuteOfDay], from now on. Today's is
 * left out once something is logged today, so the nudge only comes on days that need it. Each app
 * schedules these as one-off notifications and recomputes them whenever the app opens or a log
 * changes.
 */
fun upcomingReminders(
    minuteOfDay: Int,
    now: Instant,
    timeZone: TimeZone,
    loggedToday: Boolean,
    count: Int = REMINDER_DAYS_AHEAD,
): List<Instant> {
    val time = LocalTime(minuteOfDay / 60, minuteOfDay % 60)
    val today = logicalDate(now, timeZone)
    val start = now.toLocalDateTime(timeZone).date
    return generateSequence(start) { it.plus(DatePeriod(days = 1)) }
        .map { LocalDateTime(it, time).toInstant(timeZone) }
        .filter { it > now }
        .filterNot { loggedToday && logicalDate(it, timeZone) == today }
        .take(count)
        .toList()
}

/** One reminder time to pick, e.g. 20:00. */
data class ReminderOption(val minutes: Int, val label: String)

val REMINDER_OPTIONS: List<ReminderOption> = REMINDER_TIME_CHOICES.map { ReminderOption(it, reminderTimeLabel(it)) }

/** "20:00". */
fun reminderTimeLabel(minuteOfDay: Int): String =
    "${(minuteOfDay / 60).toString().padStart(2, '0')}:${(minuteOfDay % 60).toString().padStart(2, '0')}"

/** The notification's text. */
const val REMINDER_MESSAGE = "Nothing logged today. Two taps keeps your streak going."

