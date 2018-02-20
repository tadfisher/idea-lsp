package com.tadfisher.idea.lsp

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.CommandLineApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.PlatformTestCase
import com.intellij.testFramework.TestDataProvider
import com.intellij.testFramework.TestLoggerFactory
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
    val application = LspTestApplication.getInstance()
    val server = IdeaLanguageServer()
    val client = mock<LanguageClient>()

    init {
        Logger.setFactory(TestLoggerFactory::class.java)
    }

    fun setup() {
        connect()
        initialize()
    }

    fun teardown() {
        server.shutdown()
        application.dataProvider = null
        TestLoggerFactory.onTestFinished(false)
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
        }).get().also {
            application.dataProvider = TestDataProvider(server.workspace.project)
        }

    fun find(path: String): File =
        File(baseDir, path).takeUnless { !it.exists() }
            ?: throw FileNotFoundException(path)

    fun findDocument(file: File): Document =
        server.session.workspace.findDocument(file.url())
            ?: throw FileNotFoundException(file.canonicalPath)
}

class LspTestApplication :  CommandLineApplication(true, false, true), Disposable {
    companion object {
        @JvmStatic
        @Synchronized
        fun getInstance() = ourInstance as? LspTestApplication
            ?: PlatformTestCase.doAutodetectPlatformPrefix()
                .let { LspTestApplication() }
                .also {
                    PluginManagerCore.getPlugins()
                    with (ApplicationManagerEx.getApplicationEx()) {
                        load()
                    }
                }

        @JvmStatic
        fun disposeInstance() {
            ourInstance
                ?.let { ApplicationManager.getApplication() }
                ?.let(Disposer::dispose)
            ourInstance = null
        }
    }

    var dataProvider: DataProvider? = null

    override fun getData(dataId: String): Any? = dataProvider?.getData(dataId)

    override fun dispose() {
        disposeInstance()
    }
}

fun File.url(): String = "file://$canonicalPath"
