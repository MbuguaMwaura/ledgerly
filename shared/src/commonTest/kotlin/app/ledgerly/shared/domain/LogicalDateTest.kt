package app.ledgerly.shared.domain

import app.ledgerly.shared.domain.time.logicalDate
import app.ledgerly.shared.testing.NAIROBI
import app.ledgerly.shared.testing.nairobi
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LogicalDateTest {
    @Test
    fun lateNightCountsForTheDayBefore() {
        assertEquals(LocalDate(2026, 11, 24), logicalDate(nairobi("2026-11-25T01:30"), NAIROBI))
        assertEquals(LocalDate(2026, 11, 24), logicalDate(nairobi("2026-11-25T02:59"), NAIROBI))
    }

    @Test
    fun fromThreeItIsTheNewDay() {
        assertEquals(LocalDate(2026, 11, 25), logicalDate(nairobi("2026-11-25T03:00"), NAIROBI))
        assertEquals(LocalDate(2026, 11, 25), logicalDate(nairobi("2026-11-25T23:59"), NAIROBI))
    }
}
