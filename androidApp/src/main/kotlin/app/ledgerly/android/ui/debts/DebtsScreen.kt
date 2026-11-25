package app.ledgerly.android.ui.debts

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ledgerly.android.ui.components.Amount
import app.ledgerly.android.ui.components.DebtorRow
import app.ledgerly.android.ui.components.EmptyNote
import app.ledgerly.android.ui.components.LoanRow
import app.ledgerly.android.ui.components.SegmentedControl
import app.ledgerly.android.ui.components.TabScreen
import app.ledgerly.android.ui.components.TabTitle
import app.ledgerly.android.ui.components.LedgerlyIcons
import app.ledgerly.android.ui.components.LedgerlyIcon
import app.ledgerly.android.ui.components.RoundIconButton
import app.ledgerly.android.ui.components.label
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.model.DebtDirection
import app.ledgerly.shared.domain.rules.ReminderTone
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.presentation.common.DebtRowState
import app.ledgerly.shared.presentation.debts.DebtsUiState
import app.ledgerly.shared.presentation.debts.DebtsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DebtsRoute(
    onAddDebt: (DebtDirection) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var remindingId by rememberSaveable { mutableStateOf<String?>(null) }
    // The share sheet comes back whether or not the user sent it; returning from it counts as reminded.
    val share = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        remindingId?.let(viewModel::onReminderSent)
        remindingId = null
    }
    var paying by rememberSaveable { mutableStateOf<String?>(null) }

    DebtsScreen(
        state = state,
        onSelectDirection = viewModel::onSelectDirection,
        onAddDebt = { onAddDebt(state.direction) },
        draftFor = viewModel::reminderDraft,
        onRecordPayment = { paying = it },
        onSendReminder = { id, text ->
            remindingId = id
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            share.launch(Intent.createChooser(send, "Send reminder"))
        },
        modifier = modifier,
    )

    val row = paying?.let { id -> state.allRows().firstOrNull { it.id == id } }
    if (row != null) {
        RecordPaymentSheet(
            row = row,
            onSave = { amount ->
                viewModel.onRecordPayment(row.id, amount)
                paying = null
            },
            onDismiss = { paying = null },
        )
    }
}

private fun DebtsUiState.allRows(): List<DebtRowState> = overdue + dueSoon + later + settled

@Composable
fun DebtsScreen(
    state: DebtsUiState,
    onSelectDirection: (DebtDirection) -> Unit,
    onAddDebt: () -> Unit,
    draftFor: (String, ReminderTone) -> String?,
    onRecordPayment: (String) -> Unit,
    onSendReminder: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerlyTheme.colors
    val owedToMe = state.direction == DebtDirection.OwedToMe

    TabScreen(modifier) {
        item("title") {
            TabTitle("Debts") {
                RoundIconButton(onAddDebt, "Add debt") { LedgerlyIcon(LedgerlyIcons.Plus, size = 22.dp) }
            }
        }

        item("totals") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerlySpacing.space3)) {
                Stat("Coming in", state.totalOwedToMe, "Owed to you", Modifier.weight(1f))
                Stat("Going out", state.totalIOwe, "You owe", Modifier.weight(1f))
            }
        }

        if (owedToMe && state.overdue.isNotEmpty()) {
            item("overdue-alert") {
                val total = state.overdue.fold(Money.zero(state.totalOwedToMe.currency)) { sum, row -> sum + row.remaining }
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LedgerlyRadius.radiusMd))
                        .background(colors.lossSoft)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.loss),
                    )
                    val n = state.overdue.size
                    Text("$n overdue · ${total.label()} waiting on ${if (n == 1) "one person" else "$n people"}", style = LedgerlyTextStyles.label, color = colors.ink)
                }
            }
        }

        item("direction") {
            SegmentedControl(
                options = listOf(DebtDirection.OwedToMe, DebtDirection.IOwe),
                selected = state.direction,
                label = { if (it == DebtDirection.OwedToMe) "Owed to you" else "You owe" },
                onSelect = onSelectDirection,
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
            )
        }

        val groups = listOf(
            "Overdue" to state.overdue,
            "Due soon" to state.dueSoon,
            "Later" to state.later,
            "Settled" to state.settled,
        ).filter { it.second.isNotEmpty() }

        groups.forEach { (title, rows) ->
            item("head-$title") {
                Text(title, style = LedgerlyTextStyles.label, color = colors.inkMuted, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp))
            }
            rows.forEach { row ->
                item(row.id) {
                    val rowModifier = Modifier.padding(bottom = LedgerlySpacing.space3)
                    if (owedToMe) {
                        DebtorRow(
                            row = row,
                            draftFor = { tone -> draftFor(row.id, tone) },
                            onRecordPayment = { onRecordPayment(row.id) },
                            onSend = { text -> onSendReminder(row.id, text) },
                            modifier = rowModifier,
                        )
                    } else {
                        LoanRow(row, onRecordPayment = { onRecordPayment(row.id) }, modifier = rowModifier)
                    }
                }
            }
        }

        if (groups.isEmpty() && !state.isLoading) {
            item("empty") {
                EmptyNote(if (owedToMe) "Nobody owes you anything right now." else "You don't owe anyone right now.")
            }
        }

        if (owedToMe) {
            item("footer") {
                Text(
                    "Reminders go out from your own phone. Ledgerly never messages anyone.",
                    style = LedgerlyTextStyles.caption,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Stat(title: String, amount: Money, caption: String, modifier: Modifier) {
    val colors = LedgerlyTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(LedgerlyRadius.radiusMd))
            .background(colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = LedgerlyTextStyles.label, color = colors.inkMuted)
        Amount(amount, style = LedgerlyTextStyles.amount.copy(fontSize = LedgerlyTextStyles.amount.fontSize * 0.86f), showFraction = false)
        Text(caption, style = LedgerlyTextStyles.caption, color = colors.inkMuted)
    }
}
