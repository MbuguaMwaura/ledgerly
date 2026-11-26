import Foundation
import Observation
import Shared

/// Whether amounts show as "KES ••••". AppShell keeps it in step with the setting; every
/// `AmountText` and `label()` reads it, so views redraw when it changes.
@Observable
final class AmountPrivacy {
    static let shared = AmountPrivacy()
    var hidden = false
}

extension Money {
    /// The figure's parts, with the digits hidden while amounts are hidden.
    func shownParts() -> MoneyParts {
        let parts = parts()
        return AmountPrivacy.shared.hidden ? parts.hidden() : parts
    }

    /// "KES 1,250". With `signed`, entries read "− KES 650" and "+ KES 4,000". "KES ••••" while amounts are hidden.
    func label(signed: Bool = false, fraction: Bool = false) -> String {
        let parts = shownParts()
        let text = parts.format(withFraction: fraction)
        switch parts.sign {
        case .negative: return "− " + text
        case .positive: return signed ? "+ " + text : text
        default: return text
        }
    }

    var isZero: Bool { minorUnits == 0 }
}

private let monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]

extension Kotlinx_datetimeLocalDate {
    /// "27 Nov".
    var short: String { "\(dayOfMonth) \(monthNames[Int(monthNumber) - 1])" }
}

/// Up to two initials: "Wanjiru Kamau" → "WK".
func initials(_ name: String) -> String {
    let letters = name.split(separator: " ").prefix(2).compactMap { $0.first }.map { String($0).uppercased() }
    return letters.isEmpty ? "?" : letters.joined()
}

/// The first letter of a title, for row tiles.
func firstLetter(_ text: String) -> String {
    text.first.map { String($0).uppercased() } ?? "?"
}
