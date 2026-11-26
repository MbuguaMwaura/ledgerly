package app.ledgerly.android.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.money.MoneyParts
import app.ledgerly.shared.money.hidden

/**
 * Whether amounts show as "KES ••••". LedgerlyApp keeps it in step with the setting; every
 * [Amount] and [label] reads it, so they redraw when it changes.
 */
object AmountPrivacy {
    var hidden by mutableStateOf(false)
}

/** The figure's parts, with the digits hidden while amounts are hidden. */
fun Money.shownParts(): MoneyParts = parts().let { if (AmountPrivacy.hidden) it.hidden() else it }
