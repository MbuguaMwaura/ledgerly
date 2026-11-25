package app.ledgerly.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.domain.rules.StreakState
import app.ledgerly.shared.presentation.home.StreakDayState
import app.ledgerly.shared.presentation.home.StreakMark

/** The one dark hero card: the streak count, this week, and the day's log buttons. */
@Composable
fun StreakCard(
    streak: StreakState,
    week: List<StreakDayState>,
    nextMilestone: Int?,
    onLog: () -> Unit,
    onNothingSpent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerlyTheme.colors
    val days = streak.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LedgerlyRadius.radiusLg))
            .background(colors.deep)
            .drawBehind {
                // The violet glow in the top-right corner.
                val radius = 150.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.violet.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(size.width - 50.dp.toPx(), 30.dp.toPx()),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width - 50.dp.toPx(), 30.dp.toPx()),
                )
            }
            .padding(LedgerlySpacing.space5)
            .semantics(mergeDescendants = false) { contentDescription = "$days day streak" },
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onDeep) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2)) {
                    LedgerlyIcon(LedgerlyIcons.Flame, size = 36.dp, modifier = Modifier.align(Alignment.CenterVertically))
                    Text("$days", style = LedgerlyTextStyles.display.copy(lineHeight = 60.sp), color = colors.onDeep)
                    Text(
                        "${if (days == 1) "day" else "days"}\nin a row",
                        style = LedgerlyTextStyles.label.copy(lineHeight = 17.sp),
                        color = colors.onDeepMuted,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "${streak.freezesHeld} streak freezes" },
                    horizontalArrangement = Arrangement.spacedBy(LedgerlySpacing.space1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LedgerlyIcon(LedgerlyIcons.Snow, size = 16.dp)
                    Text("${streak.freezesHeld}", style = LedgerlyTextStyles.label.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = colors.onDeep)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = LedgerlySpacing.space5),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                week.forEach { day -> WeekDay(day) }
            }

            if (streak.loggedToday) {
                Row(
                    Modifier.padding(top = LedgerlySpacing.space5),
                    horizontalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LedgerlyIcon(LedgerlyIcons.Check, size = 18.dp)
                    val text = when {
                        streak.milestoneToday != null -> "$days days. That's a milestone."
                        nextMilestone != null -> "Logged today. ${nextMilestone - days} more to $nextMilestone days."
                        else -> "Logged today. See you tomorrow."
                    }
                    Text(text, style = LedgerlyTextStyles.label.copy(fontSize = 15.sp), color = colors.onDeep)
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = LedgerlySpacing.space5),
                    horizontalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2),
                ) {
                    LedgerlyButton("Log today", onLog, style = ButtonStyle.OnDeep, modifier = Modifier.weight(1f))
                    LedgerlyButton("No spend today", onNothingSpent, style = ButtonStyle.OnDeepQuiet)
                }
            }
        }
    }
}

@Composable
private fun WeekDay(day: StreakDayState) {
    val colors = LedgerlyTheme.colors
    val (fill, content) = when (day.mark) {
        StreakMark.Logged -> colors.onDeep to colors.deep
        StreakMark.Frozen -> Color.White.copy(alpha = 0.28f) to colors.onDeep
        else -> Color.White.copy(alpha = 0.10f) to colors.onDeep
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(fill)
                .then(if (day.isToday) Modifier.border(2.dp, colors.onDeep, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when (day.mark) {
                StreakMark.Logged -> LedgerlyIcon(LedgerlyIcons.Check, size = 14.dp, tint = content)
                StreakMark.Frozen -> LedgerlyIcon(LedgerlyIcons.Snow, size = 14.dp, tint = content)
                else -> Unit
            }
        }
        Text(
            day.letter,
            style = LedgerlyTextStyles.caption.let { if (day.isToday) it.copy(fontWeight = FontWeight.Bold) else it },
            color = if (day.isToday) colors.onDeep else colors.onDeepMuted,
        )
    }
}
