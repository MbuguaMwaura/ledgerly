import Shared
import SwiftUI

@main
struct LedgerlyApp: App {
    init() {
        LedgerlyShared.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
