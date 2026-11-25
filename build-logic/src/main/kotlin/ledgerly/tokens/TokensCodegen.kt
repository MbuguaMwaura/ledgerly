package ledgerly.tokens

import java.util.Locale

/** Turns [DesignTokens] into source files for the two apps. */
object TokensCodegen {
    private const val HEADER = "Generated from designTokens/tokens.json. Do not edit by hand."

    fun kotlin(tokens: DesignTokens, packageName: String): String = buildString {
        appendLine("// $HEADER")
        appendLine("// Regenerated on every Android build.")
        appendLine("package $packageName")
        appendLine()
        appendLine("import androidx.compose.runtime.Immutable")
        appendLine("import androidx.compose.ui.graphics.Color")
        appendLine("import androidx.compose.ui.text.TextStyle")
        appendLine("import androidx.compose.ui.text.font.FontWeight")
        appendLine("import androidx.compose.ui.unit.dp")
        appendLine("import androidx.compose.ui.unit.em")
        appendLine("import androidx.compose.ui.unit.sp")
        appendLine()
        appendLine("@Immutable")
        appendLine("class LedgerlyColors(")
        tokens.colors.forEach { appendLine("    /** ${it.usage.kdoc()} */\n    val ${it.name.toCamelCase()}: Color,") }
        appendLine(")")
        appendLine()
        for ((label, pick) in listOf<Pair<String, (DesignTokens.ColorToken) -> Long>>("Light" to { it.light }, "Dark" to { it.dark })) {
            appendLine("val Ledgerly${label}Colors = LedgerlyColors(")
            tokens.colors.forEach { appendLine("    ${it.name.toCamelCase()} = Color(0x${pick(it).hex8()}),") }
            appendLine(")")
            appendLine()
        }
        appendLine("object LedgerlyTextStyles {")
        tokens.textStyles.forEach {
            appendLine("    /** ${it.usage.kdoc()} */")
            appendLine(
                "    val ${it.name.toCamelCase()} = TextStyle(fontSize = ${it.fontSize.num()}.sp, " +
                    "lineHeight = ${it.lineHeight.num()}.sp, fontWeight = FontWeight(${it.fontWeight}), " +
                    "letterSpacing = (${it.letterSpacingEm.num()}).em)",
            )
        }
        appendLine("}")
        appendLine()
        appendLine("object LedgerlySpacing {")
        tokens.spacing.forEach { appendLine("    /** ${it.usage.kdoc()} */\n    val ${it.name.toCamelCase()} = ${it.value.num()}.dp") }
        appendLine("}")
        appendLine()
        appendLine("object LedgerlyRadius {")
        tokens.radii.forEach { appendLine("    /** ${it.usage.kdoc()} */\n    val ${it.name.toCamelCase()} = ${it.value.num()}.dp") }
        appendLine("}")
    }

    fun swift(tokens: DesignTokens): String = buildString {
        appendLine("// $HEADER")
        appendLine("// Regenerate with: ./gradlew :designTokens:generateSwiftTokens")
        appendLine()
        appendLine("import SwiftUI")
        appendLine()
        appendLine("enum LedgerlyColor {")
        tokens.colors.forEach {
            appendLine("    /// ${it.usage}")
            appendLine("    static let ${it.name.toCamelCase()} = Color(light: 0x${it.light.hex8()}, dark: 0x${it.dark.hex8()})")
        }
        appendLine("}")
        appendLine()
        appendLine("enum LedgerlyText {")
        tokens.textStyles.forEach {
            appendLine("    /// ${it.usage}")
            appendLine(
                "    static let ${it.name.toCamelCase()} = LedgerlyTextStyle(size: ${it.fontSize.num()}, " +
                    "lineHeight: ${it.lineHeight.num()}, weight: ${it.fontWeight}, trackingEm: ${it.letterSpacingEm.num()})",
            )
        }
        appendLine("}")
        appendLine()
        appendLine("enum LedgerlySpacing {")
        tokens.spacing.forEach { appendLine("    /// ${it.usage}\n    static let ${it.name.toCamelCase()}: CGFloat = ${it.value.num()}") }
        appendLine("}")
        appendLine()
        appendLine("enum LedgerlyRadius {")
        tokens.radii.forEach { appendLine("    /// ${it.usage}\n    static let ${it.name.toCamelCase()}: CGFloat = ${it.value.num()}") }
        appendLine("}")
    }

    private fun Long.hex8() = String.format(Locale.ROOT, "%08X", this)

    private fun Double.num(): String =
        if (this == Math.floor(this) && !isInfinite()) toLong().toString() else toString()

    private fun String.kdoc() = replace("*/", "* /")
}
