import Shared
import SwiftUI

/// Profile, appearance, money, habit, what you track, and about.
struct SettingsView: View {
    let onEditBudgets: () -> Void
    let onClose: () -> Void

    @State private var model = SettingsModel()
    @State private var name: String?

    var body: some View {
        let state = model.state
        let viewModel = model.viewModel

        NavigationStack {
            Form {
                if !state.isLoading {
                    Section {
                        HStack(spacing: 12) {
                            LetterTile(text: state.initials.isEmpty ? "?" : state.initials, background: LedgerlyColor.cobaltSoft, foreground: LedgerlyColor.cobalt)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(state.name.isEmpty ? "You" : state.name).ledgerlyText(LedgerlyText.heading).foregroundStyle(LedgerlyColor.ink)
                                if !state.profileLine.isEmpty {
                                    Text(state.profileLine).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                                }
                            }
                        }
                        TextField("Your name", text: Binding(
                            get: { name ?? state.name },
                            set: { value in
                                name = value
                                viewModel.onNameChange(name: value)
                            }
                        ))
                        .textContentType(.name)
                    }

                    Section("Appearance") {
                        Picker("Theme", selection: Binding(get: { state.theme }, set: { viewModel.onThemeChange(theme: $0) })) {
                            ForEach(ThemeChoice.allCases, id: \.self) { theme in
                                Text(themeTitle(theme)).tag(theme)
                            }
                        }
                        .pickerStyle(.segmented)
                    }

                    Section("Money") {
                        if state.trackBudgets {
                            Button(action: onEditBudgets) {
                                LabeledContent {
                                    Image(systemName: "chevron.right").foregroundStyle(LedgerlyColor.inkFaint)
                                } label: {
                                    Text("Budgets").foregroundStyle(LedgerlyColor.ink)
                                    Text(state.budgetsText)
                                }
                            }
                        }
                        LabeledContent {
                            Text(state.currencyText)
                        } label: {
                            Text("Currency")
                            Text("Chosen when you started")
                        }
                    }

                    Section("Daily habit") {
                        LabeledContent {
                            Text(state.freezesText)
                        } label: {
                            Text("Streak freezes")
                            Text("Each one covers a day you miss")
                        }
                    }

                    Section {
                        Toggle(isOn: .constant(true)) {
                            Text("Spending")
                            Text("Always on. It's what your streak counts.")
                        }
                        .disabled(true)
                        Toggle("Budgets", isOn: Binding(get: { state.trackBudgets }, set: { viewModel.onTrackBudgetsChange(on: $0) }))
                        Toggle("Debts", isOn: Binding(get: { state.trackDebts }, set: { viewModel.onTrackDebtsChange(on: $0) }))
                    } header: {
                        Text("What you track")
                    } footer: {
                        Text("Turning one off hides its tab and cards. Nothing is deleted.")
                    }

                    Section("About") {
                        LabeledContent("Version", value: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "")
                        LabeledContent {
                            EmptyView()
                        } label: {
                            Text("Your data")
                            Text("Stored only on this phone")
                        }
                    }
                }
            }
            .tint(LedgerlyColor.cobalt)
            .scrollContentBackground(.hidden)
            .background(LedgerlyColor.canvas.ignoresSafeArea())
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", action: onClose)
                }
            }
        }
        .task { await model.observe() }
    }

    private func themeTitle(_ theme: ThemeChoice) -> String {
        switch theme {
        case .light: "Light"
        case .dark: "Dark"
        default: "Match phone"
        }
    }
}
