import Shared
import SwiftUI

/// Title and amount on one line.
private struct RowLine: View {
    let title: String
    let trailing: String
    var trailingColor: Color = LedgerlyColor.ink

    var body: some View {
        HStack(spacing: 12) {
            Text(title).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.ink).lineLimit(1)
            Spacer(minLength: 0)
            Text(trailing).ledgerlyText(LedgerlyText.amountRow).foregroundStyle(trailingColor).monospacedDigit().lineLimit(1)
        }
    }
}

/// One logged expense or income.
struct EntryRowView: View {
    let row: EntryRowState

    var body: some View {
        let income = row.kind == .income
        HStack(spacing: 12) {
            if income {
                LetterTile(text: firstLetter(row.title), background: LedgerlyColor.gainSoft, foreground: LedgerlyColor.gain)
            } else {
                LetterTile(text: firstLetter(row.title), bordered: true)
            }
            VStack(alignment: .leading, spacing: 2) {
                RowLine(title: row.title, trailing: row.signedAmount.label(signed: true), trailingColor: income ? LedgerlyColor.gain : LedgerlyColor.ink)
                Text("\(row.categoryName) · \(row.time)").ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
            }
        }
        .padding(.vertical, 14)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

/// A category budget: spent of limit, a track, and what's left.
struct BudgetRowView: View {
    let row: BudgetRowState
    let daysLeft: Int?

    var body: some View {
        let (tile, tone): (Color, Color) = {
            switch row.tone {
            case .over: return (LedgerlyColor.lossSoft, LedgerlyColor.loss)
            case .caution: return (LedgerlyColor.cautionSoft, LedgerlyColor.caution)
            default: return (LedgerlyColor.gainSoft, LedgerlyColor.gain)
            }
        }()
        let onTrack = row.tone != .over && row.tone != .caution
        HStack(spacing: 12) {
            LetterTile(text: firstLetter(row.categoryName), background: tile, foreground: tone)
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(row.categoryName).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.ink)
                    Spacer()
                    Text("\(row.spent.label()) / \(row.limit.parts().whole)").ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink).monospacedDigit()
                }
                Track(fraction: Double(row.percentUsed) / 100, fill: tone)
                Text(note).ledgerlyText(LedgerlyText.caption).foregroundStyle(onTrack ? LedgerlyColor.inkMuted : tone)
            }
        }
        .padding(.vertical, 14)
        .accessibilityElement(children: .combine)
    }

    private var note: String {
        var text = row.percentUsed >= 80 ? "\(row.percentUsed)% · " : ""
        if row.tone == .over {
            text += "\(row.overBy.label()) over"
        } else {
            text += "\(row.left.label()) left"
            if let daysLeft { text += " · \(daysLeft) \(daysLeft == 1 ? "day" : "days") to go" }
        }
        return text
    }
}

extension ReminderTone {
    var title: String {
        switch self {
        case .friendly: return "Friendly"
        case .clear: return "Clear"
        default: return "Firm"
        }
    }
}

extension DebtRowState {
    var isOverdue: Bool {
        if case .overdue = onEnum(of: status) { return true }
        return false
    }

    var isSettled: Bool {
        if case .settled = onEnum(of: status) { return true }
        return false
    }

    var dueText: String {
        switch onEnum(of: status) {
        case .settled: return "Settled"
        case .noDueDate: return "No due date"
        case .overdue: return "Overdue · was due \(dueDate?.short ?? "")"
        case .dueSoon(let soon): return soon.days == 0 ? "Due today" : "Due \(dueDate?.short ?? "")"
        case .upcoming: return "Due \(dueDate?.short ?? "")"
        }
    }

    var dueColor: Color {
        switch onEnum(of: status) {
        case .overdue: return LedgerlyColor.loss
        case .dueSoon: return LedgerlyColor.caution
        default: return LedgerlyColor.inkMuted
        }
    }

    var remindedText: String? {
        guard let days = daysSinceReminder?.intValue else { return nil }
        switch days {
        case 0: return "Reminded today"
        case 1: return "Reminded yesterday"
        default: return "Reminded \(days) days ago"
        }
    }
}

extension DebtRowState: @retroactive Identifiable {}
extension EntryRowState: @retroactive Identifiable {}
extension BudgetRowState: @retroactive Identifiable {
    public var id: String { categoryId }
}

