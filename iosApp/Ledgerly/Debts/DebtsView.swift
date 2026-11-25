import Shared
import SwiftUI
import UIKit

private struct Reminder: Identifiable {
    let id: String
    let text: String
}

struct DebtsView: View {
    @State private var model = DebtsModel()
    @State private var paying: DebtRowState?
    @State private var reminding: Reminder?

    var body: some View {
        let state = model.state
        let owedToMe = state.direction == .owedToMe
        let groups: [(String, [DebtRowState])] = [
            ("Overdue", state.overdue), ("Due soon", state.dueSoon), ("Later", state.later), ("Settled", state.settled),
        ].filter { !$0.1.isEmpty }

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: LedgerlySpacing.space3) {
                    HStack(spacing: LedgerlySpacing.space3) {
                        stat("Coming in", state.totalOwedToMe, caption: "Owed to you")
                        stat("Going out", state.totalIOwe, caption: "You owe")
                    }

                    if owedToMe && !state.overdue.isEmpty {
                        let total = state.overdue.reduce(Money(minorUnits: 0, currency: state.totalOwedToMe.currency)) { $0.plus(other: $1.remaining) }
                        let count = state.overdue.count
                        HStack(spacing: 12) {
                            Circle().fill(LedgerlyColor.loss).frame(width: 10, height: 10)
                            Text("\(count) overdue · \(total.label()) waiting on \(count == 1 ? "one person" : "\(count) people")")
                                .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(LedgerlyColor.lossSoft, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusMd, style: .continuous))
                    }

                    Segmented(
                        options: DebtDirection.allCases,
                        selection: state.direction,
                        label: { $0 == .owedToMe ? "Owed to you" : "You owe" },
                        onSelect: { model.viewModel.onSelectDirection(value: $0) }
                    )
                    .padding(.top, 8)
                    .padding(.bottom, 4)

                    ForEach(groups, id: \.0) { title, rows in
                        Text(title).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted).padding(.leading, 4)
                        ForEach(rows) { row in
                            if owedToMe {
                                DebtorRowView(
                                    row: row,
                                    draft: { tone in model.viewModel.reminderDraft(debtId: row.id, tone: tone) },
                                    onRecordPayment: { paying = row },
                                    onSend: { text in reminding = Reminder(id: row.id, text: text) }
                                )
                            } else {
                                LoanRowView(row: row, onRecordPayment: { paying = row })
                            }
                        }
                    }

                    if groups.isEmpty && !state.isLoading {
                        EmptyNote(text: owedToMe ? "Nobody owes you anything right now." : "You don't owe anyone right now.")
                    }

                    if owedToMe {
                        Text("Reminders go out from your own phone. Ledgerly never messages anyone.")
                            .ledgerlyText(LedgerlyText.caption)
                            .foregroundStyle(LedgerlyColor.inkMuted)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 8)
                    }
                }
                .padding(.horizontal, LedgerlySpacing.space5)
                .padding(.bottom, tabBarClearance)
            }
            .background(LedgerlyColor.canvas.ignoresSafeArea())
            .navigationTitle("Debts")
        }
        .task { await model.observe() }
        .sheet(item: $paying) { row in
            RecordPaymentSheet(row: row) { amount in
                model.viewModel.onRecordPayment(debtId: row.id, amount: amount)
                paying = nil
            } onClose: {
                paying = nil
            }
        }
        .sheet(item: $reminding) { reminder in
            ShareSheet(text: reminder.text) { completed in
                if completed { model.viewModel.onReminderSent(debtId: reminder.id) }
                reminding = nil
            }
            .presentationDetents([.medium, .large])
            .ignoresSafeArea()
        }
    }

    private func stat(_ title: String, _ money: Money, caption: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
            AmountText(money: money, style: LedgerlyText.amount, showFraction: false)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            Text(caption).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusMd, style: .continuous))
    }
}

/// The system share sheet. Ledgerly never sends anything itself; `onFinish` says whether the
/// user actually shared the reminder, so only a real send counts as "reminded".
struct ShareSheet: UIViewControllerRepresentable {
    let text: String
    let onFinish: (Bool) -> Void

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        controller.completionWithItemsHandler = { _, completed, _, _ in onFinish(completed) }
        return controller
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
