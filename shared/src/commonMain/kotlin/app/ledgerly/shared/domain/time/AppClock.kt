package app.ledgerly.shared.domain.time

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** The current time and zone. Injected so rules and tests never read the system clock directly. */
interface AppClock {
    fun now(): Instant

    fun timeZone(): TimeZone

    /** Today as the user experiences it, after the 03:00 cutoff. */
    fun today(): LocalDate = logicalDate(now(), timeZone())
}

object SystemAppClock : AppClock {
    override fun now(): Instant = Clock.System.now()

    override fun timeZone(): TimeZone = TimeZone.currentSystemDefault()
}

/** Late-night logs count for the day before until this hour, so a 01:30 entry belongs to yesterday. */
const val DAY_CUTOFF_HOUR = 3

fun logicalDate(instant: Instant, timeZone: TimeZone): LocalDate {
    val local = instant.toLocalDateTime(timeZone)
    return if (local.hour < DAY_CUTOFF_HOUR) local.date.minus(1, kotlinx.datetime.DateTimeUnit.DAY) else local.date
}
