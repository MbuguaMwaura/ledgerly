package app.ledgerly.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import app.ledgerly.android.ui.theme.tokens.LedgerlyColors
import app.ledgerly.android.ui.theme.tokens.LedgerlyDarkColors
import app.ledgerly.android.ui.theme.tokens.LedgerlyLightColors
import app.ledgerly.android.ui.theme.tokens.LedgerlyTextStyles

private val LocalLedgerlyColors = staticCompositionLocalOf { LedgerlyLightColors }

/**
 * Ledgerly Clean on Android. Material 3 components get a colour scheme mapped from the tokens;
 * Ledgerly's own components read the full token set through [LedgerlyTheme.colors].
 */
@Composable
fun LedgerlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) LedgerlyDarkColors else LedgerlyLightColors
    val scheme = colors.toMaterialScheme(darkTheme)
    CompositionLocalProvider(LocalLedgerlyColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = LedgerlyTypography, content = content)
    }
}

object LedgerlyTheme {
    val colors: LedgerlyColors
        @Composable @ReadOnlyComposable
        get() = LocalLedgerlyColors.current
}

private fun LedgerlyColors.toMaterialScheme(dark: Boolean) =
    if (dark) {
        darkColorScheme(
            primary = cobalt, onPrimary = onCobalt, primaryContainer = cobaltSoft, onPrimaryContainer = ink,
            background = canvas, onBackground = ink, surface = surface, onSurface = ink,
            surfaceVariant = surfaceSunk, onSurfaceVariant = inkMuted, outline = line, outlineVariant = line,
            error = loss, onError = onSignal, errorContainer = lossSoft,
        )
    } else {
        lightColorScheme(
            primary = cobalt, onPrimary = onCobalt, primaryContainer = cobaltSoft, onPrimaryContainer = ink,
            background = canvas, onBackground = ink, surface = surface, onSurface = ink,
            surfaceVariant = surfaceSunk, onSurfaceVariant = inkMuted, outline = line, outlineVariant = line,
            error = loss, onError = onSignal, errorContainer = lossSoft,
        )
    }

private val LedgerlyTypography = Typography(
    displayLarge = LedgerlyTextStyles.display,
    headlineLarge = LedgerlyTextStyles.amountHero,
    headlineMedium = LedgerlyTextStyles.title,
    titleLarge = LedgerlyTextStyles.heading,
    bodyLarge = LedgerlyTextStyles.body,
    labelLarge = LedgerlyTextStyles.label,
    labelSmall = LedgerlyTextStyles.caption,
)
