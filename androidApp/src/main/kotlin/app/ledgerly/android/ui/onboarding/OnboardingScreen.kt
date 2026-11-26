package app.ledgerly.android.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.budgets.budgetEditorItems
import app.ledgerly.android.ui.components.ButtonStyle
import app.ledgerly.android.ui.components.Chip
import app.ledgerly.android.ui.components.Field
import app.ledgerly.android.ui.components.LedgerlyButton
import app.ledgerly.android.ui.components.LedgerlyIcon
import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.components.LedgerlyTextField
import app.ledgerly.android.ui.components.RoundIconButton
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.presentation.budgets.EditBudgetsViewModel
import app.ledgerly.shared.presentation.onboarding.OnboardingStep
import app.ledgerly.shared.presentation.onboarding.OnboardingUiState
import app.ledgerly.shared.presentation.onboarding.OnboardingViewModel
import org.koin.androidx.compose.koinViewModel

/** First run: welcome, name and currency, what to track, first budgets, done. */
@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel = koinViewModel(),
    budgetsViewModel: EditBudgetsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val budgets by budgetsViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(budgets.isSaved) { if (budgets.isSaved) viewModel.onBudgetsSaved() }
    BackHandler(enabled = state.step != OnboardingStep.Welcome) { viewModel.onBack() }

    val colors = LedgerlyTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        if (state.progress > 0) ProgressBar(state, viewModel::onBack)
        when (state.step) {
            OnboardingStep.Welcome -> Welcome(onStart = viewModel::onNext)
            OnboardingStep.AboutYou -> AboutYou(state, viewModel::onNameChange, viewModel::onCurrencyChange, viewModel::onNext)
            OnboardingStep.Track -> Track(state, viewModel::onTrackBudgetsChange, viewModel::onTrackDebtsChange, viewModel::onNext)
            OnboardingStep.Budgets -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = LedgerlySpacing.space5, vertical = 8.dp),
            ) {
                item("title") {
                    StepTitle("Set your first budgets", "A monthly limit for the categories you want to watch. You can change them any time.")
                }
                budgetEditorItems(budgets, budgetsViewModel::onLimitChange, budgetsViewModel::onStep)
                item("actions") {
                    Column(Modifier.padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LedgerlyButton("Save budgets", budgetsViewModel::onSave, enabled = budgets.canSave, modifier = Modifier.fillMaxWidth())
                        LedgerlyButton("Skip for now", viewModel::onNext, style = ButtonStyle.Quiet, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            OnboardingStep.Done -> Done(state, viewModel::onFinish)
        }
    }
}

@Composable
private fun ProgressBar(state: OnboardingUiState, onBack: () -> Unit) {
    val colors = LedgerlyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LedgerlySpacing.space5, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundIconButton(onBack, "Back", size = 44.dp) { LedgerlyIcon(LedgerlyIcons.Back, size = 22.dp) }
        Row(
            Modifier
                .weight(1f)
                .semantics { contentDescription = "Step ${state.progress} of ${state.progressCount}" },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(state.progressCount) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (i < state.progress) colors.cobalt else colors.line),
                )
            }
        }
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun StepTitle(title: String, body: String) {
    val colors = LedgerlyTheme.colors
    Column(Modifier.padding(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = LedgerlyTextStyles.title, color = colors.ink, modifier = Modifier.semantics { heading() })
        Text(body, style = LedgerlyTextStyles.body, color = colors.inkMuted)
    }
}

/** A step's scrolling body with its buttons pinned at the bottom. */
@Composable
private fun ColumnScope.Step(
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = LedgerlySpacing.space5),
        content = content,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LedgerlySpacing.space5, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = footer,
    )
}

