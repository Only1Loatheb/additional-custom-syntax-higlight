package com.github.only1loatheb.additionalcustomsyntaxhiglight.model

import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color
import java.awt.Font
import java.util.UUID

/**
 * Represents a single custom regex highlight rule.
 */
data class HighlightRule(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "New Rule",
    var pattern: String = "",
    var fileTypes: String = "*",
    var caseSensitive: Boolean = false,
    var enabled: Boolean = true,

    // Text color (foreground) - primary default
    var foregroundColorHex: String? = "#E06C75",
    var backgroundColorHex: String? = null,

    // Font style
    var bold: Boolean = false,
    var italic: Boolean = false,

    // Text decorations / effects
    var effectType: String? = null, // e.g. "BOXED", "ROUNDED_BOX", "LINE_UNDERSCORE", "WAVE_UNDERSCORE", "STRIKEOUT"
    var effectColorHex: String? = null,
    var errorStripeColorHex: String? = null
) {
    /**
     * Checks whether this rule should apply to the given virtual file.
     * Default "*" matches all files / languages.
     */
    fun matchesFile(file: VirtualFile?): Boolean {
        val trimmed = fileTypes.trim()
        if (trimmed.isEmpty() || trimmed == "*") return true
        if (file == null) return false

        val extension = file.extension?.lowercase() ?: ""
        val fileName = file.name.lowercase()
        val fileTypeName = file.fileType.name.lowercase()

        val patterns = trimmed.split(',', ';').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        for (p in patterns) {
            if (p == "*") return true
            val normalizedPattern = if (p.startsWith("*.")) p.substring(2) else if (p.startsWith(".")) p.substring(1) else p
            if (extension.isNotEmpty() && extension == normalizedPattern) return true
            if (fileName == normalizedPattern || fileName.endsWith(".$normalizedPattern")) return true
            if (fileTypeName == normalizedPattern) return true
        }
        return false
    }

    /**
     * Converts this rule's color and style settings into IntelliJ TextAttributes.
     */
    fun toTextAttributes(): TextAttributes {
        val fgColor = parseColor(foregroundColorHex)
        val bgColor = parseColor(backgroundColorHex)
        val effColor = parseColor(effectColorHex)

        val fontType = when {
            bold && italic -> Font.BOLD or Font.ITALIC
            bold -> Font.BOLD
            italic -> Font.ITALIC
            else -> Font.PLAIN
        }

        val effType = when (effectType?.uppercase()) {
            "BOXED" -> EffectType.BOXED
            "ROUNDED_BOX" -> EffectType.ROUNDED_BOX
            "LINE_UNDERSCORE" -> EffectType.LINE_UNDERSCORE
            "WAVE_UNDERSCORE" -> EffectType.WAVE_UNDERSCORE
            "STRIKEOUT" -> EffectType.STRIKEOUT
            else -> null
        }

        val attributes = TextAttributes(fgColor, bgColor, effColor, effType, fontType)
        parseColor(errorStripeColorHex)?.let {
            attributes.errorStripeColor = it
        }
        return attributes
    }

    companion object {
        fun parseColor(hex: String?): Color? {
            if (hex.isNullOrBlank()) return null
            val cleanHex = hex.trim().removePrefix("#")
            return try {
                when (cleanHex.length) {
                    6 -> Color(cleanHex.toInt(16))
                    8 -> {
                        val argb = cleanHex.toLong(16).toInt()
                        Color(argb, true)
                    }
                    else -> null
                }
            } catch (_: NumberFormatException) {
                null
            }
        }

        fun toHex(color: Color?): String? {
            if (color == null) return null
            return String.format("#%02X%02X%02X", color.red, color.green, color.blue)
        }
    }
}
