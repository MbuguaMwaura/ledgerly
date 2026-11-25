import Shared
import SwiftUI

/// Parses "1,250.50" into money in `like`'s currency. Nil unless it is a positive amount.
func parseAmount(_ text: String, like: Money) -> Money? {
    let cleaned = text.replacingOccurrences(of: ",", with: "").trimmingCharacters(in: .whitespaces)
    guard !cleaned.isEmpty, cleaned.filter({ $0 == "." }).count <= 1, cleaned.allSatisfy({ $0.isNumber || $0 == "." }) else { return nil }
    let decimals = Int(like.currency.decimals)
    let pieces = cleaned.split(separator: ".", omittingEmptySubsequences: false)
    let wholeText = pieces.isEmpty || pieces[0].isEmpty ? "0" : String(pieces[0])
    guard let whole = Int64(wholeText) else { return nil }
    let fractionText = pieces.count > 1 ? String(pieces[1]) : ""
    guard fractionText.count <= decimals else { return nil }
    let padded = fractionText.padding(toLength: decimals, withPad: "0", startingAt: 0)
    let fraction = Int64(padded.isEmpty ? "0" : padded) ?? 0
    let minor = whole * like.currency.minorUnitFactor + fraction
    return minor > 0 ? Money(minorUnits: minor, currency: like.currency) : nil
}

private extension Money {
    /// "1250" or "1250.50", for the text field.
    var plain: String {
        let parts = parts()
        let whole = parts.whole.replacingOccurrences(of: ",", with: "")
        return parts.fraction.allSatisfy({ $0 == "0" }) ? whole : "\(whole).\(parts.fraction)"
    }
}

struct RecordPaymentSheet: View {
    let row: DebtRowState
    let onSave: (Money) -> Void
    let onClose: () -> Void

    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        let amount = parseAmount(text, like: row.remaining)
        let half = Money(minorUnits: row.remaining.minorUnits / 2, currency: row.remaining.currency)

        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text("\(row.remaining.label()) left of \(row.principal.label())")
                    .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(row.remaining.currency.code).ledgerlyText(LedgerlyText.heading).foregroundStyle(LedgerlyColor.inkFaint)
                    TextField("0", text: $text)
                        .keyboardType(.decimalPad)
                        .focused($focused)
                        .font(.system(size: 32, weight: .medium))
                        .monospacedDigit()
                }
                .padding(.horizontal, 16)
                .frame(minHeight: 64)
                .background(LedgerlyColor.surfaceSunk, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusSm, style: .continuous))

                HStack(spacing: 8) {
                    if half.minorUnits > 0 {
                        ChipButton(title: "Half · \(half.label())", selected: amount == half) { text = half.plain }
                    }
                    ChipButton(title: "All · \(row.remaining.label())", selected: amount == row.remaining) { text = row.remaining.plain }
                }
                if let amount, amount.compareTo(other: row.remaining) > 0 {
                    Text("That's more than what's left. It will be marked as paid in full.")
                        .ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                }
                Spacer(minLength: 0)
                Button(amount.map { "Record \($0.label())" } ?? "Enter an amount") {
                    if let amount { onSave(amount) }
                }
                .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                .disabled(amount == nil)
            }
            .padding(LedgerlySpacing.space5)
            .navigationTitle(row.direction == .owedToMe ? "Payment from \(row.counterparty)" : "Payment to \(row.counterparty)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", action: onClose)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .onAppear { focused = true }
    }
}
