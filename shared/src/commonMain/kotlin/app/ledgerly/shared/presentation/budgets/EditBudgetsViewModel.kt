package app.ledgerly.shared.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetMonth
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.repository.BudgetRepository
import app.ledgerly.shared.domain.repository.CategoryRepository
import app.ledgerly.shared.domain.repository.TransactionRepository
import app.ledgerly.shared.domain.rules.BudgetTone
import app.ledgerly.shared.domain.rules.budgetStatus
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.money.cleanAmountInput
import app.ledgerly.shared.money.parseAmount
import app.ledgerly.shared.money.toInputText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetEditRowState(
    val categoryId: String,
    val categoryName: String,
    val icon: String,
    /** The limit as typed, e.g. "12000". Empty means no budget. */
    val limitText: String,
    val spent: Money,
    /** "Spent KES 8,500 · KES 3,500 left", or "Spent KES 8,500 · no budget". */
    val note: String,
    /** Caution at 80% of the new limit, Over past it. OnTrack when there is no limit. */
    val tone: BudgetTone,
    val canDecrease: Boolean,
)

data class EditBudgetsUiState(
    val isLoading: Boolean,
    val rows: List<BudgetEditRowState>,
    /** The sum of every limit as typed. */
    val total: Money,
    /** "KES 45,000 a month across 5 categories." */
    val totalText: String,
    val hasChanges: Boolean,
    val canSave: Boolean,
    /** True once the save finished. The UI closes the screen. */
    val isSaved: Boolean,
)

/**
 * Edits this month's limit per expense category. A limit carries over to later months until it is
 * changed, and clearing one removes the budget from this month on.
 */
class EditBudgetsViewModel(
    categories: CategoryRepository,
    private val budgets: BudgetRepository,
    transactions: TransactionRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val currency = CurrencyCode.KES
    private val month = BudgetMonth.of(clock.today())

    private data class Input(
        /** Edited limits by category id. Categories not in here keep their saved limit. */
        val edits: Map<String, String> = emptyMap(),
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
    )

    private val input = MutableStateFlow(Input())

    /** The saved limits behind the current state, as typed text. */
    private var saved: Map<String, String> = emptyMap()

    val uiState: StateFlow<EditBudgetsUiState> = combine(
        categories.observeAll(),
        budgets.observeMonth(month),
        transactions.observeBetween(month.firstDay, month.lastDay),
        input,
    ) { all, monthBudgets, entries, current ->
        val zero = Money.zero(currency)
        saved = monthBudgets.filter { it.limit.currency == currency }.associate { it.categoryId to it.limit.toInputText() }
        val spentByCategory = entries.filter { it.kind == EntryKind.Expense && it.amount.currency == currency }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.fold(zero) { s, t -> s + t.amount } }
        val rows = all.filter { it.kind == EntryKind.Expense }.map { category ->
            val text = current.edits[category.id] ?: saved[category.id].orEmpty()
            val limit = parseAmount(text, currency) ?: zero
            val spent = spentByCategory[category.id] ?: zero
            val status = budgetStatus(Budget(category.id, month, limit), spent)
            val spentLabel = "Spent ${spent.label()}"
            BudgetEditRowState(
                categoryId = category.id,
                categoryName = category.name,
                icon = category.icon,
                limitText = text,
                spent = spent,
                note = when {
                    limit.minorUnits == 0L -> "$spentLabel · no budget"
                    status.overBy.minorUnits > 0 -> "$spentLabel · ${status.overBy.label()} over"
                    else -> "$spentLabel · ${status.left.label()} left"
                },
                tone = if (limit.minorUnits == 0L) BudgetTone.OnTrack else status.tone,
                canDecrease = limit.minorUnits > 0,
            )
        }
        val limits = rows.map { parseAmount(it.limitText, currency) ?: zero }
        val total = limits.fold(zero) { s, m -> s + m }
        val count = limits.count { it.minorUnits > 0 }
        val changed = changedLimits(current.edits).isNotEmpty()
        EditBudgetsUiState(
            isLoading = false,
            rows = rows,
            total = total,
            totalText = when (count) {
                0 -> "No budgets yet. Set a limit for the categories you want to watch."
                1 -> "${total.label()} a month in 1 category."
                else -> "${total.label()} a month across $count categories."
            },
            hasChanges = changed,
            canSave = changed && !current.isSaving && !current.isSaved,
            isSaved = current.isSaved,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EditBudgetsUiState(true, emptyList(), Money.zero(currency), "", hasChanges = false, canSave = false, isSaved = false),
    )

    fun onLimitChange(categoryId: String, text: String) {
        val clean = cleanAmountInput(text, currency).substringBefore('.').take(MAX_DIGITS)
        input.update { it.copy(edits = it.edits + (categoryId to clean)) }
    }

    /** The − and + buttons: move a limit by KES 500, never below zero. */
    fun onStep(categoryId: String, up: Boolean) {
        val current = uiState.value.rows.firstOrNull { it.categoryId == categoryId } ?: return
        val limit = parseAmount(current.limitText, currency) ?: Money.zero(currency)
        val step = Money.ofMajor(STEP_MAJOR, currency)
        val next = if (up) limit + step else limit - step
        val text = if (next.minorUnits <= 0) "" else next.toInputText().take(MAX_DIGITS)
        input.update { it.copy(edits = it.edits + (categoryId to text)) }
    }

    fun onSave() {
        if (!uiState.value.canSave) return
        val changes = changedLimits(input.value.edits)
        input.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val now = clock.now()
                changes.forEach { (categoryId, limit) -> budgets.save(Budget(categoryId, month, limit), now) }
                input.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Throwable) {
                input.update { it.copy(isSaving = false) }
                throw e
            }
        }
    }

    private fun changedLimits(edits: Map<String, String>): Map<String, Money> {
        val zero = Money.zero(currency)
        return edits.mapNotNull { (id, text) ->
            val next = parseAmount(text, currency) ?: zero
            val before = parseAmount(saved[id].orEmpty(), currency) ?: zero
            if (next == before) null else id to next
        }.toMap()
    }

    private fun Money.label() = parts().format(withFraction = false)

    private companion object {
        const val STEP_MAJOR = 500L
        const val MAX_DIGITS = 7
    }
}
