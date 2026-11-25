import Shared
import SwiftUI

/// One entry: what it was, when, and fields to fix the amount, category or note, or delete it.
struct TransactionDetailView: View {
    @State private var model: TransactionDetailModel
    @State private var confirming = false
    @State private var deleted = false
    let onDone: (String) -> Void
    let onClose: () -> Void

    init(entryId: String, onDone: @escaping (String) -> Void, onClose: @escaping () -> Void) {
        _model = State(initialValue: TransactionDetailModel(entryId: entryId))
        self.onDone = onDone
        self.onClose = onClose
    }

    var body: some View {
        let state = model.state
        let viewModel = model.viewModel

        NavigationStack {
            ScrollView {
                if !state.isLoading {
                    VStack(alignment: .leading, spacing: 20) {
                        VStack(spacing: 6) {
                            Text(state.title).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.ink)
                            AmountText(money: state.signedAmount, style: LedgerlyText.amountHero)
                            Text(state.whenText).ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top, 12)
                        .padding(.bottom, 4)

                        FormField(label: "Amount") {
                            AmountField(
                                currency: state.signedAmount.currency.code,
                                text: Binding(get: { model.state.amountText }, set: { viewModel.onAmountChange(text: $0) })
                            )
                        }

                        FormField(label: "Category") {
                            FlowLayout {
                                ForEach(state.categories, id: \.id) { category in
                                    ChipButton(title: category.name, selected: category.id == state.selectedCategoryId) {
                                        viewModel.onSelectCategory(id: category.id)
                                    }
                                }
                            }
                        }

                        FormField(label: "Note", optional: true) {
                            TextField("What was it?", text: Binding(get: { model.state.note }, set: { viewModel.onNoteChange(note: $0) }))
                                .inputBox()
                        }

                        if let budget = state.budget {
                            FormField(label: "How it fits") {
                                Card { BudgetRowView(row: budget, daysLeft: nil) }
                            }
                        }

                        VStack(spacing: 4) {
                            if state.hasChanges {
                                Button("Save changes") { viewModel.onSave() }
                                    .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                                    .disabled(!state.canSave)
                            }
                            Button(state.deleteLabel, role: .destructive) { confirming = true }
                                .buttonStyle(PillButtonStyle(kind: .quiet, fill: true))
                        }
                        .padding(.top, 8)
                    }
                    .padding(LedgerlySpacing.space5)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .background(LedgerlyColor.canvas.ignoresSafeArea())
            .navigationTitle(state.kindLabel)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", action: onClose)
                }
                ToolbarItem(placement: .primaryAction) {
                    if !state.isLoading {
                        Button(state.deleteLabel, systemImage: "trash", role: .destructive) { confirming = true }
                    }
                }
            }
            .confirmationDialog("Delete this entry?", isPresented: $confirming, titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    deleted = true
                    viewModel.onDelete()
                }
                Button("Keep it", role: .cancel) {}
            } message: {
                Text("\(state.title), \(state.signedAmount.label(signed: true)). This can't be undone.")
            }
        }
        .task { await model.observe() }
        .onChange(of: state.isDone) { _, done in
            if done { onDone(deleted ? "\(state.kindLabel) deleted." : "Changes saved.") }
        }
        .onChange(of: state.notFound) { _, missing in
            if missing { onClose() }
        }
    }
}
