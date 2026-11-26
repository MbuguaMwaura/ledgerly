import LocalAuthentication
import SwiftUI

/// Face ID, Touch ID, or the iPhone's passcode: whatever the phone has set up.
enum AppLock {
    /// Whether the iPhone has a passcode (and so possibly Face ID) that app lock can use.
    static var isAvailable: Bool {
        LAContext().canEvaluatePolicy(.deviceOwnerAuthentication, error: nil)
    }

    /// "Face ID", "Touch ID", or "your passcode", for labels.
    static var methodName: String {
        let context = LAContext()
        _ = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
        switch context.biometryType {
        case .faceID: return "Face ID"
        case .touchID: return "Touch ID"
        case .opticID: return "Optic ID"
        default: return "your passcode"
        }
    }

    /// Shows the system prompt. False when the user cancels or it fails.
    static func authenticate(reason: String) async -> Bool {
        do {
            return try await LAContext().evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
        } catch {
            return false
        }
    }
}

/// Covers the app while it's locked, and in the app switcher while app lock is on.
struct LockView: View {
    let onUnlock: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            Image(systemName: "lock.fill")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 64, height: 64)
                .background(LedgerlyColor.cobalt, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            VStack(spacing: 8) {
                Text("Ledgerly is locked")
                    .ledgerlyText(LedgerlyText.title).foregroundStyle(LedgerlyColor.ink)
                    .accessibilityAddTraits(.isHeader)
                Text("Unlock with \(AppLock.methodName).")
                    .ledgerlyText(LedgerlyText.body).foregroundStyle(LedgerlyColor.inkMuted)
                    .multilineTextAlignment(.center)
            }
            .padding(.top, 24)
            Spacer()
            Button("Unlock", action: onUnlock).buttonStyle(PillButtonStyle(kind: .primary, fill: true))
        }
        .padding(.horizontal, LedgerlySpacing.space5)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(LedgerlyColor.canvas.ignoresSafeArea())
    }
}
