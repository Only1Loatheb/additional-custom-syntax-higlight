package com.github.only1loatheb.additionalcustomsyntaxhiglight.startup

import com.github.only1loatheb.additionalcustomsyntaxhiglight.highlight.RegexHighlightService
import com.github.only1loatheb.additionalcustomsyntaxhiglight.settings.HighlightSettingsChangeListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class RegexProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Highlight open editors on project startup
        RegexHighlightService.getInstance().updateAllEditors()

        // Listen for settings changes and update open editors
        val connection = project.messageBus.connect()
        connection.subscribe(
            HighlightSettingsChangeListener.TOPIC,
            HighlightSettingsChangeListener {
                RegexHighlightService.getInstance().updateAllEditors()
            }
        )
    }
}
