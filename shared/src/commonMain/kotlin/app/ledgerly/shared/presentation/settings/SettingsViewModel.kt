package app.ledgerly.shared.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.domain.repository.BudgetRepository
import app.ledgerly.shared.domain.repository.LogRepository
import app.ledgerly.shared.domain.repository.SettingsRepository
import app.ledgerly.shared.domain.rules.calculateStreak
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.onboarding.CURRENCIES
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean,
    val name: String,
    /** "WK", for the profile tile. */
    val initials: String,
    /** "Logging since Nov 2026 · 5-day streak". */
    val profileLine: String,
    val theme: ThemeChoice,
    /** "Kenyan shilling (KES)". */
    val currencyText: String,
    /** "KES 45,000 a month across 5 categories" or "Not set". */
    val budgetsText: String,
    /** "1 left" or "None left". */
    val freezesText: String,
    val trackBudgets: Boolean,
    val trackDebts: Boolean,
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    budgets: BudgetRepository,
    logs: LogRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val month = BudgetMonth.of(clock.today())

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.observe(),
        budgets.observeMonth(month),
        logs.observeLoggedDays(),
    ) { current, monthBudgets, logged ->
        val streak = calculateStreak(logged, clock.today())
        val limits = monthBudgets.filter { it.limit.currency == current.currency }
        val total = limits.fold(Money.zero(current.currency)) { s, b -> s + b.limit }
        val since = current.startedOn?.let { "Logging since ${MONTHS[it.monthNumber - 1]} ${it.year}" }
        val streakText = when (streak.current) {
            0 -> null
            else -> "${streak.current}-day streak"
        }
        SettingsUiState(
            isLoading = false,
            name = current.name,
            initials = current.name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
                .joinToString("") { it.first().uppercase() },
            profileLine = listOfNotNull(since, streakText).joinToString(" · "),
            theme = current.theme,
            currencyText = CURRENCIES.firstOrNull { it.code == current.currency }
                ?.let { "${it.label} (${it.code.code})" } ?: current.currency.code,
            budgetsText = when (limits.size) {
                0 -> "Not set"
                1 -> "${total.parts().format(withFraction = false)} a month in 1 category"
                else -> "${total.parts().format(withFraction = false)} a month across ${limits.size} categories"
            },
            freezesText = if (streak.freezesHeld == 0) "None left" else "${streak.freezesHeld} left",
            trackBudgets = current.trackBudgets,
            trackDebts = current.trackDebts,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(true, "", "", "", ThemeChoice.System, "", "", "", trackBudgets = true, trackDebts = true),
    )

    fun onNameChange(name: String) {
        viewModelScope.launch { settings.update { it.copy(name = name.trim().take(MAX_NAME_LENGTH)) } }
    }

    fun onThemeChange(theme: ThemeChoice) {
        viewModelScope.launch { settings.update { it.copy(theme = theme) } }
    }

    fun onTrackBudgetsChange(on: Boolean) {
        viewModelScope.launch { settings.update { it.copy(trackBudgets = on) } }
    }

    fun onTrackDebtsChange(on: Boolean) {
        viewModelScope.launch { settings.update { it.copy(trackDebts = on) } }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 40
        val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}
