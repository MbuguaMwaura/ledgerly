// Generated from designTokens/tokens.json. Do not edit by hand.
// Regenerate with: ./gradlew :designTokens:generateSwiftTokens

import SwiftUI

enum LedgerlyColor {
    /// Screen background behind the cards. Quiet grey so white cards read as objects without borders.
    static let canvas = Color(light: 0xFFF2F2F4, dark: 0xFF0E0F13)
    /// Cards, the tab bar pill, round header buttons, bottom sheets.
    static let surface = Color(light: 0xFFFFFFFF, dark: 0xFF17181D)
    /// Panels inside a card (Spent / Income split, rows), keypad keys, unselected pills.
    static let surfaceSunk = Color(light: 0xFFF6F6F8, dark: 0xFF1F2026)
    /// Hairlines between rows, round-button outlines, progress tracks, future chart bars, gauge hatching.
    static let line = Color(light: 0xFFE8E8ED, dark: 0xFF2A2B33)
    /// Primary text and the whole-number part of amounts.
    static let ink = Color(light: 0xFF0D0D12, dark: 0xFFF4F4F6)
    /// Labels, dates, subtitles, axis text. 4.5:1 on canvas, surface and surface-sunk.
    static let inkMuted = Color(light: 0xFF6B6B76, dark: 0xFF9D9DA8)
    /// Cents and currency codes on amounts 24px and larger only (3:1 rule for large text). Never for small text.
    static let inkFaint = Color(light: 0xFF878792, dark: 0xFF74747F)
    /// The accent: active tab icon, selected bar, links, primary buttons, gauge start. As text on surface or cobalt-soft.
    static let cobalt = Color(light: 0xFF1F2BD1, dark: 0xFF8B96FF)
    /// Text and icons on a cobalt fill.
    static let onCobalt = Color(light: 0xFFFFFFFF, dark: 0xFF0B0D2E)
    /// Active tab circle, selected chip, soft buttons, chart bars before the selected day.
    static let cobaltSoft = Color(light: 0xFFE9EBFD, dark: 0xFF1E2148)
    /// Only the far end of the gauge arc and the streak card's glow. Never text, never a flat fill.
    static let violet = Color(light: 0xFF7B3FF2, dark: 0xFFB28CFF)
    /// The streak card: the one dark hero card on a screen. Text on it is white.
    static let deep = Color(light: 0xFF1A1660, dark: 0xFF231D78)
    /// Text, filled streak days and the Log today button on the deep card.
    static let onDeep = Color(light: 0xFFFFFFFF, dark: 0xFFFFFFFF)
    /// Secondary text on the deep card ('days in a row', weekday letters). 8:1 on deep.
    static let onDeepMuted = Color(light: 0xFFC9C6FF, dark: 0xFFCDC9FF)
    /// The round + button beside the tab bar, and the strongest buttons (Save). Inverts in dark.
    static let action = Color(light: 0xFF0D0D12, dark: 0xFFF4F4F6)
    /// Icon and text on an action fill.
    static let onAction = Color(light: 0xFFFFFFFF, dark: 0xFF0D0D12)
    /// Money in, gains, paid off: '+12.6%' badges (solid with on-signal text), '+KES 1,540' text.
    static let gain = Color(light: 0xFF0B7F3D, dark: 0xFF4ADE80)
    /// Overdue, over budget, losses: '−1.6%' text, Failed / Overdue badges. Always with a sign or a word.
    static let loss = Color(light: 0xFFC8261B, dark: 0xFFFF6B5E)
    /// Text on solid gain and loss badges.
    static let onSignal = Color(light: 0xFFFFFFFF, dark: 0xFF0B0D12)
    /// Tile behind an on-track budget.
    static let gainSoft = Color(light: 0xFFE6F4EC, dark: 0xFF14281C)
    /// Background of an overdue row, tile behind an over-budget category.
    static let lossSoft = Color(light: 0xFFFDECEA, dark: 0xFF2F1614)
    /// Budget at 80–99%, due within 3 days. Text or fill on surface.
    static let caution = Color(light: 0xFF9A6200, dark: 0xFFF5B83D)
    /// Tile behind a nearly-spent budget.
    static let cautionSoft = Color(light: 0xFFFDF3E0, dark: 0xFF2B2210)
}

enum LedgerlyText {
    /// The card's headline number. Cents and currency in ink-faint.
    static let amountHero = LedgerlyTextStyle(size: 40, lineHeight: 44, weight: 500, trackingEm: -0.035)
    /// One giant figure per screen: the streak count, a gauge percentage.
    static let display = LedgerlyTextStyle(size: 64, lineHeight: 64, weight: 500, trackingEm: -0.05)
    /// Secondary figures in split panels (Spent / Income, Owed / Owe).
    static let amount = LedgerlyTextStyle(size: 28, lineHeight: 34, weight: 500, trackingEm: -0.03)
    /// Amounts in list rows, right-aligned.
    static let amountRow = LedgerlyTextStyle(size: 16, lineHeight: 22, weight: 600, trackingEm: 0)
    /// Greeting and centred screen titles.
    static let title = LedgerlyTextStyle(size: 28, lineHeight: 34, weight: 500, trackingEm: -0.025)
    /// Card titles.
    static let heading = LedgerlyTextStyle(size: 18, lineHeight: 24, weight: 500, trackingEm: -0.01)
    /// Row titles and reading text.
    static let body = LedgerlyTextStyle(size: 16, lineHeight: 22, weight: 400, trackingEm: 0)
    /// Buttons, pills, subtitles.
    static let label = LedgerlyTextStyle(size: 14, lineHeight: 18, weight: 500, trackingEm: 0)
    /// Dates, chart axis, badges.
    static let caption = LedgerlyTextStyle(size: 12, lineHeight: 16, weight: 500, trackingEm: 0)
}

enum LedgerlySpacing {
    /// Icon-to-label gap.
    static let space1: CGFloat = 4
    /// Between pills and stacked text.
    static let space2: CGFloat = 8
    /// Between cards in a stack.
    static let space3: CGFloat = 12
    /// Screen gutter and card padding.
    static let space5: CGFloat = 20
    /// Between a card's headline and its chart.
    static let space8: CGFloat = 32
}

enum LedgerlyRadius {
    /// Keypad keys, small panels.
    static let radiusSm: CGFloat = 14
    /// Panels inside cards, rows.
    static let radiusMd: CGFloat = 20
    /// Cards and bottom sheets.
    static let radiusLg: CGFloat = 28
    /// Tab bar, round buttons, badges, pills, bars.
    static let radiusPill: CGFloat = 999
}
