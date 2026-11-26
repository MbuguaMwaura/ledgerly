package app.ledgerly.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import app.ledgerly.android.ui.components.Field
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyCard
import app.ledgerly.android.ui.components.LedgerlyTextField
import app.ledgerly.android.ui.components.Page
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.money.CurrencyCode
import app.ledgerly.shared.presentation.settings.ChangeCurrencyUiState
import app.ledgerly.shared.presentation.settings.ChangeCurrencyViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChangeCurrencyRoute(
    onDone: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: ChangeCurrencyViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) {
        if (state.isDone) onDone(state.doneMessage)
    }
    ChangeCurrencyScreen(
        state = state,
        onCurrencySelect = viewModel::onCurrencySelect,
        onRateChange = viewModel::onRateChange,
        onSave = viewModel::onSave,
        onClose = onClose,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChangeCurrencyScreen(
    state: ChangeCurrencyUiState,
    onCurrencySelect: (CurrencyCode) -> Unit,
    onRateChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LedgerlyTheme.colors
    val gap = Modifier.padding(bottom = 20.dp)
    Page(title = "Change currency", onClose = onClose, closeLabel = "Back", back = true) {
        if (state.isLoading) return@Page
        item("intro") {
            Text(
                "You log in ${state.fromText}. Everything already logged moves to the new currency: entries, budgets and debts.",
                style = LedgerlyTextStyles.body,
                color = colors.inkMuted,
                modifier = gap,
            )
        }
        item("to") {
            Field("New currency", gap) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.options.forEach { option ->
                        Chip(option.code.code, selected = option.code == state.selected, onClick = { onCurrencySelect(option.code) })
                    }
                }
                state.options.firstOrNull { it.code == state.selected }?.let {
                    Text(it.label, style = LedgerlyTextStyles.caption, color = colors.inkMuted, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        item("rate") {
            Field("Exchange rate", gap) {
                LedgerlyTextField(
                    state.rateText,
                    onRateChange,
                    placeholder = "1",
                    amount = true,
                    prefix = state.rateLabel,
                    textStyle = LedgerlyTextStyles.body,
                )
                Text(
                    "Leave it at 1 to keep the numbers as they are, if you picked the wrong currency at the start.",
                    style = LedgerlyTextStyles.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        if (state.example.isNotEmpty()) {
            item("example") {
                LedgerlyCard(modifier = gap) {
                    Text(state.example, style = LedgerlyTextStyles.body, color = colors.ink)
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
