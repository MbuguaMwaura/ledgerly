package app.ledgerly.android.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.label
import app.ledgerly.android.ui.components.Amount
import app.ledgerly.android.ui.components.BudgetRow
import app.ledgerly.android.ui.components.ButtonStyle
import app.ledgerly.android.ui.components.Chip
import app.ledgerly.android.ui.components.Field
import app.ledgerly.android.ui.components.GroupTitle
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyCard
import app.ledgerly.android.ui.components.LedgerlyIcon
import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.components.LedgerlyTextField
import app.ledgerly.android.ui.components.Page
import app.ledgerly.android.ui.components.RoundIconButton
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.presentation.transaction.TransactionDetailUiState
import app.ledgerly.shared.presentation.transaction.TransactionDetailViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionDetailRoute(
    entryId: String,
    onDone: (message: String?) -> Unit,
    onClose: () -> Unit,
    viewModel: TransactionDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deleted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(entryId) { viewModel.load(entryId) }
    LaunchedEffect(state.isDone, state.notFound) {
        when {
            state.isDone -> onDone(if (deleted) "${state.kindLabel} deleted." else "Changes saved.")
            state.notFound -> onClose()
        }
    }
    TransactionDetailScreen(
        state = state,
        onAmountChange = viewModel::onAmountChange,
        onSelectCategory = viewModel::onSelectCategory,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::onSave,
        onDelete = {
            deleted = true
            viewModel.onDelete()
        },
        onClose = onClose,
    )
}

/** One entry: what it was, when, and fields to fix the amount, category or note. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    onAmountChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LedgerlyTheme.colors
    var confirming by rememberSaveable { mutableStateOf(false) }
    Page(
        title = state.kindLabel,
        onClose = onClose,
        closeLabel = "Back",
        back = true,
        action = {
            if (!state.isLoading) {
                RoundIconButton({ confirming = true }, state.deleteLabel, size = 44.dp) { LedgerlyIcon(LedgerlyIcons.Trash, size = 20.dp) }
            }
        },
    ) {
        if (state.isLoading) return@Page
        item("hero") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(state.title, style = LedgerlyTextStyles.body, color = colors.ink)
                Amount(state.signedAmount, style = LedgerlyTextStyles.amountHero)
                Text(state.whenText, style = LedgerlyTextStyles.label, color = colors.inkMuted)
            }
        }
        item("amount") {
            Field("Amount", Modifier.padding(bottom = 20.dp)) {
                LedgerlyTextField(state.amountText, onAmountChange, placeholder = "0", amount = true, prefix = state.signedAmount.currency.code)
            }
        }
        item("category") {
            Field("Category", Modifier.padding(bottom = 20.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categories.forEach { category ->
                        Chip(category.name, selected = category.id == state.selectedCategoryId, onClick = { onSelectCategory(category.id) })
                    }
                }
            }
        }
        item("note") {
            Field("Note", optional = true) {
                LedgerlyTextField(state.note, onNoteChange, placeholder = "What was it?")
            }
        }
        state.budget?.let { budget ->
            item("fit-head") { GroupTitle("How it fits") }
            item("fit") { LedgerlyCard { BudgetRow(budget, daysLeft = null) } }
        }
        item("actions") {
            Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.hasChanges) {
                    LedgerlyButton("Save changes", onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth()) {
                    LedgerlyButton(state.deleteLabel, { confirming = true }, style = ButtonStyle.Quiet, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Delete this entry?") },
            text = { Text("${state.title}, ${state.signedAmount.label()}. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onDelete()
                }) { Text("Delete", color = colors.loss) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Keep it") } },
            containerColor = colors.surface,
        )
    }
}
