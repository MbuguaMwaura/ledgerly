package app.ledgerly.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyRadius
import app.ledgerly.android.ui.theme.tokens.LedgerlySpacing
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles
import app.ledgerly.shared.money.Money
import app.ledgerly.shared.money.Sign

/** "KES 1,250", or with a sign for entries: "− KES 650", "+ KES 4,000". */
fun Money.label(withSign: Boolean = false, withFraction: Boolean = false): String {
    val parts = parts()
    val text = parts.format(withFraction)
    return when {
        withSign && parts.sign == Sign.Negative -> "− $text"
        withSign && parts.sign == Sign.Positive -> "+ $text"
        !withSign && parts.sign == Sign.Negative -> "− $text"
        else -> text
    }
}

/** Up to two initials: "Wanjiru Kamau" → "WK". */
fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** A flat white card. Cards never get a shadow; only floating controls do. */
@Composable
fun LedgerlyCard(
    modifier: Modifier = Modifier,
    color: Color = LedgerlyTheme.colors.surface,
    contentPadding: PaddingValues = PaddingValues(LedgerlySpacing.space5),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LedgerlyRadius.radiusLg))
            .background(color)
            .padding(contentPadding),
        content = content,
    )
}

/** A card of rows with hairlines between them. */
@Composable
fun <T> RowsCard(items: List<T>, modifier: Modifier = Modifier, row: @Composable (T) -> Unit) {
    LedgerlyCard(modifier, contentPadding = PaddingValues(horizontal = LedgerlySpacing.space5)) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = LedgerlyTheme.colors.line)
            row(item)
        }
    }
}

/** "Recent ............ Daily log" above a group of rows. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = LedgerlyTextStyles.heading, color = LedgerlyTheme.colors.ink)
        if (action != null) LinkButton(action, onAction)
    }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = LedgerlyTextStyles.label,
        color = LedgerlyTheme.colors.cobalt,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    )
}

enum class ButtonStyle { Primary, Soft, Quiet, OnDeep, OnDeepQuiet }

/** The pill buttons. Primary is the `action` fill: the strongest button on a screen. */
@Composable
fun LedgerlyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    small: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = LedgerlyTheme.colors
    val (background, content) = when {
        !enabled && style == ButtonStyle.Primary -> colors.surfaceSunk to colors.inkMuted
        style == ButtonStyle.Primary -> colors.action to colors.onAction
        style == ButtonStyle.Soft -> colors.cobaltSoft to colors.cobalt
        style == ButtonStyle.Quiet -> Color.Transparent to colors.inkMuted
        style == ButtonStyle.OnDeep -> colors.onDeep to colors.deep
        else -> Color.Transparent to colors.onDeepMuted
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = if (small) 38.dp else 50.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = LedgerlySpacing.space5),
        horizontalArrangement = Arrangement.spacedBy(LedgerlySpacing.space2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            leading?.invoke()
            Text(text, style = if (small) LedgerlyTextStyles.label else LedgerlyTextStyles.label.copy(fontSize = 15.sp), color = content, textAlign = TextAlign.Center)
        }
    }
}

/** The round outlined header button, e.g. "Add debt". */
@Composable
fun RoundIconButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier, size: Dp = 52.dp, content: @Composable () -> Unit) {
    val colors = LedgerlyTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.line, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { this.contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) { content() }
    }
}

/** A two- or three-way switch, e.g. "Owed to you | You owe". */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    background: Color = LedgerlyTheme.colors.surface,
) {
    val colors = LedgerlyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(if (on) colors.ink else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = LedgerlyTextStyles.label, color = if (on) colors.surface else colors.inkMuted)
            }
        }
    }
}

/** A picker pill. Selected is the `ink` fill. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LedgerlyTheme.colors
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(if (selected) colors.ink else colors.surface)
            .border(1.dp, if (selected) colors.ink else colors.line, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LedgerlyTextStyles.label, color = if (selected) colors.surface else colors.ink)
    }
}

/** A round letter tile, tinted by tone. */
@Composable
fun LetterTile(text: String, background: Color, content: Color, modifier: Modifier = Modifier, border: Color? = null) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            Text(text, style = LedgerlyTextStyles.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = content)
        }
    }
}

/** Diagonal hatching: future bars, empty tracks, the unused part of the gauge. */
fun DrawScope.hatch(color: Color, spacing: Float = 5.dp.toPx(), stroke: Float = 2.dp.toPx()) {
    clipRect {
        var x = -size.height
        while (x < size.width) {
            drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stroke)
            x += spacing
        }
    }
}

/** A progress track: hatched when empty, filled with [fill] up to [fraction]. */
@Composable
fun Track(fraction: Float, fill: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val line = LedgerlyTheme.colors.line
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape),
    ) {
        Canvas(Modifier.fillMaxSize()) { hatch(line) }
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(CircleShape)
                .background(fill),
        )
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = LedgerlyTextStyles.body,
        color = LedgerlyTheme.colors.inkMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 28.dp),
    )
}
