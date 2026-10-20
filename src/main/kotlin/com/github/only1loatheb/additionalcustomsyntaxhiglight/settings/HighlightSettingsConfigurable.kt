package com.github.only1loatheb.additionalcustomsyntaxhiglight.settings

import com.github.only1loatheb.additionalcustomsyntaxhiglight.model.HighlightRule
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnActionButton
import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.ColorUtil
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.UUID
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class HighlightSettingsConfigurable : SearchableConfigurable {

    private val rulesList = mutableListOf<HighlightRule>()
    private val tableModel = RulesTableModel(rulesList)
    private val table = JBTable(tableModel)
    private var mainPanel: JPanel? = null

    override fun getId(): String = "com.github.only1loatheb.additionalcustomsyntaxhiglight.settings"

    override fun getDisplayName(): String = "Custom Regex Highlighting"

    override fun createComponent(): JComponent {
        table.setShowGrid(true)
        table.rowHeight = JBUI.scale(26)

        // Enabled checkbox column
        table.columnModel.getColumn(0).apply {
            maxWidth = JBUI.scale(60)
            preferredWidth = JBUI.scale(60)
            cellRenderer = BooleanTableCellRenderer()
            cellEditor = BooleanTableCellEditor()
        }

        // Color renderer
        table.columnModel.getColumn(4).cellRenderer = ColorCellRenderer()
        table.columnModel.getColumn(5).cellRenderer = ColorCellRenderer()

        // Double click to edit
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && table.selectedRow != -1) {
                    editSelectedRule()
                }
            }
        })

        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                val dialog = RuleEditDialog(null)
                if (dialog.showAndGet()) {
                    rulesList.add(dialog.buildRule())
                    tableModel.fireTableDataChanged()
                    val newRow = rulesList.size - 1
                    table.setRowSelectionInterval(newRow, newRow)
                }
            }
            .setEditAction { editSelectedRule() }
            .setRemoveAction {
                val selectedIndex = table.selectedRow
                if (selectedIndex in rulesList.indices) {
                    rulesList.removeAt(selectedIndex)
                    tableModel.fireTableDataChanged()
                    if (rulesList.isNotEmpty()) {
                        val nextSelect = selectedIndex.coerceAtMost(rulesList.size - 1)
                        table.setRowSelectionInterval(nextSelect, nextSelect)
                    }
                }
            }
            .addExtraAction(object : AnActionButton("Duplicate", AllIcons.Actions.Copy) {
                override fun actionPerformed(e: AnActionEvent) {
                    val selectedIndex = table.selectedRow
                    if (selectedIndex in rulesList.indices) {
                        val original = rulesList[selectedIndex]
                        val duplicate = original.copy(
                            id = UUID.randomUUID().toString(),
                            name = "${original.name} (Copy)"
                        )
                        rulesList.add(selectedIndex + 1, duplicate)
                        tableModel.fireTableDataChanged()
                        table.setRowSelectionInterval(selectedIndex + 1, selectedIndex + 1)
                    }
                }
            })
            .addExtraAction(object : AnActionButton("Reset to Defaults", AllIcons.Actions.Rollback) {
                override fun actionPerformed(e: AnActionEvent) {
                    val result = Messages.showYesNoDialog(
                        "Reset all highlight rules to default settings?",
                        "Reset Rules",
                        Messages.getQuestionIcon()
                    )
                    if (result == Messages.YES) {
                        rulesList.clear()
                        rulesList.addAll(HighlightSettingsState.State().rules)
                        tableModel.fireTableDataChanged()
                    }
                }
            })

        val panel = JPanel(BorderLayout())
        panel.add(decorator.createPanel(), BorderLayout.CENTER)
        mainPanel = panel
        return panel
    }

    private fun editSelectedRule() {
        val selectedIndex = table.selectedRow
        if (selectedIndex in rulesList.indices) {
            val rule = rulesList[selectedIndex]
            val dialog = RuleEditDialog(null, rule)
            if (dialog.showAndGet()) {
                rulesList[selectedIndex] = dialog.buildRule()
                tableModel.fireTableRowsUpdated(selectedIndex, selectedIndex)
            }
        }
    }

    override fun isModified(): Boolean {
        val currentSaved = HighlightSettingsState.getInstance().rules
        return rulesList != currentSaved
    }

    override fun apply() {
        HighlightSettingsState.getInstance().rules = rulesList.map { it.copy() }
    }

    override fun reset() {
        rulesList.clear()
        rulesList.addAll(HighlightSettingsState.getInstance().rules.map { it.copy() })
        tableModel.fireTableDataChanged()
    }

    override fun disposeUIResources() {
        mainPanel = null
    }

    private class RulesTableModel(private val rules: List<HighlightRule>) : AbstractTableModel() {
        private val columnNames = arrayOf("Enabled", "Name", "Regex Pattern", "File Types", "Text Color", "Background", "Style")

        override fun getRowCount(): Int = rules.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> Boolean::class.javaObjectType
                else -> String::class.java
            }
        }


        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val rule = rules[rowIndex]
            return when (columnIndex) {
                0 -> rule.enabled
                1 -> rule.name
                2 -> rule.pattern
                3 -> rule.fileTypes
                4 -> rule.foregroundColorHex ?: "(None)"
                5 -> rule.backgroundColorHex ?: "(None)"
                6 -> {
                    val styles = mutableListOf<String>()
                    if (rule.bold) styles.add("Bold")
                    if (rule.italic) styles.add("Italic")
                    rule.effectType?.let { styles.add(it) }
                    if (styles.isEmpty()) "Plain" else styles.joinToString(", ")
                }
                else -> ""
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 0 && aValue is Boolean) {
                rules[rowIndex].enabled = aValue
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }
    }

    private class ColorCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
            val hex = value as? String
            val color = HighlightRule.parseColor(hex)
            if (color != null) {
                label.icon = ColorSwatchIcon(color)
                label.text = hex
            } else {
                label.icon = null
                label.text = hex ?: "(None)"
            }
            return label
        }
    }

    private class ColorSwatchIcon(private val color: Color) : Icon {
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            g.color = color
            g.fillRect(x, y + 2, iconWidth, iconHeight - 4)
            g.color = Color.DARK_GRAY
            g.drawRect(x, y + 2, iconWidth, iconHeight - 4)
        }

        override fun getIconWidth(): Int = JBUI.scale(14)
        override fun getIconHeight(): Int = JBUI.scale(14)
    }
}
