package app.ledgerly.android.ui.adddebt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.ButtonStyle
import app.ledgerly.android.ui.components.Chip
import app.ledgerly.android.ui.components.EmptyNote
import app.ledgerly.android.ui.components.Field
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyCard
import app.ledgerly.android.ui.components.LedgerlyTextField
import app.ledgerly.android.ui.components.LoanRow
import app.ledgerly.android.ui.components.Page
import app.ledgerly.android.ui.components.SegmentedControl
import app.ledgerly.android.ui.components.DebtorRow
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.presentation.adddebt.AddDebtUiState
import app.ledgerly.shared.presentation.adddebt.AddDebtViewModel
import app.ledgerly.shared.presentation.adddebt.DueChoice
import org.koin.androidx.compose.koinViewModel

/** New debt: direction, amount, who, what for, when it's due, and a preview of the row. */
@Composable
fun AddDebtRoute(
    initialDirection: DebtDirection,
    onSaved: (message: String, direction: DebtDirection) -> Unit,
    onClose: () -> Unit,
    viewModel: AddDebtViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onDirectionChange(initialDirection) }
    LaunchedEffect(state.savedMessage) { state.savedMessage?.let { onSaved(it, state.direction) } }
    AddDebtScreen(
        state = state,
        onDirectionChange = viewModel::onDirectionChange,
        onAmountChange = viewModel::onAmountChange,
        onCounterpartyChange = viewModel::onCounterpartyChange,
        onNoteChange = viewModel::onNoteChange,
        onDueChange = viewModel::onDueChange,
        onSave = viewModel::onSave,
        onClose = onClose,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDebtScreen(
    state: AddDebtUiState,
    onDirectionChange: (DebtDirection) -> Unit,
    onAmountChange: (String) -> Unit,
    onCounterpartyChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDueChange: (DueChoice) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LedgerlyTheme.colors
    val gap = Modifier.padding(bottom = 20.dp)
    Page(title = "New debt", onClose = onClose) {
        item("direction") {
            SegmentedControl(
                options = listOf(DebtDirection.OwedToMe, DebtDirection.IOwe),
                selected = state.direction,
                label = { if (it == DebtDirection.OwedToMe) "Someone owes me" else "I owe" },
                onSelect = onDirectionChange,
                modifier = gap,
            )
        }
        item("amount") {
            Field("How much", gap) {
                LedgerlyTextField(state.amountText, onAmountChange, placeholder = "0", amount = true, prefix = state.currency.code)
            }
        }
        item("who") {
            Field(state.whoLabel, gap) {
                if (state.people.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.people.forEach { name ->
                            Chip(name, selected = name == state.counterparty.trim(), onClick = { onCounterpartyChange(name) })
                        }
                    }
                }
                LedgerlyTextField(state.counterparty, onCounterpartyChange, placeholder = "Their name")
            }
        }
        item("note") {
            Field("What it's for", gap, optional = true) {
                LedgerlyTextField(state.note, onNoteChange, placeholder = "Lunch, the trip, rent")
            }
        }
        item("due") {
            Field("When it's due", gap) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.dueChoices.forEach { choice ->
                        Chip(choice.label, selected = choice.choice == state.due, onClick = { onDueChange(choice.choice) })
                    }
                }
                Text(state.dueText, style = LedgerlyTextStyles.caption, color = colors.inkMuted, modifier = Modifier.padding(start = 4.dp))
            }
        }
        item("preview") {
            Field("How it'll look in Debts", gap) {
                val preview = state.preview
                when {
                    preview == null -> LedgerlyCard { EmptyNote("Add an amount and a name to see how it'll look.") }
                    preview.direction == DebtDirection.OwedToMe ->
                        Column { DebtorRow(preview, draftFor = { null }, onRecordPayment = {}, onSend = {}, showActions = false) }
                    else -> LoanRow(preview, onRecordPayment = {}, showActions = false)
                }
            }
        }
        item("save") {
            LedgerlyButton(
                text = state.saveLabel,
                onClick = onSave,
                enabled = state.canSave,
                style = ButtonStyle.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = LedgerlySpacing.space2),
            )
        }
    }
}
