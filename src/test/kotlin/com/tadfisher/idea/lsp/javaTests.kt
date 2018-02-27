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
            val src = fixture.find("src/main/java/com/example/PackagePrivate.java")

            val fileContents = src.readText()

            val virtualContents = """
                package com.example;
                class PackagePrivate {
                    void otherMethod();
                }
            """.trimIndent()

            fixture.server.didOpen(DidOpenTextDocumentParams(
                TextDocumentItem(src.url, "java", 0, virtualContents)
            ))

            val doc = fixture.findDocument(src)
            assertThat(doc.text).isEqualTo(virtualContents)
            assertThat(src.readText()).isEqualTo(fileContents)
        }

        group("should find definitions") {
            val src = fixture.find("src/main/java/com/example/Definition.java")

            test("for private const reference") {
                val found = fixture.server.definition(src.position(6, 19)).get()

                assertThat(found).containsExactly(src.location(3, 32, 3, 37))
            }

            test("for external method call") {
                val dst = fixture.find("src/main/java/com/example/PackagePrivate.java")

                val found = fixture.server.definition(src.position(12, 14)).get()

                assertThat(found).containsExactly(dst.location(3, 9, 3, 16))
            }

            test("for external class reference") {
                val dst = fixture.find("src/main/java/com/example/PackagePrivate.java")

                val found = fixture.server.definition(src.position(11, 11)).get()

                assertThat(found).containsExactly(dst.location(2, 6, 2, 20))
            }
        }

        group("should find references") {
            group("for private const declaration") {
                val src = fixture.find("src/main/java/com/example/Definition.java")
                val ref = src.reference(3, 32)

                test("not including declaration") {
                    ref.context.isIncludeDeclaration = false

                    val found = fixture.server.references(ref).get()

                    assertThat(found).containsExactly(src.location(6, 19, 6, 24))
                }
                test("including declaration") {
                    ref.context.isIncludeDeclaration = true

                    val found = fixture.server.references(ref).get()

                    assertThat(found).containsExactly(
                        src.location(6, 19, 6, 24),
                        src.location(3, 32, 3, 37)
                    )
                }
            }

            group("for method declaration") {
                val src = fixture.find("src/main/java/com/example/PackagePrivate.java")
                val dst = fixture.find("src/main/java/com/example/Definition.java")
                val ref = src.reference(3, 11)

                test("not including declaration") {
                    ref.context.isIncludeDeclaration = false

                    val found = fixture.server.references(ref).get()

                    assertThat(found).containsExactly(dst.location(12, 8, 12, 18))
                }

                test("including declaration") {
                    ref.context.isIncludeDeclaration = true

                    val found = fixture.server.references(ref).get()

                    assertThat(found).containsExactly(
                        dst.location(12, 8, 12, 18),
                        src.location(3, 9, 3, 16)
                    )
                }
            }

            group("for class declaration") {
                val src = fixture.find("src/main/java/com/example/PackagePrivate.java")
                val dst = fixture.find("src/main/java/com/example/Definition.java")
                val ref = src.reference(2, 8)

                test("not including declaration") {
                    ref.context.isIncludeDeclaration = false

                    val found = fixture.server.references(ref).get()

                    assertThat(found).containsExactly(
                        dst.location(11, 8, 11, 22),
                        dst.location(11, 32, 11, 46)
                    )
                }

                test("including declaration") {
                    ref.context.isIncludeDeclaration = true

                    val found = fixture.server.references(ref).get()

                    assertThat(found).containsExactly(
                        dst.location(11, 8, 11, 22),
                        dst.location(11, 32, 11, 46),
                        src.location(2, 6, 2, 20)
                    )
                }
            }
        }
    }
})
