import Shared
import SwiftUI

private enum SpendMode: Hashable { case log, budgets }

struct SpendView: View {
    let onOpenEntry: (String) -> Void
    let onEditBudgets: () -> Void
    /// Off hides the Budgets view and the Set budgets prompt.
    var trackBudgets = true

    @State private var model = SpendModel()
    @State private var mode: SpendMode = .log

    var body: some View {
        let state = model.state
        let budgets = trackBudgets ? state.budgets : []
        let daysLeft: Int? = Int(state.daysLeft)

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Card {
                        Text(state.title).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                        AmountText(money: state.spentThisMonth, style: LedgerlyText.amountHero).padding(.top, 6)
                        if let pace = state.pace {
                            (Text("On pace for ").foregroundStyle(LedgerlyColor.inkMuted)
                                + Text(pace.label()).foregroundStyle(LedgerlyColor.ink).fontWeight(.semibold)
                                + Text(" by \(state.endLabel)").foregroundStyle(LedgerlyColor.inkMuted))
                                .ledgerlyText(LedgerlyText.label)
                                .padding(.top, 6)
                        }
                        SpendBarsView(bars: state.bars) { date in model.viewModel.onSelectDay(date: date) }
                            .padding(.top, 20)
                    }

                    if !budgets.isEmpty {
                        Segmented(
                            options: [SpendMode.log, SpendMode.budgets],
                            selection: mode,
                            label: { $0 == .log ? "Daily log" : "Budgets" },
                            onSelect: { mode = $0 }
                        )
                        .padding(.top, 20)
                    }

                    if mode == .log || budgets.isEmpty {
                        logSection(state)
                    } else {
                        HStack {
                            Text("\(budgets.count) \(budgets.count == 1 ? "budget" : "budgets") this month")
                                .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink)
                            Spacer()
                            Button("Edit budgets", action: onEditBudgets).buttonStyle(LinkButtonStyle())
                        }
                        .padding(.leading, 4).padding(.top, 12).padding(.bottom, 4)
                        RowsCard(items: budgets) { BudgetRowView(row: $0, daysLeft: daysLeft) }
                    }
                }
                .padding(.horizontal, LedgerlySpacing.space5)
                .padding(.bottom, tabBarClearance)
            }
            .background(LedgerlyColor.canvas.ignoresSafeArea())
            .navigationTitle("Spending")
        }
        .task { await model.observe() }
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
                Button { onOpenEntry(entry.id) } label: { EntryRowView(row: entry) }
                    .buttonStyle(.plain)
                    .accessibilityHint("Opens the entry to edit or delete it")
            }
        }
        if state.days.isEmpty && !state.isLoading {
            EmptyNote(text: state.selectedDay.map { "Nothing logged on \($0.short)." } ?? "Nothing logged this month yet. Tap + to log your first expense.")
                .padding(.top, 12)
        }
        if trackBudgets && state.budgets.isEmpty && !state.isLoading {
            Card {
                Text("No budgets yet").ledgerlyText(LedgerlyText.heading).foregroundStyle(LedgerlyColor.ink)
                Text("Set a monthly limit for the categories you want to watch. Home then shows what's left each day.")
                    .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                    .padding(.top, 4).padding(.bottom, 12)
                Button("Set budgets", action: onEditBudgets).buttonStyle(PillButtonStyle(kind: .soft, small: true))
            }
            .padding(.top, 24)
        }
    }

    private func monthName(_ month: Int) -> String {
        Calendar.current.monthSymbols[max(0, min(11, month - 1))]
    }
}
