import SwiftUI
import UIKit

/// A text style from the design tokens. Inter is the brand face; until it is bundled,
/// the system font stands in at the same size, weight and tracking.
struct LedgerlyTextStyle {
    let size: CGFloat
    let lineHeight: CGFloat
    let weight: Int
    let trackingEm: CGFloat

    var fontWeight: Font.Weight {
        switch weight {
        case ..<350: .light
        case ..<450: .regular
        case ..<550: .medium
        case ..<650: .semibold
        default: .bold
        }
    }
}

extension View {
    func ledgerlyText(_ style: LedgerlyTextStyle) -> some View {
        font(.system(size: style.size, weight: style.fontWeight))
            .tracking(style.size * style.trackingEm)
            .lineSpacing(max(0, style.lineHeight - style.size * 1.2))
    }
}

extension Color {
    /// A colour that follows light and dark mode. Values are 0xAARRGGBB, as the generator writes them.
    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { traits in
            UIColor(argb: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

private extension UIColor {
    convenience init(argb: UInt32) {
        self.init(
            red: CGFloat((argb >> 16) & 0xFF) / 255,
            green: CGFloat((argb >> 8) & 0xFF) / 255,
            blue: CGFloat(argb & 0xFF) / 255,
            alpha: CGFloat((argb >> 24) & 0xFF) / 255
        )
    }
}
