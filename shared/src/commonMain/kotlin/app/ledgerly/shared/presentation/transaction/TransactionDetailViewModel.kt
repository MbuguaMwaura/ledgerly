package app.ledgerly.shared.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ledgerly.shared.domain.model.Budget
import app.ledgerly.shared.domain.model.BudgetPeriod
import app.ledgerly.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import app.ledgerly.shared.domain.model.Category
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.model.Transaction
import app.ledgerly.shared.domain.repository.BudgetRepository
import app.ledgerly.shared.domain.repository.CategoryRepository
import app.ledgerly.shared.domain.repository.TransactionRepository
import app.ledgerly.shared.domain.rules.budgetStatus
import app.ledgerly.shared.domain.time.AppClock
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.money.cleanAmountInput
import app.ledgerly.shared.money.parseAmount
import app.ledgerly.shared.money.toInputText
import app.ledgerly.shared.presentation.common.BudgetRowState
import app.ledgerly.shared.presentation.common.dayLabel
import app.ledgerly.shared.presentation.common.toRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionDetailUiState(
    val isLoading: Boolean,
    /** The entry is gone, e.g. deleted elsewhere. The UI closes the screen. */
    val notFound: Boolean,
    val kind: EntryKind,
    /** "Expense" or "Income". */
    val kindLabel: String,
    /** The note if there is one, else the category name. */
    val title: String,
    val icon: String,
    /** Signed as saved: "− KES 650" or "+ KES 4,000". */
    val signedAmount: Money,
    /** "Today · 14:05". */
    val whenText: String,
    val amountText: String,
    val categories: List<Category>,
    val selectedCategoryId: String,
    val note: String,
    /** The saved category's budget this month, for "How it fits". Null for income or no budget. */
    val budget: BudgetRowState?,
    val hasChanges: Boolean,
    val canSave: Boolean,
    /** "Delete expense" or "Delete income". */
    val deleteLabel: String,
    /** True once the entry was saved or deleted. The UI closes the screen. */
    val isDone: Boolean,
)

