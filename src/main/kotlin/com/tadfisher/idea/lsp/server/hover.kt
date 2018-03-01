package com.tadfisher.idea.lsp.server

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.lang.Language
import com.intellij.lang.LanguageDocumentation
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.extensions.Extensions
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jsoup.nodes.Document

data class HoverInfo(val language: Language, val element: PsiElement, val content: String)

internal fun replaceElementLinks(doc: Document, element: PsiElement, workspace: Workspace): Document {
    val provider by lazy { workspace.read { DocumentationManager.getProviderFromElement(element) } }
    val manager by lazy { PsiManager.getInstance(element.project) }

    doc.select("a[href^=psi_element]").forEach { a ->
        val url = a.attr("href")
        val target = workspace.read { resolveTargetElement(url, element, provider, manager) }
        a.attr("href", target?.let { linkToElement(it, workspace) })
    }

    return doc
}

internal fun resolveTargetElement(
    url: String,
    element: PsiElement,
    provider: DocumentationProvider,
    manager: PsiManager
): PsiElement? {
    val refText = url.substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length).let {
        val separatorPos = it.lastIndexOf(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL_REF_SEPARATOR)
        if (separatorPos >= 0) it.substring(0, separatorPos) else it
    }

    return provider.getDocumentationElementForLink(manager, refText, element)
        ?: Extensions.getExtensions(DocumentationProvider.EP_NAME)
            .asSequence()
            .map { it.getDocumentationElementForLink(manager, refText, element) }
            .firstOrNull()
        ?: Language.getRegisteredLanguages()
            .asSequence()
            .mapNotNull { LanguageDocumentation.INSTANCE.forLanguage(it) }
            .map { it.getDocumentationElementForLink(manager, refText, element) }
            .firstOrNull()
}

internal fun linkToElement(element: PsiElement, workspace: Workspace): String? =
    workspace.read {
        element.containingFile?.virtualFile?.url?.let { url ->
            workspace.translate(url) + "#${element.textOffset}"
        }
    }
