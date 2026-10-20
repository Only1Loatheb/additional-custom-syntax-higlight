package com.github.only1loatheb.additionalcustomsyntaxhiglight.listener

import com.github.only1loatheb.additionalcustomsyntaxhiglight.highlight.RegexHighlightService
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.Alarm

/**
 * Listens to editor lifecycle events to attach/detach regex highlighting.
 */
class RegexEditorFactoryListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor

        // Highlight immediately on editor creation
        RegexHighlightService.getInstance().highlightEditor(editor)

        // Create a Disposable tied to the editor's lifecycle
        val editorDisposable = Disposer.newDisposable("RegexEditorHighlight")
        editor.putUserData(DISPOSABLE_KEY, editorDisposable)

        // Install a debounced document listener
        val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, editorDisposable)
        val documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (editor.isDisposed) return
                alarm.cancelAllRequests()
                alarm.addRequest({
                    if (!editor.isDisposed) {
                        RegexHighlightService.getInstance().highlightEditor(editor)
                    }
                }, 200)
            }
        }

        editor.document.addDocumentListener(documentListener, editorDisposable)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val disposable = editor.getUserData(DISPOSABLE_KEY)
        if (disposable != null) {
            Disposer.dispose(disposable)
            editor.putUserData(DISPOSABLE_KEY, null)
        }
        RegexHighlightService.getInstance().clearHighlights(editor)
    }

    companion object {
        private val DISPOSABLE_KEY = Key.create<Disposable>("com.github.only1loatheb.additionalcustomsyntaxhiglight.disposable")
    }
}
