import Shared
import SwiftUI

struct HomeView: View {
    let model: HomeModel
    let onLog: () -> Void
    let onOpenSpend: () -> Void
    let onOpenDebts: () -> Void
    let onOpenEntry: (String) -> Void
    let onEditBudgets: () -> Void
    let onOpenSettings: () -> Void
    let firstName: String
    let trackBudgets: Bool
    let trackDebts: Bool

    var body: some View {
        let state = model.state
        let hasBudgets = trackBudgets && !state.totalBudget.isZero

        ScrollView {
            VStack(alignment: .leading, spacing: LedgerlySpacing.space3) {
                header

                Card {
                    HStack {
                        Text(hasBudgets ? "Left to spend" : "Spent this month").ledgerlyText(LedgerlyText.heading).foregroundStyle(LedgerlyColor.ink)
                        Spacer()
                        Text(Date.now.formatted(.dateTime.month(.wide))).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                    }
                    AmountText(money: hasBudgets ? state.leftToSpend : state.spent, style: LedgerlyText.amountHero)
                        .padding(.top, 6)
                    subline(state, hasBudgets: hasBudgets)
                        .padding(.top, 6)
                    if trackBudgets && !hasBudgets && !state.isLoading {
                        Button("Set budgets", action: onEditBudgets)
                            .buttonStyle(PillButtonStyle(kind: .soft, small: true))
                            .padding(.top, 12)
                    }
                    HStack(spacing: 0) {
                        splitCell("Spent", caption: hasBudgets ? "\(state.budgetPercentUsed)% of budget" : "This month", money: state.spent)
                        Rectangle().fill(LedgerlyColor.line).frame(width: 1).padding(.vertical, 12)
                        splitCell("Income", caption: "This month", money: state.income)
                    }
                    .fixedSize(horizontal: false, vertical: true)
                    .background(LedgerlyColor.surfaceSunk, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusMd, style: .continuous))
                    .padding(.top, 20)
                }

                StreakCardView(
                    streak: state.streak,
                    week: state.week,
                    nextMilestone: state.nextMilestone.map { Int($0.intValue) },
                    onLog: onLog,
                    onNothingSpent: { model.viewModel.onNothingSpentToday() }
                )

                if hasBudgets {
                    Card {
                        HStack {
                            Text("Budgets").ledgerlyText(LedgerlyText.heading).foregroundStyle(LedgerlyColor.ink)
                            Spacer()
                            Button("Edit", action: onEditBudgets).buttonStyle(LinkButtonStyle())
                        }
                        GaugeView(fraction: Double(state.budgetPercentUsed) / 100) {
                            Text("Budget used").ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                            Text("\(state.budgetPercentUsed)%").ledgerlyText(LedgerlyText.display).foregroundStyle(LedgerlyColor.ink).minimumScaleFactor(0.6)
                            Text("of \(state.totalBudget.label())").ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                        }
                        .frame(maxWidth: .infinity)
                        Text(needsALookText(state.needsALook.count))
                            .ledgerlyText(LedgerlyText.caption)
                            .foregroundStyle(LedgerlyColor.inkMuted)
                            .frame(maxWidth: .infinity)
                            .padding(.bottom, 6)
                        ForEach(state.needsALook) { row in
                            BudgetRowView(row: row, daysLeft: Int(state.daysLeft))
                        }
                    }
                }

                SectionHeader(title: "Recent", action: "Daily log", onAction: onOpenSpend)
                    .padding(.top, -LedgerlySpacing.space3)
                if state.recent.isEmpty {
                    Card { EmptyNote(text: state.isLoading ? "" : "Nothing logged this month yet. Tap + to log your first expense.") }
                } else {
                    RowsCard(items: state.recent) { entry in
                        Button { onOpenEntry(entry.id) } label: { EntryRowView(row: entry) }
                            .buttonStyle(.plain)
                    }
                }

                if trackDebts && !state.debtsDueSoon.isEmpty {
                    SectionHeader(title: "Owed to you, due soon", action: "All debts", onAction: onOpenDebts)
                        .padding(.top, -LedgerlySpacing.space3)
                    ForEach(state.debtsDueSoon) { row in
                        DueSoonRow(row: row, action: onOpenDebts)
                    }
                }
            }
            .padding(.horizontal, LedgerlySpacing.space5)
            .padding(.bottom, tabBarClearance)
        }
        .background(LedgerlyColor.canvas.ignoresSafeArea())
    }

    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text(Date.now.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated).year()))
                    .ledgerlyText(LedgerlyText.label)
                    .foregroundStyle(LedgerlyColor.inkMuted)
                Text(firstName.isEmpty ? greeting : "\(greeting), \(firstName)")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(LedgerlyColor.ink)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
                    .accessibilityAddTraits(.isHeader)
            }
            Spacer(minLength: 8)
            Button(action: onOpenSettings) {
                Image(systemName: "slider.horizontal.3")
                    .font(.system(size: 17, weight: .medium))
                    .foregroundStyle(LedgerlyColor.ink)
                    .frame(width: 44, height: 44)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .glassControl(in: Circle())
            .accessibilityLabel("Settings")
        }
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    private var greeting: String {
        switch Calendar.current.component(.hour, from: .now) {
        case 5..<12: "Good morning"
        case 12..<17: "Good afternoon"
        default: "Good evening"
        }
    }

    @ViewBuilder
    private func subline(_ state: HomeUiState, hasBudgets: Bool) -> some View {
        if !hasBudgets && !trackBudgets {
            Text("Across everything you logged this month.")
                .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
        } else if !hasBudgets {
            Text("Set budgets to see what's left each day.")
                .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
        } else if state.leftToSpend.minorUnits < 0 {
            (Text("\(state.leftToSpend.unaryMinus().label()) over").foregroundStyle(LedgerlyColor.loss).fontWeight(.semibold)
                + Text(" your budgets this month").foregroundStyle(LedgerlyColor.inkMuted))
                .ledgerlyText(LedgerlyText.label)
        } else {
            let days = Int(state.daysLeft)
            (Text("\(state.perDay.label()) a day").foregroundStyle(LedgerlyColor.ink).fontWeight(.semibold)
                + Text(" for the last \(days) \(days == 1 ? "day" : "days")").foregroundStyle(LedgerlyColor.inkMuted))
                .ledgerlyText(LedgerlyText.label)
        }
    }

    private func splitCell(_ title: String, caption: String, money: Money) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.ink)
            AmountText(money: money, style: LedgerlyText.amount, showFraction: false)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            Text(caption).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func needsALookText(_ count: Int) -> String {
        switch count {
        case 0: "Every budget is on track."
        case 1: "1 category needs a look."
        default: "\(count) categories need a look."
        }
    }
}

/// A compact debtor row on Home. Tapping it opens Debts, where reminders and payments live.
private struct DueSoonRow: View {
    let row: DebtRowState
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                LetterTile(text: initials(row.counterparty), bordered: true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(row.counterparty).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.ink)
                    Text(row.dueText).ledgerlyText(LedgerlyText.caption).foregroundStyle(row.dueColor)
                }
                Spacer()
                Text(row.remaining.label()).ledgerlyText(LedgerlyText.amountRow).foregroundStyle(LedgerlyColor.ink).monospacedDigit()
            }
            .padding(LedgerlySpacing.space5)
            .background(row.isOverdue ? LedgerlyColor.lossSoft : LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusMd, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
