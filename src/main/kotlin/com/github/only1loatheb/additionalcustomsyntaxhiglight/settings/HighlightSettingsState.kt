package com.github.only1loatheb.additionalcustomsyntaxhiglight.settings

import com.github.only1loatheb.additionalcustomsyntaxhiglight.model.HighlightRule
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Topic notified whenever highlight rules are modified.
 */
fun interface HighlightSettingsChangeListener {
    fun settingsChanged(rules: List<HighlightRule>)

    companion object {
        val TOPIC = Topic.create("CustomRegexHighlightSettingsChanged", HighlightSettingsChangeListener::class.java)
    }
}

/**
 * Application-level persistent state for custom regex highlighting rules.
 */
@Service(Service.Level.APP)
@State(
    name = "CustomRegexHighlightSettings",
    storages = [Storage("CustomRegexHighlightSettings.xml")]
)
class HighlightSettingsState : PersistentStateComponent<HighlightSettingsState.State> {

    class State {
        var rules: MutableList<HighlightRule> = mutableListOf(
            HighlightRule(
                name = "Arrow (==>)",
                pattern = "==>",
                fileTypes = "*",
                caseSensitive = true,
                enabled = true,
                foregroundColorHex = "#E5C07B",
                bold = true
            ),
            HighlightRule(
                name = "Case (`case`)",
                pattern = "(`case`|\\bcase\\b)",
                fileTypes = "*",
                caseSensitive = false,
                enabled = true,
                foregroundColorHex = "#E06C75",
                bold = true
            ),
            HighlightRule(
                name = "EndCase (.endCase)",
                pattern = "\\.endCase\\b",
                fileTypes = "*",
                caseSensitive = false,
                enabled = true,
                foregroundColorHex = "#61AFEF",
                bold = true
            )
        )
    }


    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    var rules: List<HighlightRule>
        get() = myState.rules
        set(value) {
            myState.rules = value.toMutableList()
            notifyChanged()
        }

    fun notifyChanged() {
        ApplicationManager.getApplication().messageBus
            .syncPublisher(HighlightSettingsChangeListener.TOPIC)
            .settingsChanged(rules)
    }

    companion object {
        fun getInstance(): HighlightSettingsState = ApplicationManager.getApplication().service<HighlightSettingsState>()
    }
}
