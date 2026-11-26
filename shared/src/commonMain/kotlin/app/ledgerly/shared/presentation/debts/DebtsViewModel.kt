package app.ledgerly.shared.presentation.debts

import app.ledgerly.shared.presentation.common.currency
import app.ledgerly.shared.domain.repository.SettingsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.Debt
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.repository.DebtRepository
import app.ledgerly.shared.domain.rules.DueStatus
import app.ledgerly.shared.domain.rules.ReminderTone
import app.ledgerly.shared.domain.rules.draftReminder
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.domain.usecase.MarkReminded
import app.ledgerly.shared.domain.usecase.RecordDebtPayment
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.common.DebtRowState
import app.ledgerly.shared.presentation.common.toRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebtsUiState(
    val isLoading: Boolean,
    /** The segmented control: "Owed to you" or "You owe". */
    val direction: DebtDirection,
    val totalOwedToMe: Money,
    val totalIOwe: Money,
    val overdue: List<DebtRowState>,
    /** Due today or within the next 3 days. */
    val dueSoon: List<DebtRowState>,
    /** Due later, or with no due date. */
    val later: List<DebtRowState>,
    val settled: List<DebtRowState>,
)

class DebtsViewModel(
    private val debts: DebtRepository,
    private val recordDebtPayment: RecordDebtPayment,
    private val markReminded: MarkReminded,
    settings: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val direction = MutableStateFlow(DebtDirection.OwedToMe)

    /** The debts behind the current [uiState], so drafts use the real record. */
    private var latest: List<Debt> = emptyList()

    val uiState: StateFlow<DebtsUiState> = combine(debts.observeAll(), direction, settings.currency()) { all, selected, currency ->
        latest = all
        val today = clock.today()
        val zero = Money.zero(currency)
        val rows = all.filter { it.principal.currency == currency }.map { it.toRow(today, clock.timeZone()) }
        fun total(d: DebtDirection) = rows.filter { it.direction == d }.fold(zero) { s, r -> s + r.remaining }
        val shown = rows.filter { it.direction == selected }
        DebtsUiState(
            isLoading = false,
            direction = selected,
            totalOwedToMe = total(DebtDirection.OwedToMe),
            totalIOwe = total(DebtDirection.IOwe),
            overdue = shown.filter { it.status is DueStatus.Overdue }
                .sortedByDescending { (it.status as DueStatus.Overdue).days },
            dueSoon = shown.filter { it.status is DueStatus.DueSoon },
            later = shown.filter { it.status is DueStatus.Upcoming || it.status == DueStatus.NoDueDate },
            settled = shown.filter { it.status == DueStatus.Settled },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DebtsUiState(true, DebtDirection.OwedToMe, Money.zero(CurrencyCode.KES), Money.zero(CurrencyCode.KES), emptyList(), emptyList(), emptyList(), emptyList()),
    )

    fun onSelectDirection(value: DebtDirection) {
        direction.value = value
    }

    fun onRecordPayment(debtId: String, amount: Money) {
        viewModelScope.launch { recordDebtPayment(debtId, amount) }
    }

    /**
     * The text for the share sheet. Ledgerly never sends it; the UI opens the share sheet and calls
     * [onReminderSent] once the user has sent it.
     */
    fun reminderDraft(debtId: String, tone: ReminderTone): String? {
        val debt = latest.firstOrNull { it.id == debtId && !it.isSettled } ?: return null
        return draftReminder(debt, tone, clock.today())
    }

    fun onReminderSent(debtId: String) {
        viewModelScope.launch { markReminded(debtId) }
    }
}
