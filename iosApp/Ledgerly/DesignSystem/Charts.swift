import Shared
import SwiftUI

/// Daily spending for the month. Past days are bars scaled to the biggest day, future days are
/// hatched. The highlighted day (the selected one, else today) gets the gradient and a tip.
struct SpendBarsView: View {
    let bars: [DayBarState]
    var onSelect: ((Kotlinx_datetimeLocalDate) -> Void)?

    var body: some View {
        let highlighted = bars.first(where: { $0.isSelected }) ?? bars.first(where: { $0.isToday })
        let maxSpent = max(bars.filter { !$0.isFuture }.map { $0.spent.minorUnits }.max() ?? 1, 1)
        let past = LedgerlyColor.cobalt.opacity(0.3)

        VStack(alignment: .leading, spacing: 12) {
            if let highlighted {
                VStack(alignment: .leading, spacing: 2) {
                    Text(highlighted.date.short).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                    Text(highlighted.isToday && highlighted.spent.isZero ? "Nothing logged yet" : highlighted.spent.label())
                        .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink).monospacedDigit()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusSm, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: LedgerlyRadius.radiusSm, style: .continuous).stroke(LedgerlyColor.line, lineWidth: 1))
            }

            HStack(alignment: .bottom, spacing: 3) {
                ForEach(Array(bars.enumerated()), id: \.offset) { _, bar in
                    let on = bar.date == highlighted?.date
                    let fraction = bar.isFuture ? 1 : min(max(Double(bar.spent.minorUnits) / Double(maxSpent), 0.05), 1)
                    GeometryReader { geo in
                        VStack {
                            Spacer(minLength: 0)
                            Group {
                                if bar.isFuture {
                                    Hatch()
                                } else if on {
                                    LinearGradient(colors: [LedgerlyColor.violet, LedgerlyColor.cobalt], startPoint: .top, endPoint: .bottom)
                                } else {
                                    past
                                }
                            }
                            .frame(height: geo.size.height * fraction)
                            .clipShape(Capsule())
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if !bar.isFuture { onSelect?(bar.date) }
                    }
                    .accessibilityElement()
                    .accessibilityLabel(bar.isFuture ? "Not yet" : "\(bar.date.short), \(bar.spent.label())")
                    .accessibilityAddTraits(bar.isFuture ? [] : .isButton)
                }
            }
            .frame(height: 132)

            HStack {
                ForEach(axisTicks, id: \.self) { index in
                    if index > 0 { Spacer(minLength: 0) }
                    Text(bars[index].date.short)
                        .ledgerlyText(LedgerlyText.caption)
                        .foregroundStyle(LedgerlyColor.inkMuted)
                }
            }
            .accessibilityHidden(true)
        }
    }

    /// The 1st, every 7 days, and the last day.
    private var axisTicks: [Int] {
        guard !bars.isEmpty else { return [] }
        var ticks = [0]
        var day = 7
        while day < bars.count - 3 {
            ticks.append(day)
            day += 7
        }
        ticks.append(bars.count - 1)
        return ticks
    }
}

/// The three-quarter budget gauge: cobalt to violet over a quiet track, figures in the middle.
struct GaugeView<Center: View>: View {
    let fraction: Double
    var size: CGFloat = 210
    var stroke: CGFloat = 22
    @ViewBuilder var center: Center

    var body: some View {
        let value = min(max(fraction, 0), 1)
        ZStack {
            Circle()
                .trim(from: 0, to: 0.75)
                .stroke(LedgerlyColor.line, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(135))
            if value > 0.005 {
                Circle()
                    .trim(from: 0, to: 0.75 * value)
                    .stroke(
                        LinearGradient(colors: [LedgerlyColor.cobalt, LedgerlyColor.violet], startPoint: .bottomLeading, endPoint: .topTrailing),
                        style: StrokeStyle(lineWidth: stroke, lineCap: .round)
                    )
                    .rotationEffect(.degrees(135))
            }
            VStack(spacing: 2) { center }
                .padding(stroke * 2)
        }
        .padding(stroke / 2)
        .frame(width: size, height: size)
        .frame(height: size * 0.86, alignment: .top)
        .accessibilityElement(children: .combine)
    }
}
