import Shared
import SwiftUI

/// New debt: direction, amount, who, what for, when it's due, and a preview of the row.
struct AddDebtView: View {
    @State private var model: AddDebtModel
    let onSaved: (String, DebtDirection) -> Void
    let onClose: () -> Void

    init(direction: DebtDirection, onSaved: @escaping (String, DebtDirection) -> Void, onClose: @escaping () -> Void) {
        _model = State(initialValue: AddDebtModel(direction: direction))
        self.onSaved = onSaved
        self.onClose = onClose
    }

    var body: some View {
        let state = model.state
        let viewModel = model.viewModel

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Segmented(
                        options: DebtDirection.allCases,
                        selection: state.direction,
                        label: { $0 == .owedToMe ? "Someone owes me" : "I owe" },
                        onSelect: { viewModel.onDirectionChange(direction: $0) }
                    )

                    FormField(label: "How much") {
                        AmountField(currency: "KES", text: Binding(get: { model.state.amountText }, set: { viewModel.onAmountChange(text: $0) }))
                    }

                    FormField(label: state.whoLabel) {
                        if !state.people.isEmpty {
                            FlowLayout {
                                ForEach(state.people, id: \.self) { name in
                                    ChipButton(title: name, selected: name == state.counterparty.trimmingCharacters(in: .whitespaces)) {
                                        viewModel.onCounterpartyChange(name: name)
                                    }
                                }
                            }
                        }
                        TextField("Their name", text: Binding(get: { model.state.counterparty }, set: { viewModel.onCounterpartyChange(name: $0) }))
                            .textContentType(.name)
                            .inputBox()
                    }

                    FormField(label: "What it's for", optional: true) {
                        TextField("Lunch, the trip, rent", text: Binding(get: { model.state.note }, set: { viewModel.onNoteChange(note: $0) }))
                            .inputBox()
                    }

                    FormField(label: "When it's due") {
                        FlowLayout {
                            ForEach(state.dueChoices, id: \.label) { choice in
                                ChipButton(title: choice.label, selected: choice.choice == state.due) {
                                    viewModel.onDueChange(choice: choice.choice)
                                }
                            }
                        }
                        Text(state.dueText).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted).padding(.leading, 4)
                    }

                    FormField(label: "How it'll look in Debts") {
                        if let preview = state.preview {
                            if preview.direction == .owedToMe {
                                DebtorRowView(row: preview, draft: { _ in nil }, onRecordPayment: {}, onSend: { _ in }, showActions: false)
                            } else {
                                LoanRowView(row: preview, onRecordPayment: {}, showActions: false)
                            }
                        } else {
                            Card { EmptyNote(text: "Add an amount and a name to see how it'll look.") }
                        }
                    }
                    .allowsHitTesting(false)

                    Button(state.saveLabel) { viewModel.onSave() }
                        .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                        .disabled(!state.canSave)
                }
                .padding(LedgerlySpacing.space5)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(LedgerlyColor.canvas.ignoresSafeArea())
            .navigationTitle("New debt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", action: onClose)
                }
            }
        }
        .task { await model.observe() }
        .onChange(of: state.savedMessage) { _, message in
            if let message { onSaved(message, model.state.direction) }
        }
    }
}
