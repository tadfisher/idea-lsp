package com.tadfisher.idea.lsp.server

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.io.FileNotFoundException

fun PsiElement.location(): Location =
    textRange.let { range -> Location(
        "file://" + containingFile.virtualFile.canonicalPath,
        Range(containingFile.position(range.startOffset), containingFile.position(range.endOffset))
    )}

fun PsiFile.position(offset: Int): Position =
    with (viewProvider.document ?: throw FileNotFoundException(virtualFile.url)) {
        val line = getLineNumber(offset)
        Position(line, offset - getLineStartOffset(line))
    }
