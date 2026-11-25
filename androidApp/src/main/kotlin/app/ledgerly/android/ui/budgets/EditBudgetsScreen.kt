package app.ledgerly.android.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.Amount
import app.ledgerly.android.ui.components.ButtonStyle
import app.ledgerly.android.ui.components.GroupTitle
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyCard
import app.ledgerly.android.ui.components.LedgerlyIcon
import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.components.Page
import app.ledgerly.android.ui.components.RowsCard
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.rules.BudgetTone
import app.ledgerly.shared.presentation.budgets.BudgetEditRowState
import app.ledgerly.shared.presentation.budgets.EditBudgetsUiState
import app.ledgerly.shared.presentation.budgets.EditBudgetsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditBudgetsRoute(
    onSaved: () -> Unit,
    onClose: () -> Unit,
    viewModel: EditBudgetsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }
    EditBudgetsScreen(state, viewModel::onLimitChange, viewModel::onStep, viewModel::onSave, onClose)
}

/** A monthly limit per expense category, each with − and + to move it by KES 500. */
@Composable
fun EditBudgetsScreen(
    state: EditBudgetsUiState,
    onLimitChange: (String, String) -> Unit,
    onStep: (String, Boolean) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LedgerlyTheme.colors
    Page(title = "Budgets", onClose = onClose, closeLabel = "Cancel") {
        item("total") {
            LedgerlyCard {
                Text("Monthly total", style = LedgerlyTextStyles.label, color = colors.inkMuted)
                Amount(state.total, style = LedgerlyTextStyles.amount, showFraction = false, modifier = Modifier.padding(top = 4.dp))
                Text(state.totalText, style = LedgerlyTextStyles.caption, color = colors.inkMuted, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item("head") { GroupTitle("Per category") }
        item("rows") {
            RowsCard(state.rows) { row -> BudgetEditRow(row, onLimitChange, onStep) }
        }
        item("hint") {
            Text(
                "The buttons move a budget by KES 500. A budget carries over to next month until you change it. Clear one to stop tracking that category.",
                style = LedgerlyTextStyles.caption,
                color = colors.inkMuted,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
        item("actions") {
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LedgerlyButton("Save budgets", onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth())
                LedgerlyButton("Cancel", onClose, style = ButtonStyle.Quiet, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BudgetEditRow(row: BudgetEditRowState, onLimitChange: (String, String) -> Unit, onStep: (String, Boolean) -> Unit) {
    val colors = LedgerlyTheme.colors
    val noteColor = when (row.tone) {
        BudgetTone.OnTrack -> colors.inkMuted
        BudgetTone.Caution -> colors.caution
        BudgetTone.Over -> colors.loss
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.categoryName, style = LedgerlyTextStyles.body, color = colors.ink)
            Text(row.note, style = LedgerlyTextStyles.caption, color = noteColor)
        }
        StepButton("Lower ${row.categoryName} by KES 500", enabled = row.canDecrease, onClick = { onStep(row.categoryId, false) }) {
            LedgerlyIcon(LedgerlyIcons.Minus, size = 18.dp)
        }
        BasicTextField(
            value = row.limitText,
            onValueChange = { onLimitChange(row.categoryId, it) },
            singleLine = true,
            textStyle = LedgerlyTextStyles.label.copy(color = colors.ink, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(colors.cobalt),
            modifier = Modifier
                .width(84.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceSunk)
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .semantics { contentDescription = "${row.categoryName} budget in KES" },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (row.limitText.isEmpty()) Text("0", style = LedgerlyTextStyles.label, color = colors.inkFaint)
                    inner()
                }
            },
        )
        StepButton("Raise ${row.categoryName} by KES 500", enabled = true, onClick = { onStep(row.categoryId, true) }) {
            LedgerlyIcon(LedgerlyIcons.Plus, size = 18.dp)
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = LedgerlyTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, colors.line, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) colors.ink else colors.inkFaint) { content() }
    }
}
