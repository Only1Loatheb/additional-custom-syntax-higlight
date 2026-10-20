package com.github.only1loatheb.additionalcustomsyntaxhiglight

import com.github.only1loatheb.additionalcustomsyntaxhiglight.highlight.RegexHighlightService
import com.github.only1loatheb.additionalcustomsyntaxhiglight.model.HighlightRule
import com.github.only1loatheb.additionalcustomsyntaxhiglight.settings.HighlightSettingsState
import com.intellij.ide.highlighter.XmlFileType

import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Color
import java.awt.Font

class HighlightPluginTest : BasePlatformTestCase() {

    fun testRuleMatchesFile() {
        val psiFile = myFixture.configureByText("Sample.kt", "fun main() {}")
        val vFile = psiFile.virtualFile

        // Default "*" matches all files
        val ruleAll = HighlightRule(fileTypes = "*")
        assertTrue(ruleAll.matchesFile(vFile))

        // Empty fileTypes also matches all files
        val ruleEmpty = HighlightRule(fileTypes = "")
        assertTrue(ruleEmpty.matchesFile(vFile))

        // Matching extension
        val ruleKt = HighlightRule(fileTypes = "kt")
        assertTrue(ruleKt.matchesFile(vFile))

        // Comma-separated list
        val ruleMulti = HighlightRule(fileTypes = "java, kt, py")
        assertTrue(ruleMulti.matchesFile(vFile))

        // Non-matching extension
        val ruleJava = HighlightRule(fileTypes = "java, py")
        assertFalse(ruleJava.matchesFile(vFile))

        // Glob pattern
        val ruleGlob = HighlightRule(fileTypes = "*.kt")
        assertTrue(ruleGlob.matchesFile(vFile))
    }

    fun testRuleToTextAttributes() {
        val rule = HighlightRule(
            foregroundColorHex = "#FF0000",
            backgroundColorHex = "#00FF00",
            bold = true,
            italic = true,
            effectType = "BOXED",
            effectColorHex = "#0000FF",
            errorStripeColorHex = "#FFFF00"
        )

        val attrs = rule.toTextAttributes()
        assertEquals(Color(0xFF, 0x00, 0x00), attrs.foregroundColor)
        assertEquals(Color(0x00, 0xFF, 0x00), attrs.backgroundColor)
        assertEquals(Font.BOLD or Font.ITALIC, attrs.fontType)
        assertEquals(EffectType.BOXED, attrs.effectType)
        assertEquals(Color(0x00, 0x00, 0xFF), attrs.effectColor)
        assertEquals(Color(0xFF, 0xFF, 0x00), attrs.errorStripeColor)
    }

    fun testSettingsStatePersistence() {
        val settings = HighlightSettingsState.getInstance()
        val originalRules = settings.rules

        try {
            val customRule = HighlightRule(
                name = "TestRule",
                pattern = "\\btest\\b",
                fileTypes = "kt",
                foregroundColorHex = "#123456"
            )
            settings.rules = listOf(customRule)

            val loadedState = HighlightSettingsState.State()
            loadedState.rules = mutableListOf(customRule)

            settings.loadState(loadedState)
            assertEquals(1, settings.rules.size)
            assertEquals("TestRule", settings.rules[0].name)
            assertEquals("\\btest\\b", settings.rules[0].pattern)
            assertEquals("#123456", settings.rules[0].foregroundColorHex)
        } finally {
            settings.rules = originalRules
        }
    }

    fun testRegexHighlightingInEditor() {
        val settings = HighlightSettingsState.getInstance()
        val originalRules = settings.rules

        try {
            val rule = HighlightRule(
                name = "Highlight Word Foo",
                pattern = "\\bfoo\\b",
                fileTypes = "*",
                caseSensitive = false,
                enabled = true,
                foregroundColorHex = "#FF5555"
            )
            settings.rules = listOf(rule)

            myFixture.configureByText("Bar.java", "public class Bar { int foo = 42; String s = \"foo inside\"; }")
            val editor = myFixture.editor


            // Perform highlight
            RegexHighlightService.getInstance().highlightEditor(editor)

            // Allow EDT tasks or ReadActions to execute
            myFixture.doHighlighting()

            val highlighters = editor.getUserData(RegexHighlightService.HIGHLIGHTERS_KEY)
            assertNotNull("Highlighters should be attached to editor", highlighters)
            assertTrue("Should find at least 2 occurrences of 'foo'", highlighters!!.size >= 2)

            for (h in highlighters) {
                assertEquals(HighlighterLayer.ADDITIONAL_SYNTAX + 100, h.layer)
                assertEquals(Color(0xFF, 0x55, 0x55), h.getTextAttributes(editor.colorsScheme)?.foregroundColor)
            }

        } finally {
            settings.rules = originalRules
        }
    }

    fun testInvalidRegexDoesNotThrow() {
        val settings = HighlightSettingsState.getInstance()
        val originalRules = settings.rules

        try {
            val brokenRule = HighlightRule(
                name = "Broken Regex",
                pattern = "[unclosed bracket",
                fileTypes = "*",
                enabled = true
            )
            settings.rules = listOf(brokenRule)

            myFixture.configureByText("sample.txt", "Some random text")
            // Should execute gracefully without throwing PatternSyntaxException
            RegexHighlightService.getInstance().highlightEditor(myFixture.editor)
        } finally {
            settings.rules = originalRules
        }
    }
}
