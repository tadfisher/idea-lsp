package com.tadfisher.idea.lsp

import com.google.common.truth.Truth.assertThat
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.TextDocumentItem

class JavaTests : LspFixtureTestCase("java-project", "main") {

    fun testInitialize() {
        val (server, _) = connect(null)
            val result = server.initialize(InitializeParams().apply {
                processId = 0
                rootUri = project.baseDir.url
                trace = "verbose"
                capabilities = ALL_CLIENT_CAPABILITIES
            }).get()

        assertThat(result).isNotNull()
        assertThat(server.session.project).isNotNull()
    }

    fun testDidOpen() {
        val file = myFixture.findFileInTempDir("java/com/example/PackagePrivate.java")
        val contents = """
            package com.example;
            class PackagePrivate {
                void otherMethod() {}
            }
            """.trimIndent()
        val (server, _) = connect()
        server.didOpen(DidOpenTextDocumentParams(
            TextDocumentItem(
                file.url,
                "java",
                0,
                contents)))

        val psi = myFixture.psiManager.findFile(file)!!
        val doc = myFixture.getDocument(psi)!!

        assertThat(doc.text == contents)
    }
}
