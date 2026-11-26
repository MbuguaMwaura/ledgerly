package app.ledgerly.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.lock.AppLockEffects
import app.ledgerly.android.lock.PromptWhenLocked
import app.ledgerly.android.lock.rememberAuthenticator
import app.ledgerly.android.reminders.ReminderSync
import app.ledgerly.android.ui.lock.LockScreen
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.ledgerly.android.ui.components.AmountPrivacy
import app.ledgerly.android.ui.components.ScopedViewModels
import androidx.compose.runtime.SideEffect
import app.ledgerly.android.ui.onboarding.OnboardingRoute
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.presentation.app.AppViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Picks the theme from settings, then shows onboarding on first run and the tabs after, with the
 * lock screen on top while app lock is on and the app is locked.
 */
@Composable
fun LedgerlyApp(versionName: String, viewModel: AppViewModel = koinViewModel()) {
    val app by viewModel.uiState.collectAsStateWithLifecycle()
    val dark = when (app.theme) {
        ThemeChoice.System -> isSystemInDarkTheme()
        ThemeChoice.Light -> false
        ThemeChoice.Dark -> true
    }
    if (!app.isLoading) ReminderSync(app.reminderTimes, app.reminderMessage)
    AppLockEffects(app.appLock, viewModel::onAppHidden)
    SideEffect { AmountPrivacy.hidden = app.hideAmounts }
    val authenticate = rememberAuthenticator()
    val unlock = { authenticate("Unlock Ledgerly") { ok -> if (ok) viewModel.onUnlocked() } }
    PromptWhenLocked(app.isLocked, unlock)
    LedgerlyTheme(darkTheme = dark) {
        when {
            app.isLoading -> Box(Modifier.fillMaxSize().background(LedgerlyTheme.colors.canvas))
            !app.isOnboarded -> ScopedViewModels { OnboardingRoute() }
            else -> Box(Modifier.fillMaxSize()) {
                // Kept underneath while locked so the user comes back to where they were, but
                // hidden from screen readers.
                Box(if (app.isLocked) Modifier.clearAndSetSemantics {} else Modifier) {
                    LedgerlyRoot(
                        app = app,
                        versionName = versionName,
                        onToggleAmounts = { viewModel.onHideAmountsChange(!app.hideAmounts) },
                    )
                }
                if (app.isLocked) {
                    LockScreen(onUnlock = unlock)
                }
            }
        }
    }
}
