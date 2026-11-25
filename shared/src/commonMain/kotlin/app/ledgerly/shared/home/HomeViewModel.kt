package app.ledgerly.shared.home

import androidx.lifecycle.ViewModel
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State for the Home tab. For now it only proves the wiring from the shared module to both UIs;
 * budgets, the streak and debts arrive with the shared domain in the next phases.
 */
data class HomeUiState(
    val leftToSpend: Money,
    val streakDays: Int,
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        HomeUiState(
            leftToSpend = Money.zero(CurrencyCode.KES),
            streakDays = 0,
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
