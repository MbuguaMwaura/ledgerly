package ledgerly.tokens

import groovy.json.JsonSlurper

/** The parts of the Ledgerly Clean `tokens.json` that the apps use. */
data class DesignTokens(
    val colors: List<ColorToken>,
    val textStyles: List<TextStyleToken>,
    val spacing: List<LengthToken>,
    val radii: List<LengthToken>,
) {
    data class ColorToken(val name: String, val light: Long, val dark: Long, val usage: String)

    data class TextStyleToken(
        val name: String,
        val fontSize: Double,
        val lineHeight: Double,
        val fontWeight: Int,
        /** Tracking in em, e.g. -0.035. */
        val letterSpacingEm: Double,
        val usage: String,
    )

    data class LengthToken(val name: String, val value: Double, val usage: String)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun parse(json: String): DesignTokens {
            val root = JsonSlurper().parseText(json) as Map<String, Any?>

            val colorFamily = root["color"] as Map<String, Any?>
            val themes = (colorFamily["themes"] as List<Map<String, Any?>>).map { it["id"] as String }
            require(themes.firstOrNull() == "light") { "The first colour theme must be light, found $themes" }
            val colors = (colorFamily["tokens"] as List<Map<String, Any?>>).map { token ->
                val name = token["name"] as String
                val (light, dark) = when (val value = token["value"]) {
                    is String -> parseHex(name, value).let { it to it }
                    is Map<*, *> -> {
                        val light = parseHex(name, value["light"] as String)
                        val dark = (value["dark"] as String?)?.let { parseHex(name, it) } ?: light
                        light to dark
                    }
                    else -> error("Colour token $name has an unsupported value: $value")
                }
                ColorToken(name, light, dark, token["usage"] as String? ?: "")
            }

            val type = root["type"] as Map<String, Any?>
            val textStyles = (type["groups"] as List<Map<String, Any?>>).flatMap { group ->
                (group["styles"] as List<Map<String, Any?>>).map { style ->
                    val name = style["name"] as String
                    val fontSize = px(name, style["fontSize"])
                    TextStyleToken(
                        name = name,
                        fontSize = fontSize,
                        lineHeight = px(name, style["lineHeight"]),
                        fontWeight = (style["fontWeight"] as Number).toInt(),
                        letterSpacingEm = em(name, style["letterSpacing"], fontSize),
                        usage = style["usage"] as String? ?: "",
                    )
                }
            }

            fun lengths(family: String): List<LengthToken> =
                ((root[family] as Map<String, Any?>?)?.get("tokens") as List<Map<String, Any?>>? ?: emptyList()).map {
                    val name = it["name"] as String
                    LengthToken(name, px(name, it["value"]), it["usage"] as String? ?: "")
                }

            return DesignTokens(colors, textStyles, lengths("spacing"), lengths("radius"))
        }

        private fun parseHex(name: String, value: String): Long {
            val hex = value.removePrefix("#")
            return when (hex.length) {
                6 -> ("FF$hex").toLong(16)
                // tokens.json writes alpha last (#rrggbbaa); store as AARRGGBB.
                8 -> (hex.substring(6) + hex.substring(0, 6)).toLong(16)
                else -> error("Colour token $name must be #rrggbb or #rrggbbaa, found $value")
            }
        }

        private fun px(name: String, value: Any?): Double = when (value) {
            is Number -> value.toDouble()
            is String -> value.removeSuffix("px").toDoubleOrNull() ?: error("Token $name: expected px, found $value")
            else -> error("Token $name: expected a length, found $value")
        }

        private fun em(name: String, value: Any?, fontSize: Double): Double = when (value) {
            null -> 0.0
            is Number -> value.toDouble()
            is String -> when {
                value.endsWith("em") -> value.removeSuffix("em").toDouble()
                value.endsWith("px") -> value.removeSuffix("px").toDouble() / fontSize
                else -> error("Token $name: letterSpacing must be em or px, found $value")
            }
            else -> error("Token $name: unsupported letterSpacing $value")
        }
    }
}

/** `surface-sunk` -> `surfaceSunk`, `space-1` -> `space1`. */
internal fun String.toCamelCase(): String =
    split('-', '_', '.').filter { it.isNotEmpty() }.mapIndexed { i, part ->
        if (i == 0) part else part.replaceFirstChar { it.uppercase() }
    }.joinToString("")
