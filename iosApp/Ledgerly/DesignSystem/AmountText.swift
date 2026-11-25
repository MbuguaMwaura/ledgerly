import Shared
import SwiftUI

/// A money figure: the whole number in `ink`, the currency code and cents in `inkFaint`.
/// The design system allows the faint parts only at 24pt and up, so smaller styles stay in `ink`.
struct AmountText: View {
    let money: Money
    let style: LedgerlyTextStyle
    var showFraction = true

    var body: some View {
        let parts = money.parts()
        let faint = style.size >= 24 ? LedgerlyColor.inkFaint : LedgerlyColor.ink
        let sign = parts.sign == .negative ? "− " : ""
        let fraction = showFraction && !parts.fraction.isEmpty ? ".\(parts.fraction)" : ""

        (Text(sign).foregroundStyle(LedgerlyColor.ink)
            + Text("\(parts.currencyCode) ").foregroundStyle(faint)
            + Text(parts.whole).foregroundStyle(LedgerlyColor.ink)
            + Text(fraction).foregroundStyle(faint))
            .ledgerlyText(style)
            .monospacedDigit()
            .accessibilityLabel(parts.format(withFraction: showFraction))
    }
}
