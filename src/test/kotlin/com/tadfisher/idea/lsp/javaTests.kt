package com.tadfisher.idea.lsp

import com.google.common.truth.Truth.assertThat
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.TextDocumentPositionParams
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode

class JavaSpec : Spek({
    val fixture by memoized(CachingMode.SCOPE) { testFixture("java-project") }

    beforeEachTest {
        fixture.setup()
    }
    afterEachTest { fixture.teardown() }

    describe("a Java LSP server") {
        it("should handle didOpen") {
            val src = fixture.find("src/main/java/com/example/PackagePrivate.java")

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

            val doc = fixture.findDocument(src)
            assertThat(doc.text).isEqualTo(virtualContents)
            assertThat(src.readText()).isEqualTo(fileContents)
        }

        group("should find definitions") {
            val src = fixture.find("src/main/java/com/example/Definition.java")

            test("for private const") {
                val found = fixture.server.definition(TextDocumentPositionParams(
                    TextDocumentIdentifier(src.url()),
                    Position(6, 19)
                )).get()

                assertThat(found.size).isEqualTo(1)
                with (found[0]) {
                    assertThat(uri).isEqualTo(src.url())
                    assertThat(range.start).isEqualTo(Position(3, 4))
                    assertThat(range.end).isEqualTo(Position(3, 48))
                }
            }

            test("for external package-private method") {
                val dst = fixture.find("src/main/java/com/example/PackagePrivate.java")

                val found = fixture.server.definition(TextDocumentPositionParams(
                    TextDocumentIdentifier(src.url()),
                    Position(12, 14)
                )).get()

                assertThat(found.size).isEqualTo(1)
                assertThat(found[0].uri).isEqualTo(dst.url())
            }
        }
    }
})
