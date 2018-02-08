package com.tadfisher.idea.lsp.server

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.listeners.RefactoringListenerManager
import com.intellij.usageView.UsageInfo
import com.tadfisher.idea.lsp.LspRenameRefactoring
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.services.LanguageClient
import java.io.FileNotFoundException

class ClientSession(val client: LanguageClient,
    val processId: Int?,
    val rootUri: String,
    val capabilities: ClientCapabilities,
    val project: Project) {

    val fileManager by lazy { VirtualFileManager.getInstance() }
    val fileDocumentManager by lazy { FileDocumentManager.getInstance() }
    val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }
    val psiFileFactory by lazy { PsiFileFactory.getInstance(project) }
    val psiManager by lazy { PsiManagerEx.getInstance(project) }
    val refactoringFactory by lazy { RefactoringFactory.getInstance(project) }
    val refactoringListenerManager by lazy { RefactoringListenerManager.getInstance(project) }
    val virtualFileManager by lazy { VirtualFileManager.getInstance() }

    fun reloadFile(uri: String) {
        findVirtualFile(uri)?.refresh(false, true)
    }

    fun updateFile(uri: String, text: String) =
        with (findDocument(uri) ?: throw FileNotFoundException(uri)) {
            setText(text)
            psiDocumentManager.commitDocument(this)
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

    private fun findDocument(uri: String): Document? = findPsiFile(uri)?.let { psiDocumentManager.getDocument(it) }

    private fun findPsiFile(uri: String): PsiFile? = findVirtualFile(uri)?.let { psiManager.findFile(it) }

    private fun findVirtualFile(uri: String): VirtualFile? = fileManager.findFileByUrl(uri)

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
}
