package app.ledgerly.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.model.EntryKind
import app.ledgerly.shared.domain.rules.BudgetTone
import app.ledgerly.shared.domain.rules.DueStatus
import app.ledgerly.shared.domain.rules.ReminderTone
import app.ledgerly.shared.presentation.common.BudgetRowState
import app.ledgerly.shared.presentation.common.DebtRowState
import app.ledgerly.shared.presentation.common.EntryRowState
import kotlinx.datetime.LocalDate

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "27 Nov". */
fun LocalDate.short(): String = "$dayOfMonth ${MONTHS[monthNumber - 1]}"

/** Title and amount on one line, the rest underneath. */
@Composable
private fun RowLine(title: String, trailing: String, trailingColor: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = LedgerlyTextStyles.body, color = LedgerlyTheme.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(trailing, style = LedgerlyTextStyles.amountRow, color = trailingColor, maxLines = 1)
    }
}

/** One logged expense or income. */
@Composable
fun EntryRow(row: EntryRowState, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = LedgerlyTheme.colors
    val income = row.kind == EntryKind.Income
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (income) {
            LetterTile(row.title.take(1).uppercase(), colors.gainSoft, colors.gain)
        } else {
            LetterTile(row.title.take(1).uppercase(), colors.surface, colors.ink, border = colors.line)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RowLine(row.title, row.signedAmount.label(withSign = true), if (income) colors.gain else colors.ink)
            Text("${row.categoryName} · ${row.time}", style = LedgerlyTextStyles.caption, color = colors.inkMuted)
        }
    }
}

/** A category budget: spent of limit, a track, and what's left. */
@Composable
fun BudgetRow(row: BudgetRowState, daysLeft: Int?, modifier: Modifier = Modifier) {
    val colors = LedgerlyTheme.colors
    val (tileBg, tone) = when (row.tone) {
        BudgetTone.OnTrack -> colors.gainSoft to colors.gain
        BudgetTone.Caution -> colors.cautionSoft to colors.caution
        BudgetTone.Over -> colors.lossSoft to colors.loss
    }
    val note = buildString {
        if (row.percentUsed >= 80) append("${row.percentUsed}% · ")
        if (row.tone == BudgetTone.Over) {
            append("${row.overBy.label()} over")
        } else {
            append("${row.left.label()} left")
            if (daysLeft != null) append(" · $daysLeft ${if (daysLeft == 1) "day" else "days"} to go")
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterTile(row.categoryName.take(1).uppercase(), tileBg, tone)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.categoryName, style = LedgerlyTextStyles.body, color = colors.ink)
                Text(
                    "${row.spent.label()} / ${row.limit.shownParts().whole}",
                    style = LedgerlyTextStyles.label,
                    color = colors.ink,
                )
            }
            Track(row.percentUsed / 100f, tone)
            Text(note, style = LedgerlyTextStyles.caption, color = if (row.tone == BudgetTone.OnTrack) colors.inkMuted else tone)
        }
    }
}

private fun DebtRowState.dueText(): String = when (val s = status) {
    DueStatus.Settled -> "Settled"
    DueStatus.NoDueDate -> "No due date"
    is DueStatus.Overdue -> "Overdue · was due ${dueDate?.short()}"
    is DueStatus.DueSoon -> if (s.days == 0) "Due today" else "Due ${dueDate?.short()}"
    is DueStatus.Upcoming -> "Due ${dueDate?.short()}"
}

@Composable
private fun DebtRowState.dueColor(): Color = when (status) {
    is DueStatus.Overdue -> LedgerlyTheme.colors.loss
    is DueStatus.DueSoon -> LedgerlyTheme.colors.caution
    else -> LedgerlyTheme.colors.inkMuted
}

private fun remindedText(days: Int): String = when (days) {
    0 -> "Reminded today"
    1 -> "Reminded yesterday"
    else -> "Reminded $days days ago"
}

/**
 * Someone who owes the user. "Remind" opens a tone picker and the draft; "Send reminder" hands
 * the text to [onSend], which opens the share sheet. Ledgerly never messages anyone itself.
 */
