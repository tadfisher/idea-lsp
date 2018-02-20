package com.tadfisher.idea.lsp

import com.intellij.openapi.application.ApplicationStarterEx
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.xenomachina.argparser.ArgParser

class Args(parser: ArgParser) {
    val port by parser.storing("-p", "--port",
        help = "listen port") { toIntOrNull() }
}

class LspApplicationStarter : ApplicationStarterEx() {

    override fun getCommandName(): String = "lang-server"
    override fun isHeadless(): Boolean = true
    override fun canProcessExternalCommandLine(): Boolean = true

    override fun processExternalCommandLine(args: Array<out String>, currentDirectory: String?) {
        super.processExternalCommandLine(args, currentDirectory)
    }

    override fun premain(args: Array<out String>) {
        // pass
    }

    override fun main(a: Array<out String>) {
        val application = ApplicationManagerEx.getApplicationEx().apply { doNotSave() }
        val server = application.getComponent(LspServer::class.java)
        val future = server.connect(StdioConnectionFactory().open())
        future?.get()
    }
}
