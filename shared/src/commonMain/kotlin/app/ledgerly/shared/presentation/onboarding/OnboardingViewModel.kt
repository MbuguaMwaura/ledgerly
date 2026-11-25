package app.ledgerly.shared.presentation.onboarding

import app.ledgerly.shared.domain.rules.reminderTimeLabel
import app.ledgerly.shared.domain.rules.REMINDER_MESSAGE
import app.ledgerly.shared.domain.rules.REMINDER_OPTIONS
import app.ledgerly.shared.domain.rules.ReminderOption
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.repository.SettingsRepository
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.money.CurrencyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Welcome → about you → what to track → first budgets (if tracked) → reminder → done. */
enum class OnboardingStep { Welcome, AboutYou, Track, Budgets, Reminder, Done }

data class CurrencyOption(val code: CurrencyCode, val label: String)

data class OnboardingUiState(
    val step: OnboardingStep,
    /** 1-based position among the steps with a progress bar, 0 on Welcome and Done. */
    val progress: Int,
    val progressCount: Int,
    val name: String,
    val currency: CurrencyCode,
    val currencies: List<CurrencyOption>,
    val trackBudgets: Boolean,
    val trackDebts: Boolean,
    val reminderMinutes: Int,
    val reminderOptions: List<ReminderOption>,
    /** "Remind me at 20:00". */
    val reminderButton: String,
    val reminderMessage: String,
    val canContinue: Boolean,
    /** For the Done step: "Your budgets are set." or "You can set budgets any time in Settings." */
    val doneText: String,
    /** True once settings are saved. The UI shows the tabs. */
    val isFinished: Boolean,
)

class OnboardingViewModel(
    private val settings: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {
    private data class Input(
        val step: OnboardingStep = OnboardingStep.Welcome,
        val name: String = "",
        val currency: CurrencyCode = CurrencyCode.KES,
        val trackBudgets: Boolean = true,
        val trackDebts: Boolean = true,
        val budgetsSaved: Boolean = false,
        val reminderOn: Boolean = false,
        val reminderMinutes: Int = 20 * 60,
        val isFinished: Boolean = false,
    )

    private val input = MutableStateFlow(Input())

    val uiState: StateFlow<OnboardingUiState> = input.map(::state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state(Input()))

    fun onNameChange(name: String) = input.update { it.copy(name = name.take(MAX_NAME_LENGTH)) }

    fun onCurrencyChange(currency: CurrencyCode) = input.update { it.copy(currency = currency) }

    fun onTrackBudgetsChange(on: Boolean) = input.update { it.copy(trackBudgets = on) }

    fun onTrackDebtsChange(on: Boolean) = input.update { it.copy(trackDebts = on) }

    /**
     * Moves to the next step. Leaving About you saves the name and currency, so the budgets step
     * already uses the chosen currency.
     */
    fun onNext() {
        val current = input.value
        if (!state(current).canContinue) return
        when (current.step) {
            OnboardingStep.AboutYou -> viewModelScope.launch {
                settings.update { it.copy(name = current.name.trim(), currency = current.currency) }
            }
            OnboardingStep.Track -> viewModelScope.launch {
                settings.update { it.copy(trackBudgets = current.trackBudgets, trackDebts = current.trackDebts) }
            }
            else -> Unit
        }
        input.update { it.copy(step = next(it)) }
    }

    /** The budgets step calls this after its own save. */
    fun onBudgetsSaved() {
        input.update { it.copy(budgetsSaved = true, step = OnboardingStep.Reminder) }
    }

    fun onReminderTimeChange(minutes: Int) {
        if (minutes in 0 until 24 * 60) input.update { it.copy(reminderMinutes = minutes) }
    }

    /**
     * The reminder step's answer. Each app asks for notification permission before calling this
     * with true, and passes false if the user declines there.
     */
    fun onReminderAnswer(on: Boolean) {
        input.update { it.copy(reminderOn = on, step = OnboardingStep.Done) }
    }

    fun onBack() = input.update { it.copy(step = previous(it)) }

    fun onFinish() {
        val current = input.value
        viewModelScope.launch {
            settings.update {
                it.copy(
                    name = current.name.trim(),
                    currency = current.currency,
                    trackBudgets = current.trackBudgets,
                    trackDebts = current.trackDebts,
                    reminderOn = current.reminderOn,
                    reminderMinutes = current.reminderMinutes,
                    startedOn = clock.today(),
                )
            }
            input.update { it.copy(isFinished = true) }
        }
    }

    private fun next(current: Input): OnboardingStep = when (current.step) {
        OnboardingStep.Welcome -> OnboardingStep.AboutYou
        OnboardingStep.AboutYou -> OnboardingStep.Track
        OnboardingStep.Track -> if (current.trackBudgets) OnboardingStep.Budgets else OnboardingStep.Reminder
        OnboardingStep.Budgets -> OnboardingStep.Reminder
        OnboardingStep.Reminder, OnboardingStep.Done -> OnboardingStep.Done
    }

    private fun previous(current: Input): OnboardingStep = when (current.step) {
        OnboardingStep.Welcome, OnboardingStep.AboutYou -> OnboardingStep.Welcome
        OnboardingStep.Track -> OnboardingStep.AboutYou
        OnboardingStep.Budgets -> OnboardingStep.Track
        OnboardingStep.Reminder -> if (current.trackBudgets) OnboardingStep.Budgets else OnboardingStep.Track
        OnboardingStep.Done -> OnboardingStep.Reminder
    }

    private fun state(current: Input): OnboardingUiState {
        val flow = listOfNotNull(
            OnboardingStep.AboutYou,
            OnboardingStep.Track,
            OnboardingStep.Budgets.takeIf { current.trackBudgets },
            OnboardingStep.Reminder,
        )
        return OnboardingUiState(
            step = current.step,
            progress = flow.indexOf(current.step) + 1,
            progressCount = flow.size,
            name = current.name,
            currency = current.currency,
            currencies = CURRENCIES,
            trackBudgets = current.trackBudgets,
            trackDebts = current.trackDebts,
            reminderMinutes = current.reminderMinutes,
            reminderOptions = REMINDER_OPTIONS,
            reminderButton = "Remind me at ${reminderTimeLabel(current.reminderMinutes)}",
            reminderMessage = REMINDER_MESSAGE,
            canContinue = current.step != OnboardingStep.AboutYou || current.name.isNotBlank(),
            doneText = when {
                current.budgetsSaved -> "Your budgets are set. Home shows what's left each day."
                current.trackBudgets -> "You can set budgets any time from Home or Settings."
                else -> "Log what you spend with the + button. One entry a day keeps your streak going."
            },
            isFinished = current.isFinished,
        )
    }

    private companion object {
        const val MAX_NAME_LENGTH = 40
    }
}

val CURRENCIES: List<CurrencyOption> = listOf(
    CurrencyOption(CurrencyCode.KES, "Kenyan shilling"),
    CurrencyOption(CurrencyCode.UGX, "Ugandan shilling"),
    CurrencyOption(CurrencyCode.TZS, "Tanzanian shilling"),
    CurrencyOption(CurrencyCode.USD, "US dollar"),
    CurrencyOption(CurrencyCode.EUR, "Euro"),
    CurrencyOption(CurrencyCode.GBP, "British pound"),
)
