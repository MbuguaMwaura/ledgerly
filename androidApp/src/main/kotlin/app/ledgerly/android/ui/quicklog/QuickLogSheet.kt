package app.ledgerly.android.ui.quicklog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.ButtonStyle
import app.ledgerly.android.ui.components.Chip
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyIcon
import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.components.RoundIconButton
import app.ledgerly.android.ui.components.label
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.presentation.quicklog.KeypadKey
import app.ledgerly.shared.presentation.quicklog.QuickLogUiState
import app.ledgerly.shared.presentation.quicklog.QuickLogViewModel

/**
 * Amount → category → save. Shown over any tab from the + button. When the shared ViewModel
 * reports the save, the sheet hands its message to [onSaved] and closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogSheet(
    viewModel: QuickLogViewModel,
    showNothingSpent: Boolean,
    onNothingSpent: () -> Unit,
    onSaved: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.savedMessage) {
        val message = state.savedMessage ?: return@LaunchedEffect
        sheetState.hide()
        onSaved(message)
        viewModel.reset()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LedgerlyTheme.colors.surface,
        shape = RoundedCornerShape(topStart = LedgerlyRadius.radiusLg, topEnd = LedgerlyRadius.radiusLg),
    ) {
        QuickLogContent(
            state = state,
            onKey = viewModel::onKey,
            onSelectCategory = viewModel::onSelectCategory,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::onSave,
            showNothingSpent = showNothingSpent && state.kind == EntryKind.Expense,
            onNothingSpent = onNothingSpent,
            onClose = onDismiss,
        )
    }
}

private val KEYS: List<KeypadKey> = (1..9).map { KeypadKey.Digit(it) } + listOf(KeypadKey.Decimal, KeypadKey.Digit(0), KeypadKey.Backspace)

@Composable
fun QuickLogContent(
    state: QuickLogUiState,
    onKey: (KeypadKey) -> Unit,
    onSelectCategory: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    showNothingSpent: Boolean,
    onNothingSpent: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LedgerlyTheme.colors
    val currency = state.amount.currency.code
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LedgerlySpacing.space5)
            .padding(bottom = LedgerlySpacing.space5)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.kind == EntryKind.Expense) "Log expense" else "Log income", style = LedgerlyTextStyles.heading, color = colors.ink)
            RoundIconButton(onClose, "Close", size = 44.dp) { LedgerlyIcon(LedgerlyIcons.Close, size = 20.dp) }
        }

        val empty = state.amountText.isEmpty()
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 20.sp, color = colors.inkFaint, baselineShift = BaselineShift(0.5f), letterSpacing = 0.sp)) { append(currency) }
                append(" ")
                withStyle(SpanStyle(color = if (empty) colors.inkFaint else colors.ink)) { append(if (empty) "0" else state.amountText) }
            },
            style = LedgerlyTextStyles.amountHero.copy(fontSize = 44.sp, lineHeight = 52.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LedgerlySpacing.space2)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = if (empty) "No amount yet" else state.amount.label(withFraction = true)
                },
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(state.categories, key = { it.id }) { category ->
                Chip(category.name, selected = category.id == state.selectedCategoryId, onClick = { onSelectCategory(category.id) })
            }
        }

        BasicTextField(
            value = state.note,
            onValueChange = onNoteChange,
            singleLine = true,
            textStyle = LedgerlyTextStyles.body.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.cobalt),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LedgerlyRadius.radiusSm))
                .background(colors.surfaceSunk)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { field ->
                Box {
                    if (state.note.isEmpty()) Text("Add a note (optional)", style = LedgerlyTextStyles.body, color = colors.inkMuted)
                    field()
                }
            },
        )

        Column(
            Modifier.padding(vertical = LedgerlySpacing.space2),
            verticalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2),
        ) {
            KEYS.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2)) {
                    row.forEach { key -> Key(key, onKey, Modifier.weight(1f)) }
                }
            }
        }

        LedgerlyButton(
            text = when {
                state.isSaving -> "Saving…"
                state.canSave -> "Save ${state.amount.label()}"
                else -> "Enter an amount and category"
            },
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showNothingSpent) {
            LedgerlyButton("Nothing spent today", onNothingSpent, style = ButtonStyle.Quiet, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Key(key: KeypadKey, onKey: (KeypadKey) -> Unit, modifier: Modifier) {
    val colors = LedgerlyTheme.colors
    val description = when (key) {
        KeypadKey.Backspace -> "Delete"
        KeypadKey.Decimal -> "Decimal point"
        is KeypadKey.Digit -> key.value.toString()
    }
    Box(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(LedgerlyRadius.radiusSm))
            .background(colors.surfaceSunk)
            .clickable(role = Role.Button) { onKey(key) }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            KeypadKey.Backspace -> LedgerlyIcon(LedgerlyIcons.Back, size = 22.dp, tint = colors.ink)
            KeypadKey.Decimal -> Text(".", style = LedgerlyTextStyles.heading.copy(fontSize = 22.sp), color = colors.ink)
            is KeypadKey.Digit -> Text(key.value.toString(), style = LedgerlyTextStyles.heading.copy(fontSize = 22.sp), color = colors.ink)
        }
    }
}
