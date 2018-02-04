package com.tadfisher.idea.lsp.server

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiManagerEx
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.services.LanguageClient

class ClientSession(val client: LanguageClient,
                    val processId: Int?,
                    val rootUri: String,
                    val capabilities: ClientCapabilities,
                    val project: Project) {

  val psiFileFactory by lazy { PsiFileFactory.getInstance(project) }
  val psiManager by lazy { PsiManagerEx.getInstance(project) }
  val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }
}