import Shared
import SwiftUI

@main
struct LedgerlyApp: App {
    init() {
        LedgerlyShared.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            AppShell()
        }
    }
}

/// Picks the colour scheme from settings, then shows onboarding on first run and the tabs after,
/// with the lock screen on top while app lock is on and the app is locked.
struct AppShell: View {
    @State private var app = AppModel()
    @State private var prompted = false
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        let state = app.state
        Group {
            if state.isLoading {
                LedgerlyColor.canvas.ignoresSafeArea()
            } else if !state.isOnboarded {
                OnboardingView()
            } else {
                RootView(app: state, onToggleAmounts: { app.viewModel.onHideAmountsChange(hidden: !state.hideAmounts) })
                    .accessibilityHidden(state.isLocked)
            }
        }
        .overlay {
            // Also covers the app switcher's snapshot while app lock is on.
            if state.isLocked || (state.appLock && state.isOnboarded && scenePhase != .active) {
                LockView(onUnlock: { Task { await unlock() } })
            }
        }
        .preferredColorScheme(colorScheme(state.theme))
        .task { await app.observe() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background { app.viewModel.onAppHidden() }
        }
        .onChange(of: state.hideAmounts, initial: true) { _, hidden in
            AmountPrivacy.shared.hidden = hidden
        }
        .onChange(of: state.isLocked) { _, locked in
            if !locked { prompted = false }
        }
        // Asks once each time the app is locked, as soon as it's on screen.
        .task(id: "\(state.isLocked)-\(scenePhase == .active)") {
            guard state.isLocked, scenePhase == .active, !prompted else { return }
            prompted = true
            await unlock()
        }
        .task(id: reminderKey(state)) {
            guard let times = reminderKey(state) else { return }
            await ReminderScheduler.schedule(times: times, message: state.reminderMessage)
        }
    }

    private func unlock() async {
        if await AppLock.authenticate(reason: "Unlock Ledgerly") {
            app.viewModel.onUnlocked()
        }
    }

    /// The reminder times to schedule, or nil while settings are still loading.
    private func reminderKey(_ state: AppUiState) -> [Int64]? {
        state.isLoading ? nil : state.reminderTimes.map { $0.int64Value }
    }

    private func colorScheme(_ theme: ThemeChoice) -> ColorScheme? {
        switch theme {
        case .light: .light
        case .dark: .dark
        default: nil
        }
    }
}
