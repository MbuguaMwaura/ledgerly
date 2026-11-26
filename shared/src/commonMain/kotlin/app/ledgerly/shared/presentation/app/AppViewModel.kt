package app.ledgerly.shared.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.ThemeChoice
import app.ledgerly.shared.domain.repository.LogRepository
import app.ledgerly.shared.domain.repository.SettingsRepository
import app.ledgerly.shared.domain.rules.REMINDER_MESSAGE
import app.ledgerly.shared.domain.rules.upcomingReminders
import app.ledgerly.shared.domain.time.AppClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    /**
     * When the daily reminders should fire, as epoch milliseconds, soonest first. Empty when
     * reminders are off. Each app replaces its scheduled notifications with these whenever they change.
     */
    val reminderTimes: List<Long>,
    val reminderMessage: String,
)

/**
 * What the app shell needs from settings: onboarding or tabs, theme, which features show, and the
 * daily reminders to schedule.
 */
class AppViewModel(
    settings: SettingsRepository,
    logs: LogRepository,
    clock: AppClock,
) : ViewModel() {
    val uiState: StateFlow<AppUiState> = combine(settings.observe(), logs.observeLoggedDays()) { current, logged ->
        val today = clock.today()
        AppUiState(
            isLoading = false,
            isOnboarded = current.isOnboarded,
            theme = current.theme,
            trackBudgets = current.trackBudgets,
            trackDebts = current.trackDebts,
            firstName = current.name.trim().substringBefore(' '),
            reminderTimes = if (!current.reminderOn || !current.isOnboarded) emptyList() else upcomingReminders(
                minuteOfDay = current.reminderMinutes,
                now = clock.now(),
                timeZone = clock.timeZone(),
                loggedToday = today in logged,
            ).map { it.toEpochMilliseconds() },
            reminderMessage = REMINDER_MESSAGE,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppUiState(
            isLoading = true, isOnboarded = false, theme = ThemeChoice.System, trackBudgets = true, trackDebts = true,
            firstName = "", reminderTimes = emptyList(), reminderMessage = REMINDER_MESSAGE,
        ),
    )
}
