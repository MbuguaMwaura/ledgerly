package app.ledgerly.android.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ledgerly.android.ui.components.ButtonStyle
import app.ledgerly.android.ui.components.Chip
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyIcon
import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.components.RoundIconButton
import app.ledgerly.android.ui.components.label
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.common.DebtRowState
import androidx.compose.foundation.shape.RoundedCornerShape

/** Parses "1,250.50" into money in [like]'s currency. Null when it isn't a positive amount. */
internal fun parseAmount(text: String, like: Money): Money? {
    val cleaned = text.replace(",", "").trim()
    if (cleaned.isEmpty() || cleaned.count { it == '.' } > 1 || cleaned.any { !it.isDigit() && it != '.' }) return null
    val decimals = like.currency.decimals
    val whole = cleaned.substringBefore('.').ifEmpty { "0" }.toLongOrNull() ?: return null
    val fractionText = cleaned.substringAfter('.', "")
    if (fractionText.length > decimals) return null
    val fraction = fractionText.padEnd(decimals, '0').ifEmpty { "0" }.toLong()
    val minor = whole * like.currency.minorUnitFactor + fraction
    return if (minor > 0) Money(minor, like.currency) else null
}

private fun Money.plain(): String = parts().let { p -> p.whole.replace(",", "") + if (p.fraction.isNotEmpty() && p.fraction.any { it != '0' }) ".${p.fraction}" else "" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentSheet(row: DebtRowState, onSave: (Money) -> Unit, onDismiss: () -> Unit) {
    val colors = LedgerlyTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable(row.id) { mutableStateOf("") }
    val amount = parseAmount(text, row.remaining)
    val half = Money(row.remaining.minorUnits / 2, row.remaining.currency)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = LedgerlyRadius.radiusLg, topEnd = LedgerlyRadius.radiusLg),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LedgerlySpacing.space5)
                .padding(bottom = LedgerlySpacing.space5)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (row.direction == DebtDirection.OwedToMe) "Payment from ${row.counterparty}" else "Payment to ${row.counterparty}",
                    style = LedgerlyTextStyles.heading,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                RoundIconButton(onDismiss, "Close", size = 44.dp) { LedgerlyIcon(LedgerlyIcons.Close, size = 20.dp) }
            }
            Text("${row.remaining.label()} left of ${row.principal.label()}", style = LedgerlyTextStyles.label, color = colors.inkMuted)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LedgerlyTextStyles.amount.copy(color = colors.ink),
                placeholder = { Text("${row.remaining.currency.code} 0", style = LedgerlyTextStyles.amount, color = colors.inkFaint) },
                prefix = { Text("${row.remaining.currency.code} ", style = LedgerlyTextStyles.heading, color = colors.inkFaint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(LedgerlyRadius.radiusSm),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.cobalt,
                    unfocusedBorderColor = colors.line,
                    cursorColor = colors.cobalt,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (half.minorUnits > 0) Chip("Half · ${half.label()}", selected = amount == half, onClick = { text = half.plain() })
                Chip("All · ${row.remaining.label()}", selected = amount == row.remaining, onClick = { text = row.remaining.plain() })
            }
            if (amount != null && amount > row.remaining) {
                Text("That's more than what's left. It will be marked as paid in full.", style = LedgerlyTextStyles.caption, color = colors.inkMuted)
            }
            LedgerlyButton(
                text = if (amount != null) "Record ${amount.label()}" else "Enter an amount",
                onClick = { amount?.let(onSave) },
                enabled = amount != null,
                style = ButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