@Composable
private fun ColumnScope.Welcome(onStart: () -> Unit) {
    val colors = LedgerlyTheme.colors
    Step(footer = { LedgerlyButton("Get started", onStart, modifier = Modifier.fillMaxWidth()) }) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cobalt),
            contentAlignment = Alignment.Center,
        ) { LedgerlyIcon(LedgerlyIcons.Flame, size = 32.dp, tint = colors.onAction) }
        Text("Ledgerly", style = LedgerlyTextStyles.label, color = colors.inkMuted, modifier = Modifier.padding(top = 24.dp))
        Text(
            "Know where your money goes.",
            style = LedgerlyTextStyles.display.copy(fontSize = LedgerlyTextStyles.title.fontSize * 1.4f, lineHeight = LedgerlyTextStyles.title.lineHeight * 1.4f),
            color = colors.ink,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { heading() },
        )
        Text(
            "Log what you spend in seconds, keep a daily streak, and keep track of who owes who.",
            style = LedgerlyTextStyles.body,
            color = colors.inkMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.AboutYou(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (app.ledgerly.shared.money.CurrencyCode) -> Unit,
    onNext: () -> Unit,
) {
    Step(footer = { LedgerlyButton("Continue", onNext, enabled = state.canContinue, modifier = Modifier.fillMaxWidth()) }) {
        StepTitle("A bit about you", "Ledgerly keeps everything on this phone.")
        Field("Your name", Modifier.padding(bottom = 20.dp)) {
            LedgerlyTextField(state.name, onNameChange, placeholder = "What should we call you?")
        }
        Field("Your currency") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.currencies.forEach { option ->
                    Chip(option.code.code, selected = option.code == state.currency, onClick = { onCurrencyChange(option.code) })
                }
            }
            Text(
                (state.currencies.firstOrNull { it.code == state.currency }?.label ?: "") + ". Every amount is logged in it.",
                style = LedgerlyTextStyles.caption,
                color = LedgerlyTheme.colors.inkMuted,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun ColumnScope.Track(
    state: OnboardingUiState,
    onTrackBudgetsChange: (Boolean) -> Unit,
    onTrackDebtsChange: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    Step(footer = { LedgerlyButton("Continue", onNext, modifier = Modifier.fillMaxWidth()) }) {
        StepTitle("What do you want to track?", "Spending is always on. Add the rest now or later in Settings.")
        Column(verticalArrangement = Arrangement.spacedBy(LedgerlySpacing.space3)) {
            Choice("Spending", "A daily log and a streak", LedgerlyIcons.Wallet, on = true, enabled = false) {}
            Choice("Budgets", "A limit per category", LedgerlyIcons.Up, on = state.trackBudgets) { onTrackBudgetsChange(!state.trackBudgets) }
            Choice("Debts", "What you owe and who owes you", LedgerlyIcons.People, on = state.trackDebts) { onTrackDebtsChange(!state.trackDebts) }
        }
    }
}

@Composable
private fun Choice(
    title: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    on: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit,
) {
    val colors = LedgerlyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(if (on) 2.dp else 1.dp, if (on) colors.cobalt else colors.line, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onToggle)
            .semantics { stateDescription = if (on) "On" else "Off" }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.cobaltSoft),
            contentAlignment = Alignment.Center,
        ) { LedgerlyIcon(icon, size = 22.dp, tint = colors.cobalt) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = LedgerlyTextStyles.body, color = colors.ink)
            Text(sub, style = LedgerlyTextStyles.caption, color = colors.inkMuted)
        }
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (on) colors.cobalt else colors.surface)
                .border(1.dp, if (on) colors.cobalt else colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (on) LedgerlyIcon(LedgerlyIcons.Check, size = 16.dp, tint = colors.onAction) }
    }
}

@Composable
private fun ColumnScope.Done(state: OnboardingUiState, onFinish: () -> Unit) {
    val colors = LedgerlyTheme.colors
    Step(footer = { LedgerlyButton("Go to Home", onFinish, modifier = Modifier.fillMaxWidth()) }) {
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(colors.cautionSoft),
                contentAlignment = Alignment.Center,
            ) { LedgerlyIcon(LedgerlyIcons.Flame, size = 44.dp, tint = colors.caution) }
            Text(
                if (state.name.isBlank()) "You're all set." else "You're all set, ${state.name.trim().substringBefore(' ')}.",
                style = LedgerlyTextStyles.title,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Text(state.doneText, style = LedgerlyTextStyles.body, color = colors.inkMuted, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
    }
}
