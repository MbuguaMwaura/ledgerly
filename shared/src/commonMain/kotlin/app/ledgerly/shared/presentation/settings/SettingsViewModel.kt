package app.ledgerly.shared.presentation.settings

import app.ledgerly.shared.domain.rules.reminderTimeLabel
import app.ledgerly.shared.domain.rules.REMINDER_OPTIONS
import app.ledgerly.shared.domain.rules.ReminderOption
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
import app.ledgerly.shared.money.hidden
import app.ledgerly.shared.presentation.onboarding.CURRENCIES
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import app.ledgerly.shared.presentation.common.period
import app.ledgerly.shared.presentation.common.periodLabel
import app.ledgerly.shared.presentation.common.ordinal
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetPeriod
import app.ledgerly.shared.domain.model.MONTH_START_DAYS
import app.ledgerly.shared.domain.model.UserSettings
import kotlinx.datetime.LocalDate
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
    val reminderOn: Boolean,
    val reminderMinutes: Int,
    /** "Every day at 20:00 if nothing is logged yet" or "Off". */
    val reminderText: String,
    val reminderOptions: List<ReminderOption>,
    val appLock: Boolean,
    val hideAmounts: Boolean,
    /** 1 to 28. */
    val monthStartDay: Int,
    /** "1st, a calendar month" or "25th, so this month is 25 Oct – 24 Nov". */
    val monthStartText: String,
    /** Every day a budget month can start on, with its label: "1st", "2nd"... */
    val monthStartOptions: List<MonthStartOption>,
)

data class MonthStartOption(val day: Int, val label: String)

private val MONTH_START_OPTIONS = MONTH_START_DAYS.map { MonthStartOption(it, ordinal(it)) }

class SettingsViewModel(
    private val settings: SettingsRepository,
    budgets: BudgetRepository,
    logs: LogRepository,
    private val clock: AppClock,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SettingsUiState> = settings.period(clock).flatMapLatest { period ->
        combine(
            settings.observe(),
            budgets.observeMonth(period.month),
            logs.observeLoggedDays(),
        ) { current, monthBudgets, logged -> state(period, current, monthBudgets, logged) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            true, "", "", "", ThemeChoice.System, "", "", "", trackBudgets = true, trackDebts = true,
            reminderOn = false, reminderMinutes = 20 * 60, reminderText = "", reminderOptions = REMINDER_OPTIONS,
            appLock = false,
            hideAmounts = false,
            monthStartDay = 1, monthStartText = "", monthStartOptions = MONTH_START_OPTIONS,
        ),
    )

    private fun state(period: BudgetPeriod, current: UserSettings, monthBudgets: List<Budget>, logged: Set<LocalDate>): SettingsUiState {
        val streak = calculateStreak(logged, clock.today())
        val limits = monthBudgets.filter { it.limit.currency == current.currency }
        val total = limits.fold(Money.zero(current.currency)) { s, b -> s + b.limit }
        val totalText = total.parts().let { if (current.hideAmounts) it.hidden() else it }.format(withFraction = false)
        val since = current.startedOn?.let { "Logging since ${MONTHS[it.monthNumber - 1]} ${it.year}" }
        val streakText = when (streak.current) {
            0 -> null
            else -> "${streak.current}-day streak"
        }
        return SettingsUiState(
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
                1 -> "$totalText a month in 1 category"
                else -> "$totalText a month across ${limits.size} categories"
            },
            freezesText = if (streak.freezesHeld == 0) "None left" else "${streak.freezesHeld} left",
            trackBudgets = current.trackBudgets,
            trackDebts = current.trackDebts,
            reminderOn = current.reminderOn,
            reminderMinutes = current.reminderMinutes,
            reminderText = if (current.reminderOn) "Every day at ${reminderTimeLabel(current.reminderMinutes)} if nothing is logged yet" else "Off",
            reminderOptions = optionsWith(current.reminderMinutes),
            appLock = current.appLock,
            hideAmounts = current.hideAmounts,
            monthStartDay = current.monthStartDay,
            monthStartText = if (current.monthStartDay == 1) "1st, a calendar month"
            else "${ordinal(current.monthStartDay)}, so this month is ${periodLabel(period)}",
            monthStartOptions = MONTH_START_OPTIONS,
        )
    }

    fun onMonthStartDayChange(day: Int) {
        if (day !in MONTH_START_DAYS) return
        viewModelScope.launch { settings.update { it.copy(monthStartDay = day) } }
    }

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

    /** Turning this on is only half of it: each app asks for notification permission first. */
    fun onReminderChange(on: Boolean) {
        viewModelScope.launch { settings.update { it.copy(reminderOn = on) } }
    }

    fun onHideAmountsChange(hidden: Boolean) {
        viewModelScope.launch { settings.update { it.copy(hideAmounts = hidden) } }
    }

    /** Each app checks Face ID, fingerprint or the passcode before turning this on. */
    fun onAppLockChange(on: Boolean) {
        viewModelScope.launch { settings.update { it.copy(appLock = on) } }
    }

    fun onReminderTimeChange(minutes: Int) {
        if (minutes !in 0 until 24 * 60) return
        viewModelScope.launch { settings.update { it.copy(reminderMinutes = minutes) } }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 40
        val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}

/** The usual choices, plus the saved time if it isn't one of them. */
private fun optionsWith(minutes: Int): List<ReminderOption> =
    if (REMINDER_OPTIONS.any { it.minutes == minutes }) REMINDER_OPTIONS
    else (REMINDER_OPTIONS + ReminderOption(minutes, reminderTimeLabel(minutes))).sortedBy { it.minutes }