@Composable
fun DebtorRow(
    row: DebtRowState,
    draftFor: (ReminderTone) -> String?,
    onRecordPayment: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
) {
    val colors = LedgerlyTheme.colors
    val overdue = row.status is DueStatus.Overdue
    val settled = row.status == DueStatus.Settled
    var open by rememberSaveable(row.id) { mutableStateOf(false) }
    var tone by rememberSaveable(row.id) { mutableStateOf(if (overdue) ReminderTone.Clear else ReminderTone.Friendly) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LedgerlyRadius.radiusMd))
            .background(if (overdue) colors.lossSoft else colors.surface)
            .padding(horizontal = LedgerlySpacing.space5),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = LedgerlySpacing.space5, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LetterTile(initials(row.counterparty), colors.surface, colors.ink, border = colors.line)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RowLine(row.counterparty, row.remaining.label(), colors.ink)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(row.dueText(), style = LedgerlyTextStyles.caption, color = row.dueColor())
                    row.daysSinceReminder?.let { Text(remindedText(it), style = LedgerlyTextStyles.caption, color = colors.inkMuted) }
                }
                if (row.paid.minorUnits > 0) {
                    Track(row.percentPaid / 100f, colors.gain, Modifier.padding(top = 6.dp), height = 5.dp)
                    Text("${row.paid.label()} of ${row.principal.label()} paid", style = LedgerlyTextStyles.caption, color = colors.inkMuted)
                }
            }
        }
        if (!showActions) {
            Spacer(Modifier.height(8.dp))
        } else if (settled) {
            Text(
                "Paid in full",
                style = LedgerlyTextStyles.caption,
                color = colors.gain,
                modifier = Modifier.padding(bottom = LedgerlySpacing.space5),
            )
        } else if (open) {
            Column(Modifier.padding(bottom = LedgerlySpacing.space5), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedControl(
                    options = ReminderTone.entries,
                    selected = tone,
                    label = { it.name },
                    onSelect = { tone = it },
                    background = if (overdue) colors.surface else colors.surfaceSunk,
                )
                val draft = draftFor(tone).orEmpty()
                Text(
                    draft,
                    style = LedgerlyTextStyles.body,
                    color = colors.ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LedgerlyRadius.radiusMd))
                        .background(if (overdue) colors.surface else colors.surfaceSunk)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    LedgerlyButton("Cancel", { open = false }, style = ButtonStyle.Quiet)
                    LedgerlyButton(
                        "Send reminder",
                        onClick = {
                            open = false
                            if (draft.isNotEmpty()) onSend(draft)
                        },
                        leading = { LedgerlyIcon(LedgerlyIcons.Send, size = 18.dp) },
                    )
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = LedgerlySpacing.space5),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                LedgerlyButton("Record payment", onRecordPayment, style = ButtonStyle.Quiet, small = true)
                LedgerlyButton(
                    "Remind",
                    onClick = { open = true },
                    style = ButtonStyle.Soft,
                    small = true,
                    leading = { LedgerlyIcon(LedgerlyIcons.Send, size = 16.dp) },
                )
            }
        }
    }
}

/** Money the user owes: balance, how much is paid off, and Record payment. */
@Composable
fun LoanRow(row: DebtRowState, onRecordPayment: () -> Unit, modifier: Modifier = Modifier, showActions: Boolean = true) {
    val colors = LedgerlyTheme.colors
    val overdue = row.status is DueStatus.Overdue
    val settled = row.status == DueStatus.Settled
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LedgerlyRadius.radiusMd))
            .background(if (overdue) colors.lossSoft else colors.surface)
            .padding(horizontal = LedgerlySpacing.space5),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = LedgerlySpacing.space5, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LetterTile(initials(row.counterparty), colors.surface, colors.ink, border = colors.line)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RowLine(row.counterparty, row.remaining.label(), colors.ink)
                Text(row.dueText(), style = LedgerlyTextStyles.caption, color = row.dueColor())
                Track(row.percentPaid / 100f, colors.gain, Modifier.padding(top = 6.dp), height = 5.dp)
                Text("${row.percentPaid}% paid off of ${row.principal.label()}", style = LedgerlyTextStyles.caption, color = colors.inkMuted)
            }
        }
        if (!showActions) {
            Spacer(Modifier.height(8.dp))
        } else if (!settled) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = LedgerlySpacing.space5),
                horizontalArrangement = Arrangement.End,
            ) {
                LedgerlyButton("Record payment", onRecordPayment, style = ButtonStyle.Soft, small = true)
            }
        } else {
            Text("Paid off", style = LedgerlyTextStyles.caption, color = colors.gain, modifier = Modifier.padding(bottom = LedgerlySpacing.space5))
        }
    }
}
