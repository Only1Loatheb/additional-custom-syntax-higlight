package com.github.only1loatheb.additionalcustomsyntaxhiglight.settings

import com.github.only1loatheb.additionalcustomsyntaxhiglight.model.HighlightRule
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.util.UUID
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

class RuleEditDialog(
    project: Project?,
    private val existingRule: HighlightRule? = null
) : DialogWrapper(project) {

    private val nameField = JBTextField(existingRule?.name ?: "New Rule")
    private val patternField = JBTextField(existingRule?.pattern ?: "")
    private val fileTypesField = JBTextField(existingRule?.fileTypes ?: "*")
    private val caseSensitiveBox = JBCheckBox("Case sensitive", existingRule?.caseSensitive ?: false)
    private val enabledBox = JBCheckBox("Enabled", existingRule?.enabled ?: true)

    // Text color (foreground) - default true
    private val fgCheckBox = JBCheckBox("Text Color", existingRule?.foregroundColorHex != null || existingRule == null)
    private val fgColorPanel = ColorPanel().apply {
        selectedColor = HighlightRule.parseColor(existingRule?.foregroundColorHex) ?: Color(0xE0, 0x6C, 0x75)
    }

    // Background color
    private val bgCheckBox = JBCheckBox("Background Color", existingRule?.backgroundColorHex != null)
    private val bgColorPanel = ColorPanel().apply {
        selectedColor = HighlightRule.parseColor(existingRule?.backgroundColorHex) ?: Color(0x3E, 0x44, 0x51)
        isEnabled = bgCheckBox.isSelected
    }

    // Font styles
    private val boldBox = JBCheckBox("Bold", existingRule?.bold ?: false)
    private val italicBox = JBCheckBox("Italic", existingRule?.italic ?: false)

    // Effects
    private val effectComboBox = JComboBox(arrayOf("None", "Boxed", "Rounded Box", "Line Underscore", "Wave Underscore", "Strikeout")).apply {
        selectedItem = when (existingRule?.effectType?.uppercase()) {
            "BOXED" -> "Boxed"
            "ROUNDED_BOX" -> "Rounded Box"
            "LINE_UNDERSCORE" -> "Line Underscore"
            "WAVE_UNDERSCORE" -> "Wave Underscore"
            "STRIKEOUT" -> "Strikeout"
            else -> "None"
        }
    }
    private val effectColorPanel = ColorPanel().apply {
        selectedColor = HighlightRule.parseColor(existingRule?.effectColorHex) ?: Color(0x98, 0xC3, 0x79)
        isEnabled = effectComboBox.selectedIndex > 0
    }

    // Gutter error stripe
    private val stripeCheckBox = JBCheckBox("Gutter Stripe", existingRule?.errorStripeColorHex != null)
    private val stripeColorPanel = ColorPanel().apply {
        selectedColor = HighlightRule.parseColor(existingRule?.errorStripeColorHex) ?: Color(0xE5, 0xC0, 0x7B)
        isEnabled = stripeCheckBox.isSelected
    }

    // Live preview
    private val previewTextPane = JTextPane().apply {
        text = "The quick brown fox jumps over the lazy dog. TODO: inspect 0x1A3F"
        isEditable = true
        border = JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1)
        margin = JBUI.insets(6)
    }

    init {
        title = if (existingRule != null) "Edit Highlight Rule" else "Add Highlight Rule"
        setupListeners()
        init()
        updatePreview()
    }

    private fun setupListeners() {
        val docListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updatePreview()
            override fun removeUpdate(e: DocumentEvent?) = updatePreview()
            override fun changedUpdate(e: DocumentEvent?) = updatePreview()
        }
        patternField.document.addDocumentListener(docListener)

        fgCheckBox.addActionListener {
            fgColorPanel.isEnabled = fgCheckBox.isSelected
            updatePreview()
        }
        fgColorPanel.addActionListener { updatePreview() }

        bgCheckBox.addActionListener {
            bgColorPanel.isEnabled = bgCheckBox.isSelected
            updatePreview()
        }
        bgColorPanel.addActionListener { updatePreview() }

        boldBox.addActionListener { updatePreview() }
        italicBox.addActionListener { updatePreview() }

        effectComboBox.addActionListener {
            effectColorPanel.isEnabled = effectComboBox.selectedIndex > 0
            updatePreview()
        }
        effectColorPanel.addActionListener { updatePreview() }

        stripeCheckBox.addActionListener {
            stripeColorPanel.isEnabled = stripeCheckBox.isSelected
        }
        caseSensitiveBox.addActionListener { updatePreview() }
    }

    override fun createCenterPanel(): JComponent {
        val fgRow = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(fgCheckBox)
            add(fgColorPanel)
        }

        val bgRow = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(bgCheckBox)
            add(bgColorPanel)
        }

        val fontRow = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(boldBox)
            add(italicBox)
        }

        val effectRow = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(JBLabel("Effect:"))
            add(effectComboBox)
            add(effectColorPanel)
        }

        val stripeRow = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(stripeCheckBox)
            add(stripeColorPanel)
        }

        val previewPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(8)
            add(JBLabel("Live Preview (sample text, editable):"), BorderLayout.NORTH)
            add(JScrollPane(previewTextPane).apply {
                preferredSize = Dimension(450, 60)
            }, BorderLayout.CENTER)
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Rule Name:", nameField)
            .addLabeledComponent("Regex Pattern:", patternField)
            .addTooltip("Regular expression to match (e.g. \\b(TODO|FIXME)\\b or \\b0x[0-9a-fA-F]+\\b)")
            .addLabeledComponent("File Types:", fileTypesField)
            .addTooltip("Default '*' matches all file types/languages. Or specify comma-separated extensions (e.g. 'kt, java, py, md')")
            .addComponent(caseSensitiveBox)
            .addComponent(enabledBox)
            .addSeparator()
            .addLabeledComponent("Text Properties:", fgRow)
            .addComponent(bgRow)
            .addComponent(fontRow)
            .addComponent(effectRow)
            .addComponent(stripeRow)
            .addSeparator()
            .addComponent(previewPanel)
            .panel
    }

    private fun updatePreview() {
        val doc = previewTextPane.styledDocument
        val plainStyle = previewTextPane.addStyle("plain", null)
        StyleConstants.setForeground(plainStyle, JBUI.CurrentTheme.Label.foreground())
        StyleConstants.setBackground(plainStyle, previewTextPane.background)
        StyleConstants.setBold(plainStyle, false)
        StyleConstants.setItalic(plainStyle, false)
        doc.setCharacterAttributes(0, doc.length, plainStyle, true)

        val patternText = patternField.text
        if (patternText.isBlank()) return

        try {
            val flags = if (caseSensitiveBox.isSelected) 0 else (Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
            val pattern = Pattern.compile(patternText, flags)
            val matcher = pattern.matcher(previewTextPane.text)

            val matchStyle = previewTextPane.addStyle("match", null)
            if (fgCheckBox.isSelected && fgColorPanel.selectedColor != null) {
                StyleConstants.setForeground(matchStyle, fgColorPanel.selectedColor)
            }
            if (bgCheckBox.isSelected && bgColorPanel.selectedColor != null) {
                StyleConstants.setBackground(matchStyle, bgColorPanel.selectedColor)
            }
            StyleConstants.setBold(matchStyle, boldBox.isSelected)
            StyleConstants.setItalic(matchStyle, italicBox.isSelected)
            if (effectComboBox.selectedItem == "Strikeout") {
                StyleConstants.setStrikeThrough(matchStyle, true)
            }
            if (effectComboBox.selectedItem == "Line Underscore" || effectComboBox.selectedItem == "Wave Underscore") {
                StyleConstants.setUnderline(matchStyle, true)
            }

            while (matcher.find()) {
                val start = matcher.start()
                val len = matcher.end() - start
                if (start >= 0 && start + len <= doc.length) {
                    doc.setCharacterAttributes(start, len, matchStyle, false)
                }
            }
        } catch (_: Exception) {
            // Ignore syntax error during typing in preview
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (nameField.text.isBlank()) {
            return ValidationInfo("Please enter a rule name", nameField)
        }
        val patternText = patternField.text
        if (patternText.isBlank()) {
            return ValidationInfo("Please enter a regex pattern", patternField)
        }
        try {
            val flags = if (caseSensitiveBox.isSelected) 0 else (Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
            Pattern.compile(patternText, flags)
        } catch (e: PatternSyntaxException) {
            return ValidationInfo("Invalid regex pattern: ${e.description}", patternField)
        }
        return null
    }

    fun buildRule(): HighlightRule {
        val effect = when (effectComboBox.selectedItem) {
            "Boxed" -> "BOXED"
            "Rounded Box" -> "ROUNDED_BOX"
            "Line Underscore" -> "LINE_UNDERSCORE"
            "Wave Underscore" -> "WAVE_UNDERSCORE"
            "Strikeout" -> "STRIKEOUT"
            else -> null
        }

        return HighlightRule(
            id = existingRule?.id ?: UUID.randomUUID().toString(),
            name = nameField.text.trim(),
            pattern = patternField.text,
            fileTypes = fileTypesField.text.trim().ifEmpty { "*" },
            caseSensitive = caseSensitiveBox.isSelected,
            enabled = enabledBox.isSelected,
            foregroundColorHex = if (fgCheckBox.isSelected) HighlightRule.toHex(fgColorPanel.selectedColor) else null,
            backgroundColorHex = if (bgCheckBox.isSelected) HighlightRule.toHex(bgColorPanel.selectedColor) else null,
            bold = boldBox.isSelected,
            italic = italicBox.isSelected,
            effectType = effect,
            effectColorHex = if (effect != null) HighlightRule.toHex(effectColorPanel.selectedColor) else null,
            errorStripeColorHex = if (stripeCheckBox.isSelected) HighlightRule.toHex(stripeColorPanel.selectedColor) else null
        )
    }
}