/// Someone who owes the user. Remind picks a tone, shows the draft, and hands it to the share sheet.
struct DebtorRowView: View {
    let row: DebtRowState
    let draft: (ReminderTone) -> String?
    let onRecordPayment: () -> Void
    let onSend: (String) -> Void
    var showActions = true

    @State private var open = false
    @State private var tone: ReminderTone = .friendly

    var body: some View {
        let overdue = row.isOverdue
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                LetterTile(text: initials(row.counterparty), bordered: true)
                VStack(alignment: .leading, spacing: 4) {
                    RowLine(title: row.counterparty, trailing: row.remaining.label())
                    HStack {
                        Text(row.dueText).ledgerlyText(LedgerlyText.caption).foregroundStyle(row.dueColor)
                        Spacer()
                        if let reminded = row.remindedText {
                            Text(reminded).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                        }
                    }
                    if !row.paid.isZero {
                        Track(fraction: Double(row.percentPaid) / 100, fill: LedgerlyColor.gain, height: 5).padding(.top, 6)
                        Text("\(row.paid.label()) of \(row.principal.label()) paid").ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                    }
                }
            }
            .padding(.top, LedgerlySpacing.space5)
            .padding(.bottom, 12)

            if !showActions {
                Spacer().frame(height: 8)
            } else if row.isSettled {
                Text("Paid in full").ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.gain)
                    .padding(.bottom, LedgerlySpacing.space5)
            } else if open {
                let text = draft(tone) ?? ""
                VStack(alignment: .leading, spacing: 12) {
                    Segmented(
                        options: ReminderTone.allCases,
                        selection: tone,
                        label: { $0.title },
                        onSelect: { tone = $0 },
                        background: overdue ? LedgerlyColor.surface : LedgerlyColor.surfaceSunk
                    )
                    Text(text)
                        .ledgerlyText(LedgerlyText.body)
                        .foregroundStyle(LedgerlyColor.ink)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(overdue ? LedgerlyColor.surface : LedgerlyColor.surfaceSunk, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusMd, style: .continuous))
                    HStack(spacing: 8) {
                        Spacer()
                        Button("Cancel") { open = false }.buttonStyle(PillButtonStyle(kind: .quiet))
                        Button {
                            open = false
                            if !text.isEmpty { onSend(text) }
                        } label: {
                            Label("Send reminder", systemImage: "paperplane")
                        }
                        .buttonStyle(PillButtonStyle(kind: .primary))
                    }
                }
                .padding(.bottom, LedgerlySpacing.space5)
            } else {
                HStack(spacing: 8) {
                    Spacer()
                    Button("Record payment", action: onRecordPayment).buttonStyle(PillButtonStyle(kind: .quiet, small: true))
                    Button {
                        tone = overdue ? .clear : .friendly
                        open = true
                    } label: {
                        Label("Remind", systemImage: "paperplane")
                    }
                    .buttonStyle(PillButtonStyle(kind: .soft, small: true))
                }
                .padding(.bottom, LedgerlySpacing.space5)
            }
        }
        .padding(.horizontal, LedgerlySpacing.space5)
        .background(overdue ? LedgerlyColor.lossSoft : LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusMd, style: .continuous))
    }
}

/// Money the user owes: balance, how much is paid off, and Record payment.
struct LoanRowView: View {
    let row: DebtRowState
    let onRecordPayment: () -> Void
    var showActions = true

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                LetterTile(text: initials(row.counterparty), bordered: true)
                VStack(alignment: .leading, spacing: 4) {
                    RowLine(title: row.counterparty, trailing: row.remaining.label())
                    Text(row.dueText).ledgerlyText(LedgerlyText.caption).foregroundStyle(row.dueColor)
                    Track(fraction: Double(row.percentPaid) / 100, fill: LedgerlyColor.gain, height: 5).padding(.top, 6)
                    Text("\(row.percentPaid)% paid off of \(row.principal.label())").ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                }
            }
            .padding(.top, LedgerlySpacing.space5)
            .padding(.bottom, 12)

            if !showActions {
                Spacer().frame(height: 8)
            } else {
                HStack {
                    if row.isSettled {
                        Text("Paid off").ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.gain)
                    } else {
                        Spacer()
                        Button("Record payment", action: onRecordPayment).buttonStyle(PillButtonStyle(kind: .soft, small: true))
                    }
                }
                .padding(.bottom, LedgerlySpacing.space5)
            }
        }
        .padding(.horizontal, LedgerlySpacing.space5)
        .background(row.isOverdue ? LedgerlyColor.lossSoft : LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusMd, style: .continuous))
    }
}
