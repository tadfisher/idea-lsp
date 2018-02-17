package com.tadfisher.idea.lsp.server

import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.tadfisher.idea.lsp.LspRenameRefactoring
import java.io.FileNotFoundException

class ClientSession(val workspace: Workspace) {
    val application = ApplicationManagerEx.getApplicationEx()
    val project = workspace.project
    val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }
    val psiManager by lazy { PsiManagerEx.getInstance(project) }

    fun close() {
        workspace.clear()
    }

    fun reloadFile(uri: String) {
        findVirtualFile(uri)?.refresh(false, true)
    }

    fun updateFile(uri: String, text: String) =
        with (findDocument(uri) ?: throw FileNotFoundException(uri)) {
            invokeAndWaitIfNeed {
                application.runWriteAction {
                    setText(text)
                    psiDocumentManager.commitDocument(this)
                }
            }
        }

    fun updateFile(uri: String, startLine: Int, startChar: Int, endLine: Int, endChar: Int, text: String) =
        with (findDocument(uri) ?: throw FileNotFoundException(uri)) {
            replaceString(offset(startLine, startChar), offset(endLine, endChar), text)
        }

    fun commitFile(uri: String) =
        with (findDocument(uri) ?: throw FileNotFoundException(uri)) {
            psiDocumentManager.commitDocument(this)
        }

    fun findDefinitions(uri: String, line: Int, char: Int): List<PsiElement> {
        val psiFile = findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = psiDocumentManager.getDocument(psiFile) ?: throw FileNotFoundException(uri)
        val reference = psiFile.findReferenceAt(document.offset(line, char))
        return reference?.resolveAll() ?: emptyList()
    }

    fun findReferences(uri: String, line: Int, char: Int): List<PsiElement> {
        val psiFile = findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = psiDocumentManager.getDocument(psiFile) ?: throw FileNotFoundException(uri)
        val element = psiFile.findElementAt(document.offset(line, char))
        return element?.named()?.findReferences() ?: emptyList()
    }

    fun rename(uri: String, line: Int, char: Int, name: String): Map<String, List<UsageInfo>> {
        val psiFile = findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = psiDocumentManager.getDocument(psiFile) ?: throw FileNotFoundException(uri)
        val element = psiFile.findElementAt(document.offset(line, char)) ?: return emptyMap()
        return with (LspRenameRefactoring(project, element, name, false, false)) {
            findUsages().also { doRefactoring(it) }.groupBy { it.virtualFile!!.url }
        }
    }

    fun findDocument(uri: String): Document? = findPsiFile(uri)?.let {
        invokeAndWaitIfNeed {
            application.runReadAction(Computable { psiDocumentManager.getDocument(it) })
        }
    }

    private fun findPsiFile(uri: String): PsiFile? = findVirtualFile(uri)?.let {
        invokeAndWaitIfNeed {
            application.runReadAction(Computable { psiManager.findFile(it) })
        }
    }

    private fun findVirtualFile(uri: String): VirtualFile? = workspace.findByUri(uri)

//    private fun uriToPath(uri: String): String? = try {
//        URI(uri).path
//    } catch (e: URISyntaxException) {
//        null
//    }

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
        if (this is PsiPolyVariantReference) {
            multiResolve(false).mapNotNull { it.element }
        } else {
            listOfNotNull(resolve())
        }

    companion object {
        private val log = Logger.getInstance(ClientSession::class.java)
    }
}
