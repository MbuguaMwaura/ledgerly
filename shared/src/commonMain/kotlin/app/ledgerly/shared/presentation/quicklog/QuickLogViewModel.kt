package app.ledgerly.shared.presentation.quicklog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.Category
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.repository.CategoryRepository
import app.ledgerly.shared.domain.repository.LogRepository
import app.ledgerly.shared.domain.rules.calculateStreak
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.domain.usecase.LogEntry
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One key on the Quick log keypad. */
sealed interface KeypadKey {
    data class Digit(val value: Int) : KeypadKey {
        init {
            require(value in 0..9) { "digit must be 0..9, was $value" }
        }
    }

    data object Decimal : KeypadKey

    data object Backspace : KeypadKey
}

data class QuickLogUiState(
    val kind: EntryKind,
    /** What the user typed, e.g. "1,250.5". Empty shows the "0" placeholder. */
    val amountText: String,
    val amount: Money,
    val categories: List<Category>,
    val selectedCategoryId: String?,
    val note: String,
    val canSave: Boolean,
    val isSaving: Boolean,
    /** Set after a save, e.g. "Logged. That's 5 days in a row." The UI shows it, then calls [QuickLogViewModel.reset]. */
    val savedMessage: String?,
)

/** Amount → category → save, in under five seconds. */
class QuickLogViewModel(
    categories: CategoryRepository,
    private val logs: LogRepository,
    private val logEntry: LogEntry,
    private val clock: AppClock,
) : ViewModel() {
    private val currency = CurrencyCode.KES

    private data class Input(
        val kind: EntryKind = EntryKind.Expense,
        val digits: String = "",
        val categoryId: String? = null,
        val note: String = "",
        val isSaving: Boolean = false,
        val savedMessage: String? = null,
    )

    private val input = MutableStateFlow(Input())

    val uiState: StateFlow<QuickLogUiState> = combine(input, categories.observeAll()) { current, all ->
        state(current, all.filter { it.kind == current.kind })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state(Input(), emptyList()))

    fun onKindChange(kind: EntryKind) {
        input.update { if (it.kind == kind) it else it.copy(kind = kind, categoryId = null) }
    }

    fun onKey(key: KeypadKey) {
        input.update { it.copy(digits = applyKey(it.digits, key, currency.decimals)) }
    }

    fun onSelectCategory(id: String) {
        input.update { it.copy(categoryId = if (it.categoryId == id) null else id) }
    }

    fun onNoteChange(note: String) {
        input.update { it.copy(note = note.take(MAX_NOTE_LENGTH)) }
    }

    fun onSave() {
        val current = uiState.value
        if (!current.canSave) return
        val categoryId = current.selectedCategoryId ?: return
        input.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                logEntry(current.kind, current.amount, categoryId, current.note)
                val streak = calculateStreak(logs.observeLoggedDays().first(), clock.today())
                val message = when {
                    streak.current <= 1 -> "Logged. Your streak starts today."
                    else -> "Logged. That's ${streak.current} days in a row."
                }
                input.update { it.copy(isSaving = false, savedMessage = message) }
            } catch (e: Throwable) {
                input.update { it.copy(isSaving = false) }
                throw e
            }
        }
    }

    /** Clears the form for the next entry. Keeps the kind the user picked. */
    fun reset() {
        input.update { Input(kind = it.kind) }
    }

    private fun state(current: Input, categories: List<Category>): QuickLogUiState {
        val amount = parseMinorUnits(current.digits, currency)
        val selected = current.categoryId?.takeIf { id -> categories.any { it.id == id } }
        return QuickLogUiState(
            kind = current.kind,
            amountText = formatTyped(current.digits),
            amount = amount,
            categories = categories,
            selectedCategoryId = selected,
            note = current.note,
            canSave = amount.minorUnits > 0 && selected != null && !current.isSaving && current.savedMessage == null,
            isSaving = current.isSaving,
            savedMessage = current.savedMessage,
        )
    }

    private companion object {
        const val MAX_NOTE_LENGTH = 140
    }
}

/** Largest whole amount the keypad accepts: 9,999,999,999 in major units. */
private const val MAX_WHOLE_DIGITS = 10

internal fun applyKey(typed: String, key: KeypadKey, decimals: Int): String {
    val dot = typed.indexOf('.')
    return when (key) {
        KeypadKey.Backspace -> typed.dropLast(1)
        KeypadKey.Decimal -> when {
            decimals == 0 || dot >= 0 -> typed
            typed.isEmpty() -> "0."
            else -> "$typed."
        }
        is KeypadKey.Digit -> when {
            dot >= 0 && typed.length - dot - 1 >= decimals -> typed
            dot < 0 && typed.length >= MAX_WHOLE_DIGITS -> typed
            typed == "0" -> key.value.toString()
            else -> typed + key.value
        }
    }
}

internal fun parseMinorUnits(typed: String, currency: CurrencyCode): Money {
    if (typed.isEmpty()) return Money.zero(currency)
    val whole = typed.substringBefore('.').ifEmpty { "0" }.toLong()
    val fraction = typed.substringAfter('.', "").padEnd(currency.decimals, '0').take(currency.decimals)
    val minor = whole * currency.minorUnitFactor + (fraction.ifEmpty { "0" }.toLong())
    return Money(minor, currency)
}

/** "1250.5" → "1,250.5". Keeps what the user typed after the point. */
internal fun formatTyped(typed: String): String {
    if (typed.isEmpty()) return ""
    val whole = typed.substringBefore('.')
    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    return if ('.' in typed) "$grouped.${typed.substringAfter('.')}" else grouped
}
