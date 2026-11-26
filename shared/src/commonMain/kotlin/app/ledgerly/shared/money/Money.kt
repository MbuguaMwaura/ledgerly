package app.ledgerly.shared.money

import kotlin.math.roundToLong

/**
 * An amount of money in integer minor units (cents), so sums never drift.
 * `Money(65_000, CurrencyCode.KES)` is KES 650.00.
 */
data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode,
) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(minorUnits + other.minorUnits, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(minorUnits - other.minorUnits, currency)
    }

    operator fun unaryMinus(): Money = Money(-minorUnits, currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    val isNegative: Boolean get() = minorUnits < 0

    /**
     * The pieces each UI styles separately: the design system draws the whole number in `ink`
     * and the currency code and cents in `ink-faint`.
     */
    fun parts(): MoneyParts {
        val absolute = if (minorUnits == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(minorUnits)
        val factor = currency.minorUnitFactor
        val whole = absolute / factor
        val fraction = absolute % factor
        return MoneyParts(
            sign = when {
                minorUnits < 0 -> Sign.Negative
                minorUnits > 0 -> Sign.Positive
                else -> Sign.Zero
            },
            currencyCode = currency.code,
            whole = groupThousands(whole),
            fraction = if (currency.decimals == 0) "" else fraction.toString().padStart(currency.decimals, '0'),
        )
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) { "Cannot combine $currency with ${other.currency}" }
    }

    companion object {
        fun zero(currency: CurrencyCode): Money = Money(0, currency)

        fun ofMajor(major: Long, currency: CurrencyCode): Money = Money(major * currency.minorUnitFactor, currency)
    }
}

enum class CurrencyCode(val code: String, val decimals: Int) {
    KES("KES", 2),
    USD("USD", 2),
    EUR("EUR", 2),
    GBP("GBP", 2),
    UGX("UGX", 0),
    TZS("TZS", 2),
    ;

    val minorUnitFactor: Long get() = pow10(decimals)
}

enum class Sign { Negative, Zero, Positive }

/** `KES 18,450.00` → sign Positive, currencyCode "KES", whole "18,450", fraction "00". */
data class MoneyParts(
    val sign: Sign,
    val currencyCode: String,
    val whole: String,
    val fraction: String,
) {
    /** Plain text for rows and accessibility, e.g. `KES 18,450` or `KES 18,450.00`. */
    fun format(withFraction: Boolean): String = buildString {
        append(currencyCode).append(' ').append(whole)
        if (withFraction && fraction.isNotEmpty()) append('.').append(fraction)
    }
}

/**
 * [minorUnits] of [from] in [to], at [rate] units of [to] per unit of [from], rounded to the
 * nearest minor unit. A rate of 1 keeps the numbers: KES 1,250 becomes UGX 1,250.
 */
fun convertMinorUnits(minorUnits: Long, from: CurrencyCode, to: CurrencyCode, rate: Double): Long {
    require(rate > 0) { "rate must be positive, was $rate" }
    return (minorUnits.toDouble() * rate * to.minorUnitFactor / from.minorUnitFactor).roundToLong()
}

/** What stands in for the digits while amounts are hidden. */
const val HIDDEN_DIGITS = "••••"

/** The same figure with its digits hidden: `KES ••••`. The sign and currency stay. */
fun MoneyParts.hidden(): MoneyParts = copy(whole = HIDDEN_DIGITS, fraction = "")

private fun pow10(exponent: Int): Long {
    var result = 1L
    repeat(exponent) { result *= 10 }
    return result
}

private fun groupThousands(value: Long): String {
    val digits = value.toString()
    val firstGroup = digits.length % 3
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index != 0 && (index - firstGroup) % 3 == 0) append(',')
            append(char)
        }
    }
}
