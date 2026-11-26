package app.ledgerly.shared.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun splitsIntoStyledParts() {
        val parts = Money(1_845_000, CurrencyCode.KES).parts()
        assertEquals(MoneyParts(Sign.Positive, "KES", "18,450", "00"), parts)
        assertEquals("KES 18,450.00", parts.format(withFraction = true))
        assertEquals("KES 18,450", parts.format(withFraction = false))
    }

    @Test
    fun groupsThousands() {
        assertEquals("0", Money.zero(CurrencyCode.KES).parts().whole)
        assertEquals("999", Money.ofMajor(999, CurrencyCode.KES).parts().whole)
        assertEquals("1,000", Money.ofMajor(1_000, CurrencyCode.KES).parts().whole)
        assertEquals("1,234,567", Money.ofMajor(1_234_567, CurrencyCode.KES).parts().whole)
    }

    @Test
    fun keepsTheSignOutOfTheDigits() {
        val parts = Money(-65_005, CurrencyCode.KES).parts()
        assertEquals(Sign.Negative, parts.sign)
        assertEquals("650", parts.whole)
        assertEquals("05", parts.fraction)
    }

    @Test
    fun currenciesWithoutMinorUnitsHaveNoFraction() {
        assertEquals("", Money.ofMajor(5_000, CurrencyCode.UGX).parts().fraction)
    }

    @Test
    fun arithmeticStaysInMinorUnits() {
        val total = Money(1_000, CurrencyCode.KES) + Money(2_050, CurrencyCode.KES) - Money(50, CurrencyCode.KES)
        assertEquals(Money(3_000, CurrencyCode.KES), total)
        assertTrue((-total).isNegative)
    }

    @Test
    fun refusesToMixCurrencies() {
        assertFailsWith<IllegalArgumentException> { Money(1, CurrencyCode.KES) + Money(1, CurrencyCode.USD) }
    }

    @Test
    fun hiddenKeepsSignAndCurrency() {
        val parts = Money.ofMajor(-1_250, CurrencyCode.KES).parts().hidden()
        assertEquals("KES ••••", parts.format(withFraction = true))
        assertEquals(Sign.Negative, parts.sign)
    }
}
