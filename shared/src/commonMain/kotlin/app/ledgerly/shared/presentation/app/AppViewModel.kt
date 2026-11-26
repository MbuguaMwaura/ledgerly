package app.ledgerly.shared.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AppUiState(
    val isLoading: Boolean,
    /** False shows onboarding instead of the tabs. */
    val isOnboarded: Boolean,
    val theme: ThemeChoice,
    /** Off hides budget cards, the Budgets view and the Set budgets prompts. */
    val trackBudgets: Boolean,
    /** Off hides the Debts tab and debt cards. */
    val trackDebts: Boolean,
    /** For the Home greeting. Empty when the user gave none. */
    val firstName: String,
)

/** What the app shell needs from settings: onboarding or tabs, theme, and which features show. */
class AppViewModel(settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<AppUiState> = settings.observe().map {
        AppUiState(
            isLoading = false,
            isOnboarded = it.isOnboarded,
            theme = it.theme,
            trackBudgets = it.trackBudgets,
            trackDebts = it.trackDebts,
            firstName = it.name.trim().substringBefore(' '),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppUiState(isLoading = true, isOnboarded = false, theme = ThemeChoice.System, trackBudgets = true, trackDebts = true, firstName = ""),
    )
}
