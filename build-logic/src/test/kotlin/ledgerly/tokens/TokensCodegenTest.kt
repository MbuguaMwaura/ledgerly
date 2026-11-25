package ledgerly.tokens

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokensCodegenTest {
    private val tokens = DesignTokens.parse(File("../designTokens/tokens.json").readText())

    @Test
    fun parsesLightAndDarkColours() {
        val canvas = tokens.colors.single { it.name == "canvas" }
        assertEquals(0xFFF2F2F4, canvas.light)
        assertEquals(0xFF0E0F13, canvas.dark)
    }

    @Test
    fun parsesTypeSpacingAndRadii() {
        val hero = tokens.textStyles.single { it.name == "amount-hero" }
        assertEquals(40.0, hero.fontSize)
        assertEquals(-0.035, hero.letterSpacingEm)
        assertEquals(20.0, tokens.spacing.single { it.name == "space-5" }.value)
        assertEquals(999.0, tokens.radii.single { it.name == "radius-pill" }.value)
    }

    @Test
    fun generatesCamelCasedKotlin() {
        val source = TokensCodegen.kotlin(tokens, "app.ledgerly.tokens")
        assertTrue("surfaceSunk = Color(0xFFF6F6F8)" in source)
        assertTrue("val amountHero = TextStyle(fontSize = 40.sp" in source)
        assertTrue("val radiusPill = 999.dp" in source)
    }

    @Test
    fun generatesSwift() {
        val source = TokensCodegen.swift(tokens)
        assertTrue("static let canvas = Color(light: 0xFFF2F2F4, dark: 0xFF0E0F13)" in source)
        assertTrue("static let space5: CGFloat = 20" in source)
    }
}
