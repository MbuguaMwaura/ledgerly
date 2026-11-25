import Foundation
import Shared

extension Money {
    /// "KES 1,250". With `signed`, entries read "− KES 650" and "+ KES 4,000".
    func label(signed: Bool = false, fraction: Bool = false) -> String {
        let parts = parts()
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
