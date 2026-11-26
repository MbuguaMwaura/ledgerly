package app.ledgerly.shared.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.repository.SettingsRepository
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.money.convertMinorUnits
import app.ledgerly.shared.presentation.onboarding.CURRENCIES
import app.ledgerly.shared.presentation.onboarding.CurrencyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangeCurrencyUiState(
    val isLoading: Boolean,
    /** The currency in use now. */
    val from: CurrencyCode,
    /** "Kenyan shilling (KES)". */
    val fromText: String,
    /** Every other currency to switch to. */
    val options: List<CurrencyOption>,
    val selected: CurrencyCode?,
    /** How many of the new currency one of the old one is worth, as typed. "1" keeps the numbers. */
    val rateText: String,
    /** "1 KES =" for the rate field's label. */
    val rateLabel: String,
    /** "KES 1,000 becomes UGX 1,000." Empty until a currency and a valid rate are chosen. */
    val example: String,
    /** "Switch to UGX" or "Choose a currency". */
    val saveLabel: String,
    val canSave: Boolean,
    /** True once the switch is saved. The UI closes and shows [doneMessage]. */
    val isDone: Boolean,
    /** "Switched to UGX." */
    val doneMessage: String,
)

/**
 * Switches the currency after setup. Every amount already logged moves to the new currency at the
 * rate given: 1 to fix a wrong choice and keep the numbers, or an exchange rate after a move.
 */
class ChangeCurrencyViewModel(
    private val settings: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {
    private data class Input(
        val selected: CurrencyCode? = null,
        val rateText: String = "1",
        val isSaving: Boolean = false,
        val isDone: Boolean = false,
        val doneMessage: String = "",
    )

    private val input = MutableStateFlow(Input())

    val uiState: StateFlow<ChangeCurrencyUiState> = combine(settings.observe(), input) { current, typed ->
        val from = current.currency
        val selected = typed.selected?.takeIf { it != from }
        val rate = parseRate(typed.rateText)
        val example = if (selected != null && rate != null) {
            val sample = Money.ofMajor(1_000, from)
            val converted = Money(convertMinorUnits(sample.minorUnits, from, selected, rate), selected)
            "${sample.parts().format(withFraction = false)} becomes ${converted.parts().format(withFraction = selected.decimals > 0 && converted.minorUnits % selected.minorUnitFactor != 0L)}."
        } else {
            ""
        }
        ChangeCurrencyUiState(
            isLoading = false,
            from = from,
            fromText = CURRENCIES.firstOrNull { it.code == from }?.let { "${it.label} (${it.code.code})" } ?: from.code,
            options = CURRENCIES.filter { it.code != from },
            selected = selected,
            rateText = typed.rateText,
            rateLabel = "1 ${from.code} =",
            example = example,
            saveLabel = selected?.let { "Switch to ${it.code}" } ?: "Choose a currency",
            canSave = selected != null && rate != null && !typed.isSaving && !typed.isDone,
            isDone = typed.isDone,
            doneMessage = typed.doneMessage,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ChangeCurrencyUiState(
            isLoading = true, from = CurrencyCode.KES, fromText = "", options = emptyList(), selected = null,
            rateText = "1", rateLabel = "", example = "", saveLabel = "", canSave = false, isDone = false, doneMessage = "",
        ),
    )

    fun onCurrencySelect(code: CurrencyCode) = input.update { it.copy(selected = code) }

    /** Keeps digits and one decimal point, up to six decimals. */
    fun onRateChange(text: String) {
        val digits = text.filter { it.isDigit() || it == '.' }
        val clean = digits.substringBefore('.').take(9) +
            if ('.' in digits) "." + digits.substringAfter('.').filter { it.isDigit() }.take(6) else ""
        input.update { it.copy(rateText = clean) }
    }

    fun onSave() {
        val state = uiState.value
        val to = state.selected ?: return
        val rate = parseRate(state.rateText) ?: return
        if (!state.canSave) return
        input.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                settings.changeCurrency(to, rate, clock.now())
                input.update { it.copy(isSaving = false, isDone = true, doneMessage = "Switched to ${to.code}.") }
            } catch (e: Throwable) {
                input.update { it.copy(isSaving = false) }
                throw e
            }
        }
    }

    private fun parseRate(text: String): Double? = text.toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }
}
