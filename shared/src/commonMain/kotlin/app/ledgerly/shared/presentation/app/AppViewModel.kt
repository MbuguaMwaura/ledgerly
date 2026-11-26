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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
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
    /** App lock is on in Settings. Each app hides its window from the app switcher while it is. */
    val appLock: Boolean,
    /** Show the lock screen instead of the app until the user unlocks. */
    val isLocked: Boolean,
)

/**
 * What the app shell needs from settings: onboarding or tabs, theme, which features show, the
 * daily reminders to schedule, and whether the app is locked.
 */
class AppViewModel(
    settings: SettingsRepository,
    logs: LogRepository,
    clock: AppClock,
) : ViewModel() {
    /**
     * Starts locked, so a cold start with app lock on asks first. Settings loading with the lock
     * off clears it, so turning the lock on later doesn't lock the user out of the session they
     * just authenticated in.
     */
    private val locked = MutableStateFlow(true)

    val uiState: StateFlow<AppUiState> = combine(
        settings.observe().onEach { if (!it.appLock) locked.value = false },
        logs.observeLoggedDays(),
        locked,
    ) { current, logged, isLocked ->
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
            appLock = current.appLock,
            isLocked = current.appLock && current.isOnboarded && isLocked,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppUiState(
            isLoading = true, isOnboarded = false, theme = ThemeChoice.System, trackBudgets = true, trackDebts = true,
            firstName = "", reminderTimes = emptyList(), reminderMessage = REMINDER_MESSAGE,
            appLock = false, isLocked = false,
        ),
    )

    /** The app left the screen (not just a rotation or a system dialog). Locks it if app lock is on. */
    fun onAppHidden() {
        if (uiState.value.appLock) locked.value = true
    }

    /** The user passed Face ID, fingerprint or the passcode. */
    fun onUnlocked() {
        locked.value = false
    }
}
