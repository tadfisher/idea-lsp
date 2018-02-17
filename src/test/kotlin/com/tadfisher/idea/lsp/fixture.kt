package com.tadfisher.idea.lsp

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.ApplicationRule
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
import org.eclipse.lsp4j.InitializeResult
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
import java.io.File
import java.io.FileNotFoundException

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

fun testFixture(name: String): LspTestFixture {
    val source = File("testdata/$name")
    val dest = FileUtil.createTempDirectory("idea-lsp-test-$name", null)
    FileUtil.copyDir(source, dest)
    return LspTestFixture(name, dest)
}

class LspTestFixture(
    val name: String,
    val baseDir: File
) {
    private val applicationRule = ApplicationRule()

    val server = IdeaLanguageServer()
    val client = mock<LanguageClient>()

    fun setup() {
        applicationRule.before()
        connect()
        initialize()
    }

    fun teardown() {
        server.shutdown()
    }

    fun connect() {
        server.connect(client)
    }

    fun initialize(): InitializeResult =
        server.initialize(InitializeParams().apply {
            processId = 0
            rootUri = baseDir.url()
            trace = "verbose"
            capabilities = ALL_CLIENT_CAPABILITIES
        }).get()

    fun find(path: String): File =
        File(baseDir, path).takeUnless { !it.exists() }
            ?: throw FileNotFoundException(path)
}

fun File.url(): String = "file://$absolutePath"