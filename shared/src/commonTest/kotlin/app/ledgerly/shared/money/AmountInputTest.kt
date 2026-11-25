package app.ledgerly.shared.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmountInputTest {
    @Test
    fun cleansWhatWasTyped() {
        assertEquals("1250.55", cleanAmountInput("1,250.559", CurrencyCode.KES))
        assertEquals("0.5", cleanAmountInput(".5", CurrencyCode.KES))
        assertEquals("12.5", cleanAmountInput("12..5", CurrencyCode.KES))
        assertEquals("7", cleanAmountInput("07", CurrencyCode.KES))
        assertEquals("2000", cleanAmountInput("KES 2 000", CurrencyCode.KES))
        assertEquals("5000", cleanAmountInput("5000.5", CurrencyCode.UGX))
        assertEquals("9999999999", cleanAmountInput("99999999999", CurrencyCode.KES))
        assertEquals("", cleanAmountInput("abc", CurrencyCode.KES))
    }

    @Test
    fun parsesAmounts() {
        assertEquals(Money(125_050, CurrencyCode.KES), parseAmount("1,250.5", CurrencyCode.KES))
        assertEquals(Money(100, CurrencyCode.KES), parseAmount("1.", CurrencyCode.KES))
        assertEquals(Money.zero(CurrencyCode.KES), parseAmount("0", CurrencyCode.KES))
        assertNull(parseAmount("", CurrencyCode.KES))
    }

    @Test
    fun turnsMoneyBackIntoFieldText() {
        assertEquals("1250.5", Money(125_050, CurrencyCode.KES).toInputText())
        assertEquals("2000", Money.ofMajor(2_000, CurrencyCode.KES).toInputText())
        assertEquals("0.05", Money(5, CurrencyCode.KES).toInputText())
        assertEquals("5000", Money.ofMajor(5_000, CurrencyCode.UGX).toInputText())
    }
}
