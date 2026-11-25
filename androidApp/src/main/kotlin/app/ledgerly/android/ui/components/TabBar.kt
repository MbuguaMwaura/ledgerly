package app.ledgerly.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ledgerly.android.ui.theme.LedgerlyTheme
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles

/** The `shadow-float` for floating controls. Cards stay flat. */
fun Modifier.floatShadow(): Modifier = shadow(elevation = 14.dp, shape = CircleShape, ambientColor = Color(0x1F0D0D12), spotColor = Color(0x1F0D0D12))

data class TabItem<T>(val id: T, val label: String, val icon: ImageVector)

/**
 * The floating pill of tabs with the round + beside it. The + toggles [logMenuOpen]; the caller
 * shows the Expense / Income pills above it. [nudge] puts a dot on the + when nothing is logged today.
 */
@Composable
fun <T> TabBar(
    items: List<TabItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    logMenuOpen: Boolean,
    onLogClick: () -> Unit,
    nudge: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerlyTheme.colors
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .floatShadow()
                .clip(CircleShape)
                .background(colors.surface)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { item ->
                val on = item.id == selected
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (on) colors.cobaltSoft else Color.Transparent)
                        .clickable(role = Role.Tab) { onSelect(item.id) }
                        .semantics {
                            contentDescription = item.label
                            this.selected = on
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    LedgerlyIcon(item.icon, size = 22.dp, tint = if (on) colors.cobalt else colors.inkMuted)
                }
            }
        }
        Box {
            Box(
                Modifier
                    .floatShadow()
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.action)
                    .clickable(role = Role.Button, onClick = onLogClick)
                    .semantics { contentDescription = if (logMenuOpen) "Close" else "Log money" },
                contentAlignment = Alignment.Center,
            ) {
                LedgerlyIcon(
                    if (logMenuOpen) LedgerlyIcons.Close else LedgerlyIcons.Plus,
                    tint = colors.onAction,
                    modifier = if (logMenuOpen) Modifier.rotate(90f) else Modifier,
                )
            }
            if (nudge && !logMenuOpen) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-3).dp, y = 3.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.canvas)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(colors.cobalt)
                        .semantics { contentDescription = "Nothing logged today" },
                )
            }
        }
    }
}

/** One of the pills that open above the +: "Expense", "Income". */
@Composable
fun LogMenuPill(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LedgerlyTheme.colors
    Row(
        modifier
            .floatShadow()
            .clip(CircleShape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surfaceSunk),
            contentAlignment = Alignment.Center,
        ) {
            LedgerlyIcon(icon, size = 16.dp, tint = colors.ink)
        }
        Text(text, style = LedgerlyTextStyles.label.copy(fontSize = LedgerlyTextStyles.body.fontSize), color = colors.ink)
    }
}
