package com.tadfisher.idea.lsp

import com.google.common.truth.Truth.assertThat
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.TextDocumentItem
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode

class JavaSpec : Spek({
    val fixture by memoized(CachingMode.SCOPE) { testFixture("java-project") }
    beforeEachTest { fixture.setup() }
    afterEachTest { fixture.teardown() }

    describe("a Java LSP server") {
        it("should handle didOpen") {
            val src = fixture.find("main/java/com/example/PackagePrivate.java")

            val fileContents = src.readText()

            val virtualContents = """
                package com.example;
                class PackagePrivate {
                    void otherMethod();
                }
            """.trimIndent()

            fixture.server.didOpen(DidOpenTextDocumentParams(
                TextDocumentItem(src.url(), "java", 0, virtualContents)
            ))

            val doc = fixture.server.session.findDocument(src.url())!!
            assertThat(doc.text).isEqualTo(virtualContents)
            assertThat(src.readText()).isEqualTo(fileContents)
        }
    }
})
