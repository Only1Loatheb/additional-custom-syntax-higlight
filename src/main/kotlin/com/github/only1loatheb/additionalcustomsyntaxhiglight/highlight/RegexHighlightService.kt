package com.github.only1loatheb.additionalcustomsyntaxhiglight.highlight

import com.github.only1loatheb.additionalcustomsyntaxhiglight.model.HighlightRule
import com.github.only1loatheb.additionalcustomsyntaxhiglight.settings.HighlightSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Service(Service.Level.APP)
class RegexHighlightService {

    private val logger = Logger.getInstance(RegexHighlightService::class.java)

    internal data class MatchResult(
        val range: TextRange,
        val attributes: TextAttributes
    )


    /**
     * Highlights the given editor based on active rules matching the editor's file.
     */
    fun highlightEditor(editor: Editor) {
        if (editor.isDisposed) return

        val project = editor.project
        val document = editor.document
        val virtualFile: VirtualFile? = FileDocumentManager.getInstance().getFile(document) ?: editor.virtualFile

        val allRules = HighlightSettingsState.getInstance().rules
        val applicableRules = allRules.filter { it.enabled && it.pattern.isNotBlank() && it.matchesFile(virtualFile) }

        if (applicableRules.isEmpty()) {
            clearHighlights(editor)
            return
        }

        var actionBuilder = ReadAction.nonBlocking<List<MatchResult>> {
            if (editor.isDisposed) return@nonBlocking emptyList()
            val text = document.immutableCharSequence
            val results = mutableListOf<MatchResult>()

            for (rule in applicableRules) {
                try {
                    val flags = if (rule.caseSensitive) 0 else (Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
                    val pattern = Pattern.compile(rule.pattern, flags)
                    val matcher = pattern.matcher(text)
                    val attributes = rule.toTextAttributes()

                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        if (start in 0..end && end <= text.length) {
                            results.add(MatchResult(TextRange(start, end), attributes))
                        }
                    }
                } catch (e: PatternSyntaxException) {
                    logger.debug("Invalid regex in rule '${rule.name}': ${e.message}")
                } catch (e: Throwable) {
                    logger.warn("Error matching regex in rule '${rule.name}': ${e.message}")
                }
            }
            results
        }

        if (project != null) {
            actionBuilder = actionBuilder.inSmartMode(project)
        }

        actionBuilder
            .expireWhen { editor.isDisposed }
            .finishOnUiThread(ApplicationManager.getApplication().defaultModalityState) { matches ->
                if (editor.isDisposed) return@finishOnUiThread
                applyMatchesToEditor(editor, matches)
            }
            .submit(AppExecutorUtil.getAppExecutorService())

    }

    /**
     * Applies computed highlight matches to the editor's markup model on EDT.
     */
    private fun applyMatchesToEditor(editor: Editor, matches: List<MatchResult>) {
        if (editor.isDisposed) return


        val markupModel = editor.markupModel
        val previousHighlighters = editor.getUserData(HIGHLIGHTERS_KEY)

        // Clear previous highlighters
        previousHighlighters?.forEach { highlighter ->
            if (highlighter.isValid) {
                markupModel.removeHighlighter(highlighter)
            }
        }

        val docLength = editor.document.textLength
        val newHighlighters = mutableListOf<RangeHighlighter>()

        for (match in matches) {
            val start = match.range.startOffset
            val end = match.range.endOffset
            if (start < end && end <= docLength) {
                try {
                    val highlighter = markupModel.addRangeHighlighter(
                        start,
                        end,
                        HighlighterLayer.ADDITIONAL_SYNTAX + 100,
                        match.attributes,
                        HighlighterTargetArea.EXACT_RANGE
                    )
                    newHighlighters.add(highlighter)
                } catch (e: Exception) {
                    logger.debug("Failed to add range highlighter: ${e.message}")
                }
            }
        }

        editor.putUserData(HIGHLIGHTERS_KEY, newHighlighters)
    }

    /**
     * Clears all custom highlighters from the editor.
     */
    fun clearHighlights(editor: Editor) {
        if (editor.isDisposed) return

        val runnable = Runnable {
            if (editor.isDisposed) return@Runnable
            val markupModel = editor.markupModel
            val previousHighlighters = editor.getUserData(HIGHLIGHTERS_KEY)
            previousHighlighters?.forEach { highlighter ->
                if (highlighter.isValid) {
                    markupModel.removeHighlighter(highlighter)
                }
            }
            editor.putUserData(HIGHLIGHTERS_KEY, null)
        }

        if (ApplicationManager.getApplication().isDispatchThread) {
            runnable.run()
        } else {
            ApplicationManager.getApplication().invokeLater(runnable)
        }
    }

    /**
     * Refreshes highlights for all currently open editors.
     */
    fun updateAllEditors() {
        val editors = EditorFactory.getInstance().allEditors
        for (editor in editors) {
            highlightEditor(editor)
        }
    }

    companion object {
        val HIGHLIGHTERS_KEY = Key.create<List<RangeHighlighter>>("com.github.only1loatheb.additionalcustomsyntaxhiglight.highlighters")

        fun getInstance(): RegexHighlightService = ApplicationManager.getApplication().service<RegexHighlightService>()
    }
}
