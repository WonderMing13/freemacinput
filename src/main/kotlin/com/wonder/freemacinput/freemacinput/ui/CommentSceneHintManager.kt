package com.wonder.freemacinput.freemacinput.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.diagnostic.Logger

object CommentSceneHintManager {
    
    private val logger = Logger.getInstance(CommentSceneHintManager::class.java)
    
    fun showHint(editor: Editor, offset: Int) {
        try {
            val document = editor.document
            val text = document.text
            
            if (offset < 2) return
            
            val beforeCursor = text.substring(maxOf(0, offset - 2), offset)
            val isLineComment = beforeCursor == "//"
            val isBlockComment = beforeCursor == "/*"
            
            if (isLineComment || isBlockComment) {
                ToastManager.showToast(editor, "注释场景", false)
                logger.info("显示注释场景提示")
            }
        } catch (e: Exception) {
            logger.error("显示注释场景提示失败", e)
        }
    }
    
    fun dismissHint(editor: Editor) {
    }
    
    fun dismissAll() {
    }
}
