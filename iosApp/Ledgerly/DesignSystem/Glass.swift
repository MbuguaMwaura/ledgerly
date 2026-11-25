import SwiftUI

/// Ledgerly on iOS is the Clean design with a Liquid Glass control layer: the tab bar, the + button,
/// round header buttons, pills and toasts turn to glass on iOS 26. Content cards stay solid.
/// Before iOS 26 the same controls fall back to Clean's solid `surface` with `shadow-float`.
extension View {
    @ViewBuilder
    func glassControl<S: Shape>(in shape: S, tint: Color? = nil, interactive: Bool = true) -> some View {
        if #available(iOS 26.0, *) {
            if let tint {
                glassEffect(.regular.tint(tint).interactive(interactive), in: shape)
            } else {
                glassEffect(.regular.interactive(interactive), in: shape)
            }
        } else {
            background(tint ?? LedgerlyColor.surface, in: shape)
                .floatShadow()
        }
    }

    /// `shadow-float` from the tokens: floating controls only; cards are flat.
    func floatShadow() -> some View {
        shadow(color: Color.black.opacity(0.12), radius: 15, x: 0, y: 10)
    }
}

/// Groups glass controls so they blend and morph together on iOS 26.
struct GlassGroup<Content: View>: View {
    var spacing: CGFloat = 12
    @ViewBuilder var content: Content

    var body: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: spacing) { content }
        } else {
            content
        }
    }
}
