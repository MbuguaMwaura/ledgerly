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

/// Picks the colour scheme from settings, then shows onboarding on first run and the tabs after.
struct AppShell: View {
    @State private var app = AppModel()

    var body: some View {
        let state = app.state
        Group {
            if state.isLoading {
                LedgerlyColor.canvas.ignoresSafeArea()
            } else if !state.isOnboarded {
                OnboardingView()
            } else {
                RootView(app: state)
            }
        }
        .preferredColorScheme(colorScheme(state.theme))
        .task { await app.observe() }
        .task(id: reminderKey(state)) {
            guard let times = reminderKey(state) else { return }
            await ReminderScheduler.schedule(times: times, message: state.reminderMessage)
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
