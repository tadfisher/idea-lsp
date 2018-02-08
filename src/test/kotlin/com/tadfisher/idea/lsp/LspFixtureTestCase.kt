package com.tadfisher.idea.lsp

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.nhaarman.mockitokotlin2.mock
import com.tadfisher.idea.lsp.server.IdeaLanguageServer
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
import org.junit.Before
import java.io.File

abstract class LspFixtureTestCase(
    private val fixtureDirectory: String,
    private vararg val pathsToCopy: String
) : LightPlatformCodeInsightFixtureTestCase() {

    override fun getTestDataPath() = "testdata/$fixtureDirectory"

    @Before
    override fun setUp() {
        super.setUp()
        for (path in pathsToCopy) {
            val file = File(testDataPath, path)
            if (file.isDirectory) {
                myFixture.copyDirectoryToProject(path, "")
            } else {
                myFixture.copyFileToProject(path)
            }
        }
    }

    protected fun connect(
        clientCapabilities: ClientCapabilities? = ALL_CLIENT_CAPABILITIES
    ): Pair<IdeaLanguageServer, LanguageClient> =
        mock<LanguageClient>().let { client ->
            Pair(
                IdeaLanguageServer().apply {
                    connect(client)
                    if (clientCapabilities != null) {
                        initialize(InitializeParams().apply {
                            processId = 0
                            rootUri = project.baseDir.url
                            trace = "verbose"
                            capabilities = ALL_CLIENT_CAPABILITIES
                        }).get()
                    }
                },
                client)
        }

    companion object {
        val ALL_CLIENT_CAPABILITIES = ClientCapabilities(
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
    }
}
