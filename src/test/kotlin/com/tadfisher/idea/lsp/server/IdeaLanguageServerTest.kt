package com.tadfisher.idea.lsp.server

import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.nhaarman.mockitokotlin2.mock
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.CodeActionCapabilities
import org.eclipse.lsp4j.CodeLensCapabilities
import org.eclipse.lsp4j.CompletionCapabilities
import org.eclipse.lsp4j.CompletionItemCapabilities
import org.eclipse.lsp4j.DefinitionCapabilities
import org.eclipse.lsp4j.DidChangeConfigurationCapabilities
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities
import org.eclipse.lsp4j.DocumentHighlightCapabilities
import org.eclipse.lsp4j.DocumentLinkCapabilities
import org.eclipse.lsp4j.DocumentSymbolCapabilities
import org.eclipse.lsp4j.ExecuteCommandCapabilities
import org.eclipse.lsp4j.FormattingCapabilities
import org.eclipse.lsp4j.HoverCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.OnTypeFormattingCapabilities
import org.eclipse.lsp4j.RangeFormattingCapabilities
import org.eclipse.lsp4j.ReferencesCapabilities
import org.eclipse.lsp4j.RenameCapabilities
import org.eclipse.lsp4j.SignatureHelpCapabilities
import org.eclipse.lsp4j.SymbolCapabilities
import org.eclipse.lsp4j.SynchronizationCapabilities
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.WorkspaceClientCapabilities
import org.eclipse.lsp4j.WorkspaceEditCapabilities
import org.eclipse.lsp4j.services.LanguageClient
import org.junit.Test

class IdeaLanguageServerTest : LightCodeInsightFixtureTestCase() {

    private lateinit var languageServer: IdeaLanguageServer

    private val languageClient: LanguageClient = mock {}

    override fun setUp() {
        super.setUp()
        languageServer = IdeaLanguageServer().apply {
            connect(languageClient)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : LightProjectDescriptor() {

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
