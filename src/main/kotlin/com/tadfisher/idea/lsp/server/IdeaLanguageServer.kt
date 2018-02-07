package com.tadfisher.idea.lsp.server

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.ex.ProjectManagerEx
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.CodeLens
import org.eclipse.lsp4j.CodeLensOptions
import org.eclipse.lsp4j.CodeLensParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentHighlight
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams
import org.eclipse.lsp4j.DocumentRangeFormattingParams
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TextDocumentPositionParams
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.net.URI
import java.util.concurrent.CompletableFuture

class IdeaLanguageServer : LanguageServer, LanguageClientAware, WorkspaceService, TextDocumentService {

    private lateinit var client: LanguageClient
    @VisibleForTesting lateinit var session: ClientSession

    override fun connect(client: LanguageClient) {
        this.client = client
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        val uri = URI.create(params.rootUri)
        val project = ProjectManagerEx.getInstanceEx().loadProject(uri.path)
            ?: throw IllegalStateException("Project could not be loaded for path '${params.rootUri}'")

        session = ClientSession(client, params.processId, params.rootUri, params.capabilities, project)

        val capabilities = ServerCapabilities().apply {
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)
            completionProvider = CompletionOptions(true, listOf(".", "@", "#"))
            hoverProvider = true
            definitionProvider = true
            documentSymbolProvider = true
            workspaceSymbolProvider = true
            referencesProvider = true
            documentHighlightProvider = true
            documentFormattingProvider = true
            documentRangeFormattingProvider = true
            codeLensProvider = CodeLensOptions(true)
            codeActionProvider = true
        }

        val result = InitializeResult(capabilities)

        return CompletableFuture.completedFuture(result)
    }

    override fun shutdown(): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(Any())
    }

    override fun exit() {
        // TODO
    }

    override fun getTextDocumentService(): TextDocumentService = this
    override fun getWorkspaceService(): WorkspaceService = this

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun symbol(params: WorkspaceSymbolParams): CompletableFuture<MutableList<out SymbolInformation>> {
        TODO("not implemented")
    }

    override fun resolveCompletionItem(unresolved: CompletionItem): CompletableFuture<CompletionItem> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<MutableList<out Command>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hover(position: TextDocumentPositionParams): CompletableFuture<Hover> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun documentHighlight(position: TextDocumentPositionParams): CompletableFuture<MutableList<out DocumentHighlight>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun definition(params: TextDocumentPositionParams): CompletableFuture<List<Location>> =
        CompletableFuture.supplyAsync {
            session.findDefinitions(params.textDocument.uri, params.position.line, params.position.character)
                .map { it.location() }
        }

    override fun rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun codeLens(params: CodeLensParams): CompletableFuture<MutableList<out CodeLens>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit> =
        CompletableFuture.supplyAsync {
            session.rename(params.textDocument.uri, params.position.line, params.position.character, params.newName)
                .mapValues { (_, usages) -> usages
                    .sortedByDescending { it.segment!!.startOffset }
                    .map {
                        TextEdit(Range(it.file!!.position(it.segment!!.startOffset),
                            it.file!!.position(it.segment!!.endOffset)),
                            it.element!!.text)
                    }
                }
                .let { textEdits -> WorkspaceEdit(textEdits) }
        }

    override fun completion(position: TextDocumentPositionParams): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<MutableList<out SymbolInformation>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun didOpen(params: DidOpenTextDocumentParams) =
        session.updateFile(params.textDocument.uri, params.textDocument.text)

    override fun didSave(params: DidSaveTextDocumentParams) =
        if (params.text != null) {
            session.updateFile(params.textDocument.uri, params.text)
        } else {
            session.reloadFile(params.textDocument.uri)
        }

    override fun signatureHelp(position: TextDocumentPositionParams): CompletableFuture<SignatureHelp> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun didClose(params: DidCloseTextDocumentParams) =
        session.reloadFile(params.textDocument.uri)

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun didChange(params: DidChangeTextDocumentParams) =
        params.contentChanges.forEach { change ->
            session.updateFile(params.textDocument.uri,
                change.range.start.line, change.range.start.character,
                change.range.end.line, change.range.end.character,
                change.text)
        }.also { session.commitFile(params.textDocument.uri) }

    override fun references(params: ReferenceParams): CompletableFuture<List<Location>> =
        CompletableFuture.supplyAsync {
            session.findReferences(params.textDocument.uri, params.position.line, params.position.character)
                .map { it.location() }
        }

    override fun resolveCodeLens(unresolved: CodeLens): CompletableFuture<CodeLens> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
