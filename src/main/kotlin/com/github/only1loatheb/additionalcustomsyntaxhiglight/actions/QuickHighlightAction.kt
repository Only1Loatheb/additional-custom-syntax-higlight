package com.github.only1loatheb.additionalcustomsyntaxhiglight.actions

import com.github.only1loatheb.additionalcustomsyntaxhiglight.model.HighlightRule
import com.github.only1loatheb.additionalcustomsyntaxhiglight.settings.HighlightSettingsState
import com.github.only1loatheb.additionalcustomsyntaxhiglight.settings.RuleEditDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.regex.Pattern

/**
 * Context action in editor to quickly create a regex highlight rule from selected text.
 */
class QuickHighlightAction : AnAction("Add Regex Highlight...") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = editor != null
        if (hasSelection) {
            val selectedText = editor.selectionModel.selectedText?.trim() ?: ""
            val truncated = if (selectedText.length > 20) selectedText.take(20) + "..." else selectedText
            e.presentation.text = "Add Regex Highlight for '$truncated'..."
        } else {
            e.presentation.text = "Add Regex Highlight..."
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText?.trim() ?: ""

        val initialPattern = if (selectedText.isNotEmpty()) {
            if (selectedText.matches(Regex("[a-zA-Z0-9_]+"))) {
                "\\b${selectedText}\\b"
            } else {
                Pattern.quote(selectedText)
            }
        } else {
            ""
        }

        val initialRule = HighlightRule(
            name = if (selectedText.isNotEmpty()) "Highlight $selectedText" else "New Rule",
            pattern = initialPattern,
            fileTypes = "*",
            caseSensitive = false,
            enabled = true,
            foregroundColorHex = "#E06C75"
        )

        val dialog = RuleEditDialog(project, initialRule)
        if (dialog.showAndGet()) {
            val newRule = dialog.buildRule()
            val state = HighlightSettingsState.getInstance()
            val updated = state.rules.toMutableList()
            updated.add(newRule)
            state.rules = updated
        }
    }
}
