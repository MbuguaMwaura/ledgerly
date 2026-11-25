import SwiftUI

/// The three tabs. Built with the iOS 26 SDK, the system tab bar renders as Liquid Glass on iOS 26
/// and as the standard bar on earlier versions. The floating + LogButton arrives in the iOS UI phase.
struct RootView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem { Label("Home", systemImage: "house") }
            PlaceholderView(title: "Spend")
                .tabItem { Label("Spend", systemImage: "creditcard") }
            PlaceholderView(title: "Debts")
                .tabItem { Label("Debts", systemImage: "person.2") }
        }
        .tint(LedgerlyColor.cobalt)
    }
}

private struct PlaceholderView: View {
    let title: String

    var body: some View {
        ZStack {
            LedgerlyColor.canvas.ignoresSafeArea()
            Text(title)
                .ledgerlyText(LedgerlyText.heading)
                .foregroundStyle(LedgerlyColor.inkMuted)
        }
    }
}
