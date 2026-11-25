package app.ledgerly.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.ScopedViewModels
import app.ledgerly.android.ui.onboarding.OnboardingRoute
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.presentation.app.AppViewModel
import org.koin.androidx.compose.koinViewModel

/** Picks the theme from settings, then shows onboarding on first run and the tabs after. */
@Composable
fun LedgerlyApp(versionName: String, viewModel: AppViewModel = koinViewModel()) {
    val app by viewModel.uiState.collectAsStateWithLifecycle()
    val dark = when (app.theme) {
        ThemeChoice.System -> isSystemInDarkTheme()
        ThemeChoice.Light -> false
        ThemeChoice.Dark -> true
    }
    LedgerlyTheme(darkTheme = dark) {
        when {
            app.isLoading -> Box(Modifier.fillMaxSize().background(LedgerlyTheme.colors.canvas))
            !app.isOnboarded -> ScopedViewModels { OnboardingRoute() }
            else -> LedgerlyRoot(app = app, versionName = versionName)
        }
    }
}
