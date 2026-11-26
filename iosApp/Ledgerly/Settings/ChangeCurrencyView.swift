import Shared
import SwiftUI

/// Switches the currency after setup, moving everything already logged at the rate given.
struct ChangeCurrencyView: View {
    let onDone: (String) -> Void
    let onClose: () -> Void

    @State private var model = ChangeCurrencyModel()

    var body: some View {
        let state = model.state
        let viewModel = model.viewModel

        NavigationStack {
            ScrollView {
                if !state.isLoading {
                    VStack(alignment: .leading, spacing: 20) {
                        Text("You log in \(state.fromText). Everything already logged moves to the new currency: entries, budgets and debts.")
                            .ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.inkMuted)

                        FormField(label: "New currency") {
                            FlowLayout {
                                ForEach(state.options, id: \.label) { option in
                                    ChipButton(title: option.code.code, selected: option.code == state.selected) {
                                        viewModel.onCurrencySelect(code: option.code)
                                    }
                                }
                            }
                            if let selected = state.options.first(where: { $0.code == state.selected }) {
                                Text(selected.label).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted).padding(.leading, 4)
                            }
                        }

                        FormField(label: "Exchange rate") {
                            HStack(spacing: 8) {
                                Text(state.rateLabel).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.inkMuted)
                                TextField("1", text: Binding(get: { model.state.rateText }, set: { viewModel.onRateChange(text: $0) }))
                                    .keyboardType(.decimalPad)
                                    .monospacedDigit()
                                    .accessibilityLabel("Exchange rate, \(state.rateLabel)")
                            }
                            .inputBox()
                            Text("Leave it at 1 to keep the numbers as they are, if you picked the wrong currency at the start.")
                                .ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted).padding(.leading, 4)
                        }

                        if !state.example.isEmpty {
                            Card {
                                Text(state.example).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.ink)
                            }
                        }

                        Button(state.saveLabel) { viewModel.onSave() }
                            .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                            .disabled(!state.canSave)
                    }
                    .padding(LedgerlySpacing.space5)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .background(LedgerlyColor.canvas.ignoresSafeArea())
            .navigationTitle("Change currency")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Back", systemImage: "chevron.left", action: onClose)
                }
            }
        }
        .task { await model.observe() }
        .onChange(of: state.isDone) { _, done in
            if done { onDone(model.state.doneMessage) }
        }
    }
}
