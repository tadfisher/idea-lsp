package com.tadfisher.idea.lsp.server

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.tadfisher.idea.lsp.LspRenameRefactoring
import java.io.FileNotFoundException

class ClientSession(val workspace: Workspace) {
    val application = ApplicationManagerEx.getApplicationEx()

    private val targetElementUtil by lazy { TargetElementUtil.getInstance() }

    fun close() {
        workspace.clear()
    }

    fun reloadFile(uri: String) {
        workspace.findVirtualFile(uri)?.refresh(false, true)
    }

    fun updateFile(uri: String, text: String) =
        with (workspace.findDocument(uri) ?: throw FileNotFoundException(uri)) {
            invokeAndWaitIfNeed {
                application.runWriteAction {
                    setText(text)
                    workspace.psiDocumentManager.commitDocument(this)
                }
            }
        }

    fun updateFile(uri: String, startLine: Int, startChar: Int, endLine: Int, endChar: Int, text: String) =
        with (workspace.findDocument(uri) ?: throw FileNotFoundException(uri)) {
            replaceString(offset(startLine, startChar), offset(endLine, endChar), text)
        }

    fun commitFile(uri: String) =
        with (workspace.findDocument(uri) ?: throw FileNotFoundException(uri)) {
            workspace.psiDocumentManager.commitDocument(this)
        }

    fun findDefinitions(uri: String, line: Int, char: Int): List<PsiElement> {
        val psiFile = workspace.findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = workspace.documentFor(psiFile) ?: throw FileNotFoundException(uri)
        val offset = document.offset(line, char)
        val ref = workspace.read { psiFile.findReferenceAt(offset) }
        if (ref != null) {
            return workspace.read { targetElementUtil.getTargetCandidates(ref) }.toList()
        }
        val element = workspace.read { psiFile.findElementAt(offset) }
        if (element != null) {
            return listOfNotNull(
                workspace.read { targetElementUtil.getGotoDeclarationTarget(element, element.navigationElement) }
            )
        }
        return emptyList()
    }

    fun findReferences(uri: String, line: Int, char: Int): List<PsiElement> {
        val psiFile = workspace.findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = workspace.psiDocumentManager.getDocument(psiFile) ?: throw FileNotFoundException(uri)
        val element = psiFile.findElementAt(document.offset(line, char))
        return element?.named()?.findReferences() ?: emptyList()
    }

    fun rename(uri: String, line: Int, char: Int, name: String): Map<String, List<UsageInfo>> {
        val psiFile = workspace.findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = workspace.psiDocumentManager.getDocument(psiFile) ?: throw FileNotFoundException(uri)
        val element = psiFile.findElementAt(document.offset(line, char)) ?: return emptyMap()
        return with (LspRenameRefactoring(workspace.project, element, name, false, false)) {
            findUsages().also { doRefactoring(it) }.groupBy { it.virtualFile!!.url }
        }
    }

    private fun Document.offset(line: Int, char: Int): Int = getLineStartOffset(line) + char

    private fun PsiElement.named(): PsiNamedElement? =
        generateSequence(this) { it.parent }
            .filterIsInstance<PsiNamedElement>()
            .firstOrNull()

    private fun PsiNamedElement.findReferences(): List<PsiElement> =
        ReferencesSearch.search(this)
            .findAll()
            .map { it.element }

    private fun PsiReference.resolveAll(): List<PsiElement> =
        resolve()?.let { listOf(it) }
            ?: if (this is PsiPolyVariantReference) {
                multiResolve(true).mapNotNull { it.element }
            } else {
                emptyList()
            }

    companion object {
        private val log = Logger.getInstance(ClientSession::class.java)
    }
}
