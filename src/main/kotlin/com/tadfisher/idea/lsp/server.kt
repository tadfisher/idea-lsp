package com.tadfisher.idea.lsp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.tadfisher.idea.lsp.server.IdeaLanguageServer
import org.eclipse.lsp4j.launch.LSPLauncher
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

interface LspServer : ApplicationComponent {
    fun connect(connection: Connection): Future<*>?
}

class LspServerImpl : LspServer {

    private val languageServer = IdeaLanguageServer()
    private val started = AtomicBoolean()
    private var listening: Future<*>? = null

    override fun getComponentName(): String = "LspServer"

    override fun initComponent() {}

    override fun disposeComponent() {}

    override fun connect(connection: Connection): Future<*>? {
        if (!started.compareAndSet(false, true)) {
            return null
        }

        return ApplicationManager.getApplication().executeOnPooledThread {
            val launcher = LSPLauncher.createServerLauncher(languageServer, connection.input, connection.output)
            val client = launcher.remoteProxy
            // TODO handle other connection types
            languageServer.connect(client)
            listening = launcher.startListening()
        }
    }
}
