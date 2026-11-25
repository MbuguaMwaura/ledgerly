import Shared
import SwiftUI

/// First run: welcome, name and currency, what to track, first budgets, a daily reminder, done.
struct OnboardingView: View {
    @State private var model = OnboardingModel()
    @State private var budgets = EditBudgetsModel()

    var body: some View {
        let state = model.state
        let viewModel = model.viewModel

        VStack(spacing: 0) {
            if state.progress > 0 {
                progressBar(state)
            }
            switch state.step {
            case .welcome:
                step(footer: {
                    Button("Get started") { viewModel.onNext() }.buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                }) {
                    Spacer()
                    Image(systemName: "flame.fill")
                        .font(.system(size: 30, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 64, height: 64)
                        .background(LedgerlyColor.cobalt, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                    Text("Ledgerly").ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted).padding(.top, 24)
                    Text("Know where your money goes.")
                        .font(.system(size: 40, weight: .bold))
                        .foregroundStyle(LedgerlyColor.ink)
                        .padding(.top, 8)
                        .accessibilityAddTraits(.isHeader)
                    Text("Log what you spend in seconds, keep a daily streak, and keep track of who owes who.")
                        .ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.inkMuted)
                        .padding(.top, 12)
                    Spacer()
                }
            case .aboutYou:
                step(footer: {
                    Button("Continue") { viewModel.onNext() }
                        .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                        .disabled(!state.canContinue)
                }) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            title("A bit about you", "Ledgerly keeps everything on this phone.")
                            FormField(label: "Your name") {
                                TextField("What should we call you?", text: Binding(get: { model.state.name }, set: { viewModel.onNameChange(name: $0) }))
                                    .textContentType(.name)
                                    .inputBox()
                            }
                            FormField(label: "Your currency") {
                                FlowLayout {
                                    ForEach(state.currencies, id: \.label) { option in
                                        ChipButton(title: option.code.code, selected: option.code == state.currency) {
                                            viewModel.onCurrencyChange(currency: option.code)
                                        }
                                    }
                                }
                                let label = state.currencies.first(where: { $0.code == state.currency })?.label ?? ""
                                Text("\(label). Every amount is logged in it.")
                                    .ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted).padding(.leading, 4)
                            }
                        }
                    }
                    .scrollDismissesKeyboard(.interactively)
                }
            case .track:
                step(footer: {
                    Button("Continue") { viewModel.onNext() }.buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                }) {
                    title("What do you want to track?", "Spending is always on. Add the rest now or later in Settings.")
                    VStack(spacing: LedgerlySpacing.space3) {
                        choice("Spending", "A daily log and a streak", symbol: "creditcard", on: true, enabled: false) {}
                        choice("Budgets", "A limit per category", symbol: "gauge.with.dots.needle.50percent", on: state.trackBudgets) {
                            viewModel.onTrackBudgetsChange(on: !state.trackBudgets)
                        }
                        choice("Debts", "What you owe and who owes you", symbol: "person.2", on: state.trackDebts) {
                            viewModel.onTrackDebtsChange(on: !state.trackDebts)
                        }
                    }
                    Spacer()
                }
            case .budgets:
                step(footer: {
                    Button("Save budgets") { budgets.viewModel.onSave() }
                        .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                        .disabled(!budgets.state.canSave)
                    Button("Skip for now") { viewModel.onNext() }.buttonStyle(PillButtonStyle(kind: .quiet, fill: true))
                }) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 0) {
                            title("Set your first budgets", "A monthly limit for the categories you want to watch. You can change them any time.")
                                .padding(.bottom, 20)
                            BudgetEditorContent(state: budgets.state, viewModel: budgets.viewModel)
                        }
                    }
                    .scrollDismissesKeyboard(.interactively)
                }
            case .reminder:
                step(footer: {
                    Button(state.reminderButton) {
                        Task {
                            let granted = await ReminderScheduler.requestPermission()
                            viewModel.onReminderAnswer(on: granted)
                        }
                    }
                    .buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                    Button("Not now") { viewModel.onReminderAnswer(on: false) }.buttonStyle(PillButtonStyle(kind: .quiet, fill: true))
                }) {
                    title("A nudge to keep your streak", "Once a day, only if you haven't logged anything yet. Turn it off any time in Settings.")
                    HStack(spacing: 12) {
                        Image(systemName: "bell.fill")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundStyle(LedgerlyColor.cobalt)
                            .frame(width: 44, height: 44)
                            .background(LedgerlyColor.cobaltSoft, in: Circle())
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Ledgerly").ledgerlyText(LedgerlyText.label).foregroundStyle(LedgerlyColor.inkMuted)
                            Text(state.reminderMessage).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.ink)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(16)
                    .background(LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(LedgerlyColor.line, lineWidth: 1))
                    .accessibilityElement(children: .combine)
                    .padding(.top, 8)
                    FormField(label: "Remind me at") {
                        FlowLayout {
                            ForEach(state.reminderOptions, id: \.minutes) { option in
                                ChipButton(title: option.label, selected: option.minutes == state.reminderMinutes) {
                                    viewModel.onReminderTimeChange(minutes: option.minutes)
                                }
                            }
                        }
                    }
                    .padding(.top, 24)
                    Spacer()
                }
            case .done:
                step(footer: {
                    Button("Go to Home") { viewModel.onFinish() }.buttonStyle(PillButtonStyle(kind: .primary, fill: true))
                }) {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "flame.fill")
                            .font(.system(size: 40))
                            .foregroundStyle(LedgerlyColor.caution)
                            .frame(width: 88, height: 88)
                            .background(LedgerlyColor.cautionSoft, in: Circle())
                        let first = state.name.trimmingCharacters(in: .whitespaces).split(separator: " ").first.map(String.init) ?? ""
                        Text(first.isEmpty ? "You're all set." : "You're all set, \(first).")
                            .ledgerlyText(LedgerlyText.title).foregroundStyle(LedgerlyColor.ink)
                            .multilineTextAlignment(.center)
                        Text(state.doneText)
                            .ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.inkMuted)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    Spacer()
                }
            }
        }
        .background(LedgerlyColor.canvas.ignoresSafeArea())
        .tint(LedgerlyColor.cobalt)
        .task { await model.observe() }
        .task { await budgets.observe() }
        .onChange(of: budgets.state.isSaved) { _, saved in
            if saved { viewModel.onBudgetsSaved() }
        }
    }

    private func progressBar(_ state: OnboardingUiState) -> some View {
        HStack(spacing: 16) {
            Button { model.viewModel.onBack() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(LedgerlyColor.ink)
                    .frame(width: 44, height: 44)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .glassControl(in: Circle())
            .accessibilityLabel("Back")

            HStack(spacing: 6) {
                ForEach(0..<Int(state.progressCount), id: \.self) { index in
                    Capsule()
                        .fill(index < Int(state.progress) ? LedgerlyColor.cobalt : LedgerlyColor.line)
                        .frame(height: 4)
                }
            }
            .accessibilityElement()
            .accessibilityLabel("Step \(state.progress) of \(state.progressCount)")

            Color.clear.frame(width: 44, height: 44)
        }
        .padding(.horizontal, LedgerlySpacing.space5)
        .padding(.vertical, 8)
    }

    private func title(_ text: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(text)
                .ledgerlyText(LedgerlyText.title).foregroundStyle(LedgerlyColor.ink)
                .accessibilityAddTraits(.isHeader)
            Text(body).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.inkMuted)
        }
        .padding(.top, 16)
        .padding(.bottom, 8)
    }

    private func step<Content: View, Footer: View>(
        @ViewBuilder footer: () -> Footer,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 0) { content() }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .padding(.horizontal, LedgerlySpacing.space5)
            VStack(spacing: 4) { footer() }
                .padding(.horizontal, LedgerlySpacing.space5)
                .padding(.vertical, 16)
        }
    }

    private func choice(_ title: String, _ sub: String, symbol: String, on: Bool, enabled: Bool = true, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: symbol)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(LedgerlyColor.cobalt)
                    .frame(width: 44, height: 44)
                    .background(LedgerlyColor.cobaltSoft, in: Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.ink)
                    Text(sub).ledgerlyText(LedgerlyText.caption).foregroundStyle(LedgerlyColor.inkMuted)
                }
                Spacer()
                Image(systemName: on ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22))
                    .foregroundStyle(on ? LedgerlyColor.cobalt : LedgerlyColor.line)
            }
            .padding(16)
            .background(LedgerlyColor.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(on ? LedgerlyColor.cobalt : LedgerlyColor.line, lineWidth: on ? 2 : 1))
            .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}