/** Shows one logged entry and lets the user fix its amount, category or note, or delete it. */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModel(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val budgets: BudgetRepository,
    private val settings: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {
    private data class Draft(
        val amountText: String? = null,
        val categoryId: String? = null,
        val note: String? = null,
        val isSaving: Boolean = false,
        val isDeleting: Boolean = false,
        val isDone: Boolean = false,
    )

    private val entryId = MutableStateFlow<String?>(null)
    private val draft = MutableStateFlow(Draft())

    /** The saved entry behind the current state. */
    private var latest: Transaction? = null

    val uiState: StateFlow<TransactionDetailUiState> = entryId.filterNotNull()
        .flatMapLatest { id -> flow { emit(transactions.get(id)) } }
        .flatMapLatest { found -> if (found == null) flowOf(NOT_FOUND) else observe(found) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LOADING)

    /** Call once with the entry to show. */
    fun load(id: String) {
        if (entryId.value == id) return
        draft.value = Draft()
        entryId.value = id
    }

    fun onAmountChange(text: String) {
        val currency = latest?.amount?.currency ?: return
        draft.update { it.copy(amountText = cleanAmountInput(text, currency)) }
    }

    fun onSelectCategory(id: String) = draft.update { it.copy(categoryId = id) }

    fun onNoteChange(note: String) = draft.update { it.copy(note = note.take(MAX_NOTE_LENGTH)) }

    fun onSave() {
        val state = uiState.value
        val entry = latest ?: return
        if (!state.canSave) return
        val amount = parseAmount(state.amountText, entry.amount.currency) ?: return
        val updated = entry.copy(
            amount = amount,
            categoryId = state.selectedCategoryId,
            note = state.note.trim().ifEmpty { null },
        )
        draft.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                transactions.save(updated)
                draft.update { it.copy(isSaving = false, isDone = true) }
            } catch (e: Throwable) {
                draft.update { it.copy(isSaving = false) }
                throw e
            }
        }
    }

    fun onDelete() {
        val entry = latest ?: return
        draft.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            transactions.delete(entry.id)
            draft.update { it.copy(isDone = true) }
        }
    }

    /**
     * Follows the entry's budget month, so the state stays current after a save. Falls back to
     * the entry as first loaded if it leaves the month's list (it was deleted).
     */
    private fun observe(found: Transaction): Flow<TransactionDetailUiState> =
        settings.observe().map { it.monthStartDay }.distinctUntilChanged().flatMapLatest { startDay ->
            observe(found, BudgetPeriod.of(found.logicalDate, startDay))
        }

    private fun observe(found: Transaction, period: BudgetPeriod): Flow<TransactionDetailUiState> {
        return combine(
            transactions.observeBetween(period.firstDay, period.lastDay),
            categories.observeAll(),
            budgets.observeMonth(period.month),
            draft,
        ) { entries, all, monthBudgets, current ->
            val entry = entries.firstOrNull { it.id == found.id }
            if (entry == null && !current.isDeleting && !current.isDone) return@combine NOT_FOUND
            val saved = entry ?: latest ?: found
            latest = saved
            state(saved, entries, all, monthBudgets, current)
        }
    }

    private fun state(
        entry: Transaction,
        entries: List<Transaction>,
        all: List<Category>,
        monthBudgets: List<Budget>,
        current: Draft,
    ): TransactionDetailUiState {
        val byId = all.associateBy { it.id }
        val row = entry.toRow(byId, clock.timeZone())
        val amountText = current.amountText ?: entry.amount.toInputText()
        val categoryId = current.categoryId ?: entry.categoryId
        val note = current.note ?: entry.note.orEmpty()
        val amount = parseAmount(amountText, entry.amount.currency)
        val changed = amount != entry.amount || categoryId != entry.categoryId ||
            note.trim().ifEmpty { null } != entry.note
        val budget = if (entry.kind != EntryKind.Expense) null else monthBudgets
            .firstOrNull { it.categoryId == entry.categoryId && it.limit.currency == entry.amount.currency }
            ?.let { b ->
                val spent = entries
                    .filter { it.kind == EntryKind.Expense && it.categoryId == b.categoryId && it.amount.currency == b.limit.currency }
                    .fold(Money.zero(b.limit.currency)) { s, t -> s + t.amount }
                budgetStatus(b, spent).toRow(byId)
            }
        val isExpense = entry.kind == EntryKind.Expense
        return TransactionDetailUiState(
            isLoading = false,
            notFound = false,
            kind = entry.kind,
            kindLabel = if (isExpense) "Expense" else "Income",
            title = row.title,
            icon = row.icon,
            signedAmount = row.signedAmount,
            whenText = "${dayLabel(entry.logicalDate, clock.today())} · ${row.time}",
            amountText = amountText,
            categories = all.filter { it.kind == entry.kind },
            selectedCategoryId = categoryId,
            note = note,
            budget = budget,
            hasChanges = changed,
            canSave = changed && amount != null && amount.minorUnits > 0 && !current.isSaving && !current.isDeleting && !current.isDone,
            deleteLabel = if (isExpense) "Delete expense" else "Delete income",
            isDone = current.isDone,
        )
    }

    private companion object {
        const val MAX_NOTE_LENGTH = 140

        val LOADING = TransactionDetailUiState(
            isLoading = true, notFound = false, kind = EntryKind.Expense, kindLabel = "", title = "", icon = "",
            signedAmount = Money.zero(CurrencyCode.KES), whenText = "", amountText = "",
            categories = emptyList(), selectedCategoryId = "", note = "", budget = null,
            hasChanges = false, canSave = false, deleteLabel = "", isDone = false,
        )
        val NOT_FOUND = LOADING.copy(isLoading = false, notFound = true)
    }
}
