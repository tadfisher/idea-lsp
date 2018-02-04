package com.tadfisher.idea.lsp.server

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.ex.ProjectManagerEx
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import java.net.URI
import java.util.concurrent.CompletableFuture

class IdeaLanguageServer : LanguageServer, LanguageClientAware, WorkspaceService, TextDocumentService {

  private lateinit var client: LanguageClient
  lateinit var session: ClientSession

  override fun connect(client: LanguageClient) {
    this.client = client
  }

  override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
    val uri = URI.create(params.rootUri)
    val project = ProjectManagerEx.getInstanceEx().loadProject(uri.path)
        ?: throw IllegalStateException("Project could not be loaded for path '${params.rootUri}'")

    session = ClientSession(client, params.processId, params.rootUri, params.capabilities, project)

    val capabilities = ServerCapabilities()
    capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Incremental)
    capabilities.completionProvider = CompletionOptions(true, listOf(".", "@", "#"))
    capabilities.hoverProvider = true
    capabilities.definitionProvider = true
    capabilities.documentSymbolProvider = true
    capabilities.workspaceSymbolProvider = true
    capabilities.referencesProvider = true
    capabilities.documentHighlightProvider = true
    capabilities.documentFormattingProvider = true
    capabilities.documentRangeFormattingProvider = true
    capabilities.codeLensProvider = CodeLensOptions(true)
    capabilities.codeActionProvider = true

    val result = InitializeResult()
    result.capabilities = capabilities
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

  override fun definition(position: TextDocumentPositionParams): CompletableFuture<MutableList<out Location>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun codeLens(params: CodeLensParams): CompletableFuture<MutableList<out CodeLens>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun completion(position: TextDocumentPositionParams): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<MutableList<out SymbolInformation>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun didOpen(params: DidOpenTextDocumentParams) {
    params.textDocument.let { doc ->
      session.psiFileFactory.createFileFromText(doc., FileTypeRegistry.getInstance().getFileTypeByFileName(doc))
    }

  }

  override fun didSave(params: DidSaveTextDocumentParams) {

  }

  override fun signatureHelp(position: TextDocumentPositionParams): CompletableFuture<SignatureHelp> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun didClose(params: DidCloseTextDocumentParams) {
  }

  override fun formatting(params: DocumentFormattingParams): CompletableFuture<MutableList<out TextEdit>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun didChange(params: DidChangeTextDocumentParams) {

  }

  override fun references(params: ReferenceParams): CompletableFuture<MutableList<out Location>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun resolveCodeLens(unresolved: CodeLens): CompletableFuture<CodeLens> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}