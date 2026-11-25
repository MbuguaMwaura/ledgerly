import SwiftUI

/// A labelled form field: the label above, then the input.
struct FormField<Content: View>: View {
    let label: String
    var optional = false
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 0) {
                Text(label).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink)
                if optional {
                    Text(" · Optional").ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                }
            }
            .padding(.leading, 4)
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

extension View {
    /// The white, hairline-bordered box around a text field.
    func inputBox(minHeight: CGFloat = 52) -> some View {
        padding(.horizontal, 16)
            .frame(minHeight: minHeight)
            .background(LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusSm, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: LedgerlyRadius.radiusSm, style: .continuous).stroke(LedgerlyColor.line, lineWidth: 1))
    }
}

/// An amount field: the currency code, then big digits on a decimal pad.
struct AmountField: View {
    let currency: String
    let text: Binding<String>

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Text(currency).ledgerlyText(LedgerlyText.heading).foregroundStyle(LedgerlyColor.inkFaint)
            TextField("0", text: text)
                .keyboardType(.decimalPad)
                .font(.system(size: 28, weight: .medium))
                .monospacedDigit()
                .accessibilityLabel("Amount in \(currency)")
        }
        .inputBox(minHeight: 60)
    }
}

/// Lays chips out in rows, wrapping to the next row when one is full.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(subviews, width: proposal.width ?? .infinity)
        let width = rows.map(\.width).max() ?? 0
        let height = rows.reduce(0) { $0 + $1.height } + spacing * CGFloat(max(0, rows.count - 1))
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for row in arrange(subviews, width: bounds.width) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
                x += size.width + spacing
            }
            y += row.height + spacing
        }
    }

    private struct Row {
        var indices: [Int] = []
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func arrange(_ subviews: Subviews, width: CGFloat) -> [Row] {
        var rows: [Row] = []
        var current = Row()
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(.unspecified)
            let next = current.indices.isEmpty ? size.width : current.width + spacing + size.width
            if !current.indices.isEmpty && next > width {
                rows.append(current)
                current = Row()
            }
            current.width = current.indices.isEmpty ? size.width : current.width + spacing + size.width
            current.height = max(current.height, size.height)
            current.indices.append(index)
        }
        if !current.indices.isEmpty { rows.append(current) }
        return rows
    }
}
