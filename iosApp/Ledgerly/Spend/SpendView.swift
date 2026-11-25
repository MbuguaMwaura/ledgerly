import Shared
import SwiftUI

private enum SpendMode: Hashable { case log, budgets }

struct SpendView: View {
    @State private var model = SpendModel()
    @State private var mode: SpendMode = .log
    @State private var deleting: EntryRowState?

    var body: some View {
        let state = model.state
        let today = state.bars.first(where: { $0.isToday })?.date
        let daysLeft = today.map { Int(state.month.lengthInDays) - Int($0.dayOfMonth) + 1 }

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Card {
                        Text("Spent in \(monthName(Int(state.month.month)))").ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                        AmountText(money: state.spentThisMonth, style: LedgerlyText.amountHero).padding(.top, 6)
                        if let today, !state.spentThisMonth.isZero {
                            let pace = Money(
                                minorUnits: state.spentThisMonth.minorUnits / Int64(today.dayOfMonth) * Int64(state.month.lengthInDays),
                                currency: state.spentThisMonth.currency
                            )
                            (Text("On pace for ").foregroundStyle(LedgerlyColor.inkMuted)
                                + Text(pace.label()).foregroundStyle(LedgerlyColor.ink).fontWeight(.semibold)
                                + Text(" by \(state.month.lastDay.short)").foregroundStyle(LedgerlyColor.inkMuted))
                                .ledgerlyText(LedgerlyText.label)
                                .padding(.top, 6)
                        }
                        SpendBarsView(bars: state.bars) { date in model.viewModel.onSelectDay(date: date) }
                            .padding(.top, 20)
                    }

                    if !state.budgets.isEmpty {
                        Segmented(
                            options: [SpendMode.log, SpendMode.budgets],
                            selection: mode,
                            label: { $0 == .log ? "Daily log" : "Budgets" },
                            onSelect: { mode = $0 }
                        )
                        .padding(.top, 20)
                    }

                    if mode == .log || state.budgets.isEmpty {
                        logSection(state)
                    } else {
                        Text("\(state.budgets.count) budgets this month")
                            .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink)
                            .padding(.leading, 4).padding(.top, 20).padding(.bottom, 8)
                        RowsCard(items: state.budgets) { BudgetRowView(row: $0, daysLeft: daysLeft) }
                    }
                }
                .padding(.horizontal, LedgerlySpacing.space5)
                .padding(.bottom, tabBarClearance)
            }
            .background(LedgerlyColor.canvas.ignoresSafeArea())
            .navigationTitle("Spending")
        }
        .task { await model.observe() }
        .confirmationDialog(
            "Delete this entry?",
            isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }),
            titleVisibility: .visible,
            presenting: deleting
        ) { entry in
            Button("Delete", role: .destructive) { model.viewModel.onDeleteEntry(id: entry.id) }
            Button("Keep it", role: .cancel) {}
        } message: { entry in
            Text("\(entry.title), \(entry.signedAmount.label(signed: true)) at \(entry.time). This can't be undone.")
        }
    }

    @ViewBuilder
    private func logSection(_ state: SpendUiState) -> some View {
        if let selected = state.selectedDay {
            HStack {
                Text("Showing \(selected.short)").ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink)
                Spacer()
                Button("Show all") { model.viewModel.onSelectDay(date: nil) }.buttonStyle(LinkButtonStyle())
            }
            .padding(.horizontal, 4)
            .padding(.top, 16)
        }
        ForEach(Array(state.days.enumerated()), id: \.offset) { _, day in
            HStack {
                Text(day.label).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink)
                Spacer()
                Text(day.spent.label()).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted).monospacedDigit()
            }
            .padding(.horizontal, 4)
            .padding(.top, 20)
            .padding(.bottom, 8)
            RowsCard(items: day.entries) { entry in
                Button { deleting = entry } label: { EntryRowView(row: entry) }
                    .buttonStyle(.plain)
                    .accessibilityHint("Offers to delete this entry")
            }
        }
        if state.days.isEmpty && !state.isLoading {
            EmptyNote(text: state.selectedDay.map { "Nothing logged on \($0.short)." } ?? "Nothing logged this month yet. Tap + to log your first expense.")
                .padding(.top, 12)
        }
    }

    private func monthName(_ month: Int) -> String {
        Calendar.current.monthSymbols[max(0, min(11, month - 1))]
    }
}
