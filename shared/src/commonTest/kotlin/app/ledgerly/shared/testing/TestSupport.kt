package app.ledgerly.shared.testing

import app.cash.sqldelight.db.SqlDriver
import app.ledgerly.shared.domain.time.AppClock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** An in-memory database driver with the schema created. */
expect fun createTestDriver(): SqlDriver

val NAIROBI = TimeZone.of("Africa/Nairobi")

fun nairobi(text: String): Instant = LocalDateTime.parse(text).toInstant(NAIROBI)

class FakeClock(var now: Instant, private val zone: TimeZone = NAIROBI) : AppClock {
    override fun now(): Instant = now

    override fun timeZone(): TimeZone = zone
}
