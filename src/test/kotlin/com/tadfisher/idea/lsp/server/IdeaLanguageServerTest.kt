package com.tadfisher.idea.lsp.server

import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.nhaarman.mockito_kotlin.mock
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.junit.Test

class IdeaLanguageServerTest : LightCodeInsightFixtureTestCase() {

  lateinit var languageServer: IdeaLanguageServer

  val languageClient: LanguageClient = mock {}

  override fun setUp() {
    super.setUp()
    languageServer = IdeaLanguageServer().apply {
      connect(languageClient)
    }
  }

  @Test
  fun testInitialize() {
    val result = languageServer.initialize(InitializeParams().apply {
      processId = 0
      rootUri = project.baseDir.url
      trace = "verbose"
      capabilities = ClientCapabilities(
          WorkspaceClientCapabilities().apply {
            applyEdit = true
            workspaceEdit = WorkspaceEditCapabilities(true)
            didChangeConfiguration = DidChangeConfigurationCapabilities(true)
            didChangeWatchedFiles = DidChangeWatchedFilesCapabilities(true)
            symbol = SymbolCapabilities(true)
            executeCommand = ExecuteCommandCapabilities(true)
          },
          TextDocumentClientCapabilities().apply {
            synchronization = SynchronizationCapabilities(true, true, true)
            completion = CompletionCapabilities(CompletionItemCapabilities(true), true)
            hover = HoverCapabilities(true)
            signatureHelp = SignatureHelpCapabilities(true)
            references = ReferencesCapabilities(true)
            documentHighlight = DocumentHighlightCapabilities(true)
            documentSymbol = DocumentSymbolCapabilities(true)
            formatting = FormattingCapabilities(true)
            rangeFormatting = RangeFormattingCapabilities(true)
            onTypeFormatting = OnTypeFormattingCapabilities(true)
            definition = DefinitionCapabilities(true)
            codeAction = CodeActionCapabilities(true)
            codeLens = CodeLensCapabilities(true)
            documentLink = DocumentLinkCapabilities(true)
            rename = RenameCapabilities(true)
          },
          null)
    }).get()

    assertThat(result).isNotNull()
    assertThat(languageServer.session.project).isNotNull()
  }
}