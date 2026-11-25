import Shared
import SwiftUI

/// The one dark hero card: the streak count, this week, and the day's log buttons.
struct StreakCardView: View {
    let streak: StreakState
    let week: [StreakDayState]
    let nextMilestone: Int?
    let onLog: () -> Void
    let onNothingSpent: () -> Void

    var body: some View {
        let days = Int(streak.current)
        VStack(alignment: .leading, spacing: LedgerlySpacing.space5) {
            HStack(alignment: .top) {
                HStack(alignment: .bottom, spacing: LedgerlySpacing.space2) {
                    Image(systemName: "flame")
                        .font(.system(size: 30, weight: .regular))
                        .padding(.bottom, 14)
                    Text("\(days)")
                        .font(.system(size: 64, weight: .medium))
                        .tracking(-3.2)
                        .monospacedDigit()
                    Text("\(days == 1 ? "day" : "days")\nin a row")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(LedgerlyColor.onDeepMuted)
                        .padding(.leading, 4)
                        .padding(.bottom, 8)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(days) day streak")
                Spacer()
                HStack(spacing: LedgerlySpacing.space1) {
                    Image(systemName: "snowflake").font(.system(size: 13, weight: .semibold))
                    Text("\(streak.freezesHeld)").font(.system(size: 13, weight: .semibold))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.white.opacity(0.14), in: Capsule())
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(streak.freezesHeld) streak freezes")
            }

            HStack {
                ForEach(Array(week.enumerated()), id: \.offset) { index, day in
                    if index > 0 { Spacer(minLength: 0) }
                    WeekDayView(day: day)
                }
            }

            if streak.loggedToday {
                Label {
                    Text(doneText(days: days)).font(.system(size: 15, weight: .medium))
                } icon: {
                    Image(systemName: "checkmark")
                }
            } else {
                HStack(spacing: LedgerlySpacing.space2) {
                    Button("Log today", action: onLog).buttonStyle(PillButtonStyle(kind: .onDeep, fill: true))
                    Button("No spend today", action: onNothingSpent).buttonStyle(PillButtonStyle(kind: .onDeepQuiet))
                }
            }
        }
        .foregroundStyle(LedgerlyColor.onDeep)
        .padding(LedgerlySpacing.space5)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            ZStack(alignment: .topTrailing) {
                LedgerlyColor.deep
                Circle()
                    .fill(RadialGradient(colors: [LedgerlyColor.violet.opacity(0.45), .clear], center: .center, startRadius: 0, endRadius: 150))
                    .frame(width: 300, height: 300)
                    .offset(x: 100, y: -120)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: LedgerlyRadius.radiusLg, style: .continuous))
    }

    private func doneText(days: Int) -> String {
        if streak.milestoneToday != nil { return "\(days) days. That's a milestone." }
        if let next = nextMilestone { return "Logged today. \(next - days) more to \(next) days." }
        return "Logged today. See you tomorrow."
    }
}

private struct WeekDayView: View {
    let day: StreakDayState

    var body: some View {
        let (fill, symbol, symbolColor): (Color, String?, Color) = {
            switch day.mark {
            case .logged: return (LedgerlyColor.onDeep, "checkmark", LedgerlyColor.deep)
            case .frozen: return (Color.white.opacity(0.28), "snowflake", LedgerlyColor.onDeep)
            default: return (Color.white.opacity(0.10), nil, LedgerlyColor.onDeep)
            }
        }()
        VStack(spacing: 6) {
            ZStack {
                Circle().fill(fill)
                if day.isToday { Circle().strokeBorder(LedgerlyColor.onDeep, lineWidth: 2) }
                if let symbol {
                    Image(systemName: symbol).font(.system(size: 12, weight: .bold)).foregroundStyle(symbolColor)
                }
            }
            .frame(width: 34, height: 34)
            Text(day.letter)
                .font(.system(size: 12, weight: day.isToday ? .bold : .medium))
                .foregroundStyle(day.isToday ? LedgerlyColor.onDeep : LedgerlyColor.onDeepMuted)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibility)
    }

    private var accessibility: String {
        switch day.mark {
        case .logged: return "\(day.letter), logged"
        case .frozen: return "\(day.letter), saved by a freeze"
        case .missed: return "\(day.letter), missed"
        default: return day.isToday ? "Today, not logged yet" : "\(day.letter), not yet"
        }
    }
}
