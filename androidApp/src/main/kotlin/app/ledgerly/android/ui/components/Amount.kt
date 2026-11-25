package app.ledgerly.android.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.money.Sign

/**
 * A money figure: the whole number in `ink`, the currency code and cents in `ink-faint`.
 * The design system allows the faint parts only at 24sp and up, so smaller styles keep everything in `ink`.
 */
@Composable
fun Amount(
    money: Money,
    style: TextStyle,
    modifier: Modifier = Modifier,
    showFraction: Boolean = true,
) {
    val colors = LedgerlyTheme.colors
    val parts = money.parts()
    val faint = if (style.fontSize.value >= 24f) colors.inkFaint else colors.ink
    val text = buildAnnotatedString {
        if (parts.sign == Sign.Negative) withStyle(SpanStyle(color = colors.ink)) { append("− ") }
        withStyle(SpanStyle(color = faint)) { append("${parts.currencyCode} ") }
        withStyle(SpanStyle(color = colors.ink)) { append(parts.whole) }
        if (showFraction && parts.fraction.isNotEmpty()) {
            withStyle(SpanStyle(color = faint)) { append(".${parts.fraction}") }
        }
    }
    Text(
        text = text,
        style = style,
        modifier = modifier.semantics { contentDescription = parts.format(withFraction = showFraction) },
    )
}
