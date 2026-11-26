package app.ledgerly.shared.presentation.adddebt

import app.ledgerly.shared.presentation.common.currency
import app.ledgerly.shared.domain.repository.SettingsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.repository.DebtRepository
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.domain.usecase.AddDebt
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.money.cleanAmountInput
import app.ledgerly.shared.money.parseAmount
import app.ledgerly.shared.presentation.common.DebtRowState
import app.ledgerly.shared.presentation.common.shortDate
import app.ledgerly.shared.presentation.common.toRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** The quick due-date choices on the New debt screen. */
enum class DueChoice { ThisFriday, InAWeek, MidNextMonth, NoDate }

data class DueChoiceState(
    val choice: DueChoice,
    /** "This Friday", "In a week", "Mid December", "No date". */
    val label: String,
    val date: LocalDate?,
)

data class AddDebtUiState(
    val currency: CurrencyCode,
    val direction: DebtDirection,
    val amountText: String,
    /** "Who owes you" or "Who you owe". */
    val whoLabel: String,
    /** People already in Debts for this direction, for one-tap picking. */
    val people: List<String>,
    val counterparty: String,
    val note: String,
    val dueChoices: List<DueChoiceState>,
    val due: DueChoice,
    /** "Due Fri 27 Nov." or "No due date." */
    val dueText: String,
    /** How the debt will look in Debts, once there is an amount and a name. */
    val preview: DebtRowState?,
    val canSave: Boolean,
    /** "Add KES 2,000" or "Enter an amount and a name". */
    val saveLabel: String,
    /** Set after a save, e.g. "Added Otieno." The UI shows it and closes the screen. */
    val savedMessage: String?,
)

class AddDebtViewModel(
    debts: DebtRepository,
    private val addDebt: AddDebt,
    settings: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {
    /** The user's currency, kept current by [uiState]. */
    private var currency = CurrencyCode.KES

    private data class Input(
        val direction: DebtDirection = DebtDirection.OwedToMe,
        val amountText: String = "",
        val counterparty: String = "",
        val note: String = "",
        val due: DueChoice = DueChoice.InAWeek,
        val isSaving: Boolean = false,
        val savedMessage: String? = null,
    )

    private val input = MutableStateFlow(Input())

    val uiState: StateFlow<AddDebtUiState> = combine(input, debts.observeAll(), settings.currency()) { current, all, code ->
        currency = code
        state(current, all)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state(Input(), emptyList()))

    fun onDirectionChange(direction: DebtDirection) = input.update { it.copy(direction = direction) }

    fun onAmountChange(text: String) = input.update { it.copy(amountText = cleanAmountInput(text, currency)) }

    fun onCounterpartyChange(name: String) = input.update { it.copy(counterparty = name.take(MAX_NAME_LENGTH)) }

    fun onNoteChange(note: String) = input.update { it.copy(note = note.take(MAX_NOTE_LENGTH)) }

    fun onDueChange(choice: DueChoice) = input.update { it.copy(due = choice) }

    fun onSave() {
        val current = uiState.value
        if (!current.canSave) return
        val amount = parseAmount(current.amountText, currency) ?: return
        val dueDate = current.dueChoices.first { it.choice == current.due }.date
        input.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val debt = addDebt(current.direction, current.counterparty, amount, dueDate, current.note)
                input.update { it.copy(isSaving = false, savedMessage = "Added ${debt.counterparty}.") }
            } catch (e: Throwable) {
                input.update { it.copy(isSaving = false) }
                throw e
            }
        }
    }

    private fun state(current: Input, all: List<Debt>): AddDebtUiState {
        val today = clock.today()
        val choices = dueChoices(today)
        val dueDate = choices.first { it.choice == current.due }.date
        val amount = parseAmount(current.amountText, currency) ?: Money.zero(currency)
        val name = current.counterparty.trim()
        val ready = amount.minorUnits > 0 && name.isNotEmpty()
        val preview = if (!ready) null else Debt(
            id = "preview",
            direction = current.direction,
            counterparty = name,
            principal = amount,
            dueDate = dueDate,
            note = current.note.trim().ifEmpty { null },
            createdAt = clock.now(),
            lastRemindedAt = null,
            payments = emptyList(),
        ).toRow(today, clock.timeZone())
        return AddDebtUiState(
            currency = currency,
            direction = current.direction,
            amountText = current.amountText,
            whoLabel = if (current.direction == DebtDirection.OwedToMe) "Who owes you" else "Who you owe",
            people = all.filter { it.direction == current.direction }
                .sortedByDescending { it.createdAt }
                .map { it.counterparty }
                .distinctBy { it.lowercase() }
                .take(MAX_PEOPLE),
            counterparty = current.counterparty,
            note = current.note,
            dueChoices = choices,
            due = current.due,
            dueText = dueDate?.let { "Due ${shortDate(it)}." } ?: "No due date.",
            preview = preview,
            canSave = ready && !current.isSaving && current.savedMessage == null,
            saveLabel = if (ready) "Add ${amount.parts().format(withFraction = false)}" else "Enter an amount and a name",
            savedMessage = current.savedMessage,
        )
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
        const val MAX_NOTE_LENGTH = 140
        const val MAX_PEOPLE = 6
    }
}

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

internal fun dueChoices(today: LocalDate): List<DueChoiceState> {
    val toFriday = (DayOfWeek.FRIDAY.ordinal - today.dayOfWeek.ordinal + 7) % 7
    val friday = today.plus(DatePeriod(days = if (toFriday == 0) 7 else toFriday))
    val nextMonth = today.plus(DatePeriod(months = 1))
    val midNextMonth = LocalDate(nextMonth.year, nextMonth.monthNumber, 15)
    return listOf(
        DueChoiceState(DueChoice.ThisFriday, if (toFriday == 0) "Next Friday" else "This Friday", friday),
        DueChoiceState(DueChoice.InAWeek, "In a week", today.plus(DatePeriod(days = 7))),
        DueChoiceState(DueChoice.MidNextMonth, "Mid ${MONTH_NAMES[midNextMonth.monthNumber - 1]}", midNextMonth),
        DueChoiceState(DueChoice.NoDate, "No date", null),
    )
}
