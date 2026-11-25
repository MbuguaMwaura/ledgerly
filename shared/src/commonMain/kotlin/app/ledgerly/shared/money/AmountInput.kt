package app.ledgerly.shared.money

/** Largest whole amount a typed field accepts: 9,999,999,999 in major units. */
private const val MAX_WHOLE_DIGITS = 10

/**
 * Cleans what the user typed into an amount field: drops grouping commas, spaces and any other
 * character, keeps one decimal point and at most [CurrencyCode.decimals] digits after it.
 * "1,250.555" → "1250.55".
 */
fun cleanAmountInput(text: String, currency: CurrencyCode): String {
    val whole = StringBuilder()
    val fraction = StringBuilder()
    var seenDot = false
    for (c in text) {
        when {
            c == '.' -> seenDot = true
            !c.isDigit() -> Unit
            !seenDot -> if (whole.length < MAX_WHOLE_DIGITS) {
                if (whole.toString() == "0") whole.clear()
                whole.append(c)
            }
            fraction.length < currency.decimals -> fraction.append(c)
        }
    }
    if (!seenDot || currency.decimals == 0) return whole.toString()
    return "${whole.ifEmpty { "0" }}.$fraction"
}

/**
 * Reads an amount the user typed, e.g. "1,250.5" → KES 1,250.50. Returns null when there is no
 * number in it. Grouping commas and spaces are ignored.
 */
fun parseAmount(text: String, currency: CurrencyCode): Money? {
    val clean = cleanAmountInput(text, currency)
    if (clean.isEmpty()) return null
    val whole = clean.substringBefore('.').ifEmpty { "0" }.toLong()
    val fraction = clean.substringAfter('.', "").padEnd(currency.decimals, '0').take(currency.decimals)
    return Money(whole * currency.minorUnitFactor + fraction.ifEmpty { "0" }.toLong(), currency)
}

/** The plain number for an editable field: KES 1,250.50 → "1250.5", KES 2,000 → "2000". */
fun Money.toInputText(): String {
    val factor = currency.minorUnitFactor
    val whole = minorUnits / factor
    val fraction = (minorUnits % factor).toString().padStart(currency.decimals, '0').trimEnd('0')
    return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
}
