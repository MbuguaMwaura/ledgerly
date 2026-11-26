package app.ledgerly.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ledgerly Clean's icon set: 24dp, 2dp-stroke outlines. Drawn in black and tinted by [LedgerlyIcon],
 * so they follow the surrounding content colour.
 */
object LedgerlyIcons {
    val Plus = icon("Plus", "M12 5v14M5 12h14")
    val Close = icon("Close", "M6 6l12 12M18 6L6 18")
    val Sliders = icon("Sliders", "M4 7h9M17 7h3M15 5v4M4 17h3M11 17h9M9 15v4")
    val Minus = icon("Minus", "M5 12h14")
    val Trash = icon("Trash", "M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3")
    val Check = icon("Check", "M5 12.5l4.5 4.5L19 7.5")
    val Flame = icon("Flame", "M12 3c1 3.5 5 5.5 5 10a5 5 0 0 1-10 0c0-2 1-3.5 2-4.5.3 1.8 1.2 2.8 2.3 3.2C10.8 9 11.5 6 12 3z")
    val Bell = icon("Bell", "M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15zM10 20.5a2 2 0 0 0 4 0")
    val Snow = icon("Snow", "M12 3v18M4.2 7.5l15.6 9M4.2 16.5l15.6-9")
    val Send = icon("Send", "M4 12l16-8-6 16-2.5-6.5L4 12z")
    val Back = icon("Back", "M14 7l-5 5 5 5M20 12H9")
    val Up = icon("Up", "M12 19V5M6 11l6-6 6 6")
    val Down = icon("Down", "M12 5v14M6 13l6 6 6-6")
    val Home = icon("Home", "M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-5H9v5H5a1 1 0 0 1-1-1z")
    val Wallet = icon("Wallet", "M4 7h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2zM4 7l11-3v3M16 13.5h.01")
    val People = icon("People", "M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM3 20c0-3 2.7-5 6-5s6 2 6 5M16 5a3 3 0 0 1 0 6M18 15c2 .6 3 2.2 3 5")

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = addPathNodes(path),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
}

@Composable
fun LedgerlyIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(size),
        tint = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint,
    )
}
