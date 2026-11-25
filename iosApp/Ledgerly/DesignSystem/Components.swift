import SwiftUI

/// A flat card on the canvas.
struct Card<Content: View>: View {
    var color: Color = LedgerlyColor.surface
    var padding: CGFloat = LedgerlySpacing.space5
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) { content }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusLg, style: .continuous))
    }
}

/// A card of rows with hairlines between them.
struct RowsCard<Item: Identifiable, Row: View>: View {
    let items: [Item]
    @ViewBuilder var row: (Item) -> Row

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                if index > 0 { Hairline() }
                row(item)
            }
        }
        .padding(.horizontal, LedgerlySpacing.space5)
        .background(LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusLg, style: .continuous))
    }
}

struct Hairline: View {
    var body: some View {
        Rectangle().fill(LedgerlyColor.line).frame(height: 1)
    }
}

/// "Recent ............ Daily log".
struct SectionHeader: View {
    let title: String
    var action: String?
    var onAction: () -> Void = {}

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title).ledgerlyText(LedgerlyText.heading).foregroundStyle(LedgerlyColor.ink)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            if let action {
                Button(action, action: onAction).buttonStyle(LinkButtonStyle())
            }
        }
        .padding(.horizontal, 4)
        .padding(.top, 24)
        .padding(.bottom, 8)
    }
}

struct LinkButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .ledgerlyText(LedgerlyText.label)
            .foregroundStyle(LedgerlyColor.cobalt)
            .padding(.vertical, 10)
            .opacity(configuration.isPressed ? 0.6 : 1)
            .contentShape(Rectangle())
    }
}

enum PillKind { case primary, soft, quiet, onDeep, onDeepQuiet }

/// The pill buttons. Primary is the `action` fill, the strongest button on a screen.
struct PillButtonStyle: ButtonStyle {
    var kind: PillKind = .primary
    var small = false
    var fill = false
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        let (background, foreground): (Color, Color) = {
            switch kind {
            case .primary: return isEnabled ? (LedgerlyColor.action, LedgerlyColor.onAction) : (LedgerlyColor.surfaceSunk, LedgerlyColor.inkMuted)
            case .soft: return (LedgerlyColor.cobaltSoft, LedgerlyColor.cobalt)
            case .quiet: return (.clear, LedgerlyColor.inkMuted)
            case .onDeep: return (LedgerlyColor.onDeep, LedgerlyColor.deep)
            case .onDeepQuiet: return (.clear, LedgerlyColor.onDeepMuted)
            }
        }()
        configuration.label
            .font(.system(size: small ? 14 : 15, weight: .medium))
            .foregroundStyle(foreground)
            .padding(.horizontal, LedgerlySpacing.space5)
            .frame(maxWidth: fill ? .infinity : nil, minHeight: small ? 38 : 50)
            .background(background, in: Capsule())
            .contentShape(Capsule())
            .opacity(configuration.isPressed ? 0.75 : 1)
    }
}

/// A two- or three-way switch, e.g. "Owed to you | You owe".
struct Segmented<T: Hashable>: View {
    let options: [T]
    let selection: T
    let label: (T) -> String
    let onSelect: (T) -> Void
    var background: Color = LedgerlyColor.surface

    var body: some View {
        HStack(spacing: 4) {
            ForEach(options, id: \.self) { option in
                let on = option == selection
                Button { onSelect(option) } label: {
                    Text(label(option))
                        .ledgerlyText(LedgerlyText.label)
                        .foregroundStyle(on ? LedgerlyColor.surface : LedgerlyColor.inkMuted)
                        .frame(maxWidth: .infinity, minHeight: 38)
                        .background(on ? LedgerlyColor.ink : Color.clear, in: Capsule())
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(on ? .isSelected : [])
            }
        }
        .padding(4)
        .background(background, in: Capsule())
    }
}

/// A picker pill; selected is the `ink` fill.
struct ChipButton: View {
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .ledgerlyText(LedgerlyText.label)
                .foregroundStyle(selected ? LedgerlyColor.surface : LedgerlyColor.ink)
                .padding(.horizontal, 16)
                .frame(minHeight: 40)
                .background(selected ? LedgerlyColor.ink : LedgerlyColor.surface, in: Capsule())
                .overlay(Capsule().stroke(selected ? LedgerlyColor.ink : LedgerlyColor.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

/// A round letter tile, tinted by tone.
struct LetterTile: View {
    let text: String
    var background: Color = LedgerlyColor.surface
    var foreground: Color = LedgerlyColor.ink
    var bordered = false

    var body: some View {
        Text(text)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(foreground)
            .frame(width: 44, height: 44)
            .background(background, in: Circle())
            .overlay(Circle().stroke(bordered ? LedgerlyColor.line : .clear, lineWidth: 1))
            .accessibilityHidden(true)
    }
}

/// Diagonal hatching: future bars, empty tracks.
struct Hatch: View {
    var color: Color = LedgerlyColor.line

    var body: some View {
        Canvas { context, size in
            var x = -size.height
            while x < size.width {
                var path = Path()
                path.move(to: CGPoint(x: x, y: size.height))
                path.addLine(to: CGPoint(x: x + size.height, y: 0))
                context.stroke(path, with: .color(color), lineWidth: 2)
                x += 5
            }
        }
    }
}

/// A progress track: hatched when empty, filled up to `fraction`.
struct Track: View {
    let fraction: Double
    let fill: Color
    var height: CGFloat = 8

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Hatch()
                Capsule().fill(fill).frame(width: geo.size.width * min(max(fraction, 0), 1))
            }
        }
        .frame(height: height)
        .clipShape(Capsule())
    }
}

struct EmptyNote: View {
    let text: String

    var body: some View {
        Text(text)
            .ledgerlyText(LedgerlyText.body)
            .foregroundStyle(LedgerlyColor.inkMuted)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 8)
            .padding(.vertical, 28)
    }
}

/// Room under the content for the floating tab bar.
let tabBarClearance: CGFloat = 120
