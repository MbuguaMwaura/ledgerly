import Shared
import SwiftUI

/// A monthly limit per expense category, each with − and + to move it by 500.
struct EditBudgetsView: View {
    @State private var model = EditBudgetsModel()
    let onSaved: () -> Void
    let onClose: () -> Void

    var body: some View {
        let state = model.state
        let viewModel = model.viewModel

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    BudgetEditorContent(state: state, viewModel: viewModel)

                    VStack(spacing: 4) {
                        Button("Save budgets") { viewModel.onSave() }
                            .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                            .disabled(!state.canSave)
                        Button("Cancel", action: onClose)
                            .buttonStyle(PillButtonStyle(kind: .quiet, fill: true))
                    }
                    .padding(.top, 24)
                }
                .padding(LedgerlySpacing.space5)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(LedgerlyColor.canvas.ignoresSafeArea())
            .navigationTitle("Budgets")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", systemImage: "xmark", action: onClose)
                }
            }
        }
        .task { await model.observe() }
        .onChange(of: state.isSaved) { _, saved in
            if saved { onSaved() }
        }
    }
}

/// The total card, a row per category and the hint. Shared with onboarding's budgets step.
struct BudgetEditorContent: View {
    let state: EditBudgetsUiState
    let viewModel: EditBudgetsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Card {
                Text("Monthly total").ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                AmountText(money: state.total, style: LedgerlyText.amount, showFraction: false).padding(.top, 4)
                Text(state.totalText).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted).padding(.top, 6)
            }

            Text("Per category")
                .ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                .padding(.leading, 4).padding(.top, 24).padding(.bottom, 8)

            VStack(spacing: 0) {
                ForEach(Array(state.rows.enumerated()), id: \.element.categoryId) { index, row in
                    if index > 0 { Hairline() }
                    BudgetEditRow(row: row, currency: state.currency.code, viewModel: viewModel)
                }
            }
            .padding(.horizontal, LedgerlySpacing.space5)
            .background(LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: LedgerlyRadius.radiusLg, style: .continuous))

            Text("The buttons move a budget by \(state.currency.code) 500. A budget carries over to next month until you change it. Clear one to stop tracking that category.")
                .ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                .padding(.horizontal, 4).padding(.top, 8)
        }
    }
}

private struct BudgetEditRow: View {
    let row: BudgetEditRowState
    let currency: String
    let viewModel: EditBudgetsViewModel

    var body: some View {
        let noteColor: Color = switch row.tone {
        case .caution: LedgerlyColor.caution
        case .over: LedgerlyColor.loss
        default: LedgerlyColor.inkMuted
        }
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(row.categoryName).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.ink)
                Text(row.note).ledgerlyText(LedgerlyText.caption).foregroundStyle(noteColor)
            }
            Spacer(minLength: 0)
            stepButton(symbol: "minus", label: "Lower \(row.categoryName) by \(currency) 500", enabled: row.canDecrease) {
                viewModel.onStep(categoryId: row.categoryId, up: false)
            }
            TextField("0", text: Binding(get: { row.limitText }, set: { viewModel.onLimitChange(categoryId: row.categoryId, text: $0) }))
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .ledgerlyText(LedgerlyText.label)
                .monospacedDigit()
                .frame(width: 84, height: 40)
                .background(LedgerlyColor.surfaceSunk, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .accessibilityLabel("\(row.categoryName) budget in \(currency)")
            stepButton(symbol: "plus", label: "Raise \(row.categoryName) by \(currency) 500", enabled: true) {
                viewModel.onStep(categoryId: row.categoryId, up: true)
            }
        }
        .padding(.vertical, 12)
    }

    private func stepButton(symbol: String, label: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(enabled ? LedgerlyColor.ink : LedgerlyColor.inkFaint)
                .frame(width: 36, height: 36)
                .overlay(Circle().stroke(LedgerlyColor.line, lineWidth: 1))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(label)
    }
}
