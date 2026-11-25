package app.ledgerly.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.presentation.spend.DayBarState
import kotlinx.datetime.LocalDate

/**
 * Daily spending for the month. Past days are bars scaled to the biggest day, future days are
 * hatched. The highlighted day (the selected one, else today) gets the gradient and a tip.
 */
@Composable
fun SpendBars(
    bars: List<DayBarState>,
    modifier: Modifier = Modifier,
    onSelect: ((LocalDate) -> Unit)? = null,
) {
    if (bars.isEmpty()) return
    val colors = LedgerlyTheme.colors
    val highlighted = bars.firstOrNull { it.isSelected } ?: bars.firstOrNull { it.isToday }
    val max = bars.filter { !it.isFuture }.maxOfOrNull { it.spent.minorUnits }?.coerceAtLeast(1L) ?: 1L
    val past = lerp(colors.surface, colors.cobalt, 0.3f)
    val gradient = Brush.verticalGradient(listOf(colors.violet, colors.cobalt))

    Column(modifier.fillMaxWidth()) {
        if (highlighted != null) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(LedgerlyRadius.radiusSm))
                    .border(1.dp, colors.line, RoundedCornerShape(LedgerlyRadius.radiusSm))
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(highlighted.date.short(), style = LedgerlyTextStyles.caption, color = colors.inkMuted)
                Text(
                    if (highlighted.isToday && highlighted.spent.minorUnits == 0L) "Nothing logged yet" else highlighted.spent.label(),
                    style = LedgerlyTextStyles.label,
                    color = colors.ink,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                val on = bar == highlighted
                val fraction = if (bar.isFuture) 1f else (bar.spent.minorUnits.toFloat() / max).coerceIn(0.05f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (onSelect != null && !bar.isFuture) {
                                Modifier
                                    .clickable(role = Role.Button) { onSelect(bar.date) }
                                    .semantics { contentDescription = "${bar.date.short()}, ${bar.spent.label()}" }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val shape = CircleShape
                    val fill = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .clip(shape)
                    when {
                        bar.isFuture -> Canvas(fill) { hatch(colors.line) }
                        on -> Box(fill.background(gradient))
                        else -> Box(fill.background(past))
                    }
                }
            }
        }
        // Axis: the 1st, every 7 days, and the last day.
        val ticks = (listOf(0) + (7 until bars.size - 3 step 7) + listOf(bars.size - 1)).distinct()
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(18.dp),
        ) {
            val slot = maxWidth / bars.size
            ticks.forEach { i ->
                val bar = bars[i]
                Text(
                    bar.date.short(),
                    style = LedgerlyTextStyles.caption.let { if (bar == highlighted) it.copy(fontWeight = FontWeight.SemiBold) else it },
                    color = if (bar == highlighted) colors.ink else colors.inkMuted,
                    modifier = Modifier.offset(x = (slot * i).coerceAtMost(maxWidth - 44.dp)),
                )
            }
        }
    }
}

/** The three-quarter budget gauge: cobalt to violet over a quiet track, figures in the middle. */
@Composable
fun Gauge(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
    stroke: Dp = 22.dp,
    center: @Composable ColumnScope.() -> Unit,
) {
    val colors = LedgerlyTheme.colors
    Box(modifier.size(width = size, height = size * 0.86f), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.size(size)) {
            val px = stroke.toPx()
            val arcSize = Size(this.size.width - px, this.size.height - px)
            val topLeft = Offset(px / 2, px / 2)
            drawArc(colors.line, startAngle = 135f, sweepAngle = 270f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(px, cap = StrokeCap.Round))
            val v = fraction.coerceIn(0f, 1f)
            if (v > 0.005f) {
                drawArc(
                    brush = Brush.linearGradient(listOf(colors.cobalt, colors.violet), start = Offset(0f, this.size.height), end = Offset(this.size.width, 0f)),
                    startAngle = 135f,
                    sweepAngle = 270f * v,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(px, cap = StrokeCap.Round),
                )
            }
        }
        Column(
            Modifier
                .size(size)
                .padding(stroke * 2),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = center,
        )
    }
}

