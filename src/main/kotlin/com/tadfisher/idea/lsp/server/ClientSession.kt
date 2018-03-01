package com.tadfisher.idea.lsp.server

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.tadfisher.idea.lsp.LspRenameRefactoring
import com.vladsch.flexmark.convert.html.FlexmarkHtmlParser
import com.vladsch.flexmark.util.format.TableFormatOptions
import com.vladsch.flexmark.util.mappers.CharWidthProvider
import com.vladsch.flexmark.util.options.MutableDataSet
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType
import org.jsoup.Jsoup
import java.io.FileNotFoundException

class ClientSession(val workspace: Workspace) {
    val application = ApplicationManagerEx.getApplicationEx()

    private val targetElementUtil by lazy { TargetElementUtil.getInstance() }
    private val documentationManager by lazy { DocumentationManager.getInstance(workspace.project) }
    private val docParserOptions by lazy { MutableDataSet()
        .set(TableFormatOptions.ADJUST_COLUMN_WIDTH, false)
        .set(TableFormatOptions.CHAR_WIDTH_PROVIDER, object : CharWidthProvider by CharWidthProvider.NULL {
            override fun charWidth(s: CharSequence): Int = 3
        })
    }

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

    fun hoverInfo(uri: String, line: Int, char: Int): HoverInfo? {
        val psiFile = workspace.findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = workspace.documentFor(psiFile) ?: throw FileNotFoundException(uri)
        val offset = document.offset(line, char)
        val editor = FakeEditor(workspace.project, document, offset)
        val originalElement = workspace.read { psiFile.findElementAt(offset) } ?: return null
        val targetElement = workspace.read {
            documentationManager.findTargetElement(editor, offset, psiFile, originalElement)
                ?: PsiTreeUtil.getParentOfType(originalElement, PsiComment::class.java)
                    ?.let { if (it is PsiDocCommentBase) it.owner else it.parent }
        } ?: return null

        return documentationManager.generateDocumentation(targetElement, originalElement)
            ?.let {
                Jsoup.parseBodyFragment(it).apply {
                    replaceElementLinks(this, originalElement, workspace)
                }.body().html()
            }
            ?.let { FlexmarkHtmlParser.parse(it, 1, docParserOptions) }
            ?.let { HoverInfo(originalElement.language, originalElement, it) }
    }

    fun findDefinitions(uri: String, line: Int, char: Int): List<PsiElement> {
        val psiFile = workspace.findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = workspace.documentFor(psiFile) ?: throw FileNotFoundException(uri)
        val offset = document.offset(line, char)
        val adjustedOffset = TargetElementUtil.adjustOffset(psiFile, document, document.offset(line, char))
        val editor = FakeEditor(workspace.project, document, offset)

        val results = workspace.read {
            GotoDeclarationAction.findTargetElementsNoVS(workspace.project, editor, offset, true)
                ?.toList()
                ?.mapNotNull {
                    targetElementUtil.getGotoDeclarationTarget(it, it.navigationElement).navigationTarget()
                }
        } ?: emptyList()
        if (results.isNotEmpty()) return results

        val ref = workspace.read { psiFile.findReferenceAt(adjustedOffset) }
        if (ref != null) {
            return workspace.read { targetElementUtil.getTargetCandidates(ref) }.toList()
        }

        val element = workspace.read { psiFile.findElementAt(adjustedOffset) }
        if (element != null) {
            return listOfNotNull(
                workspace.read { targetElementUtil.getGotoDeclarationTarget(element, element.navigationElement) }
            )
        }
        return emptyList()
    }

    fun findReferences(uri: String, line: Int, char: Int, includeDeclaration: Boolean): List<PsiElement> {
        val psiFile = workspace.findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = workspace.findDocument(uri) ?: throw FileNotFoundException(uri)
        val adjustedOffset = TargetElementUtil.adjustOffset(psiFile, document, document.offset(line, char))
        val element = workspace.read { psiFile.findElementAt(adjustedOffset)?.asNamed() } ?: return emptyList()
        return element.findReferences()
            .let { if (includeDeclaration) {
                it.plus(findDefinitions(uri, line, char))
            } else {
                it
            }}
    }

    fun listSymbols(uri: String): List<LspSymbol> {
        val psiFile = workspace.findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val uFile = psiFile.toUElementOfType<UFile>() ?: return emptyList()
        val symbols = mutableListOf<LspSymbol>()
        workspace.read { uFile.accept(SymbolUastVisitor { symbols.add(it) }) }
        return symbols.toList()
    }

    fun rename(uri: String, line: Int, char: Int, name: String): Map<String, List<UsageInfo>> {
        val psiFile = workspace.findPsiFile(uri) ?: throw FileNotFoundException(uri)
        val document = workspace.psiDocumentManager.getDocument(psiFile) ?: throw FileNotFoundException(uri)
        val element = psiFile.findElementAt(document.offset(line, char)) ?: return emptyMap()
        return with (LspRenameRefactoring(workspace.project, element, name, false, false)) {
            findUsages().also { doRefactoring(it) }.groupBy { it.virtualFile!!.url }
        }
    }

    companion object {
        private val log = Logger.getInstance(ClientSession::class.java)
    }
}
