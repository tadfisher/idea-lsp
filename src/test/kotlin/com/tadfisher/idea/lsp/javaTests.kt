package com.tadfisher.idea.lsp

import com.google.common.truth.Truth.assertThat
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.MarkedString
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.SymbolKind.*
import org.eclipse.lsp4j.SymbolKind.Boolean
import org.eclipse.lsp4j.SymbolKind.Number
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.jsonrpc.messages.Either
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

        group("should return hover information") {
            val src = fixture.find("src/main/java/com/example/Hover.java")

            test("for class") {
                val definition = fixture.find("src/main/java/com/example/Definition.java")

                val hover = fixture.server.hover(src.position(7, 15)).get()

                with (hover) {
                    assertThat(contents).containsExactly(
                        Either.forRight<String, MarkedString>(MarkedString("java", """
                            |```
                            |com.example
                            |public class Hover
                            |extends java.lang.Object
                            |```
                            |
                            |Hover javadoc.
                            |
                            ||-----|-----|
                            || See Also: | [`Definition`](${definition.url}#28) |
                            |
                            |
                            """.trimMargin())))
                    assertThat(range).isEqualTo(range(7, 13, 7, 18))
                }
            }

            test("for field") {
                val hover = fixture.server.hover(src.position(12, 21)).get()

                with (hover) {
                    assertThat(contents).containsExactly(
                        Either.forRight<String, MarkedString>(MarkedString("java", """
                            |```
                            |[`com.example.Hover`](${src.url}#83)
                            |public final int value
                            |```
                            |
                            |Field javadoc.
                            |
                            |
                            """.trimMargin())))
                    assertThat(range).isEqualTo(range(12, 21, 12, 26))
                }
            }

            test("for constructor") {
                val hover = fixture.server.hover(src.position(18, 11)).get()

                with (hover) {
                    assertThat(contents).containsExactly(
                        Either.forRight<String, MarkedString>(MarkedString("java", """
                            |```
                            |[`com.example.Hover`](${src.url}#83)
                            |public Hover()
                            |```
                            |
                            |Constructor javadoc.
                            |
                            ||-----|-----|
                            || See Also: | [`Hover(int)`](${src.url}#369) |
                            |
                            |
                            """.trimMargin())))
                    assertThat(range).isEqualTo(range(18, 11, 18, 16))
                }
            }

            test("for secondary constructor") {
                val hover = fixture.server.hover(src.position(26, 11)).get()

                with (hover) {
                    assertThat(contents).containsExactly(
                        Either.forRight<String, MarkedString>(MarkedString("java", """
                            |```
                            |[`com.example.Hover`](${src.url}#83)
                            |public Hover(int value)
                            |```
                            |
                            |Secondary constructor javadoc.
                            |
                            ||-----|-----|
                            || Params: | value -- value param |
                            |
                            |
                            """.trimMargin())))
                    assertThat(range).isEqualTo(range(26, 11, 26, 16))
                }
            }

            test("for method") {
                val hover = fixture.server.hover(src.position(33, 16)).get()

                with (hover) {
                    assertThat(contents).containsExactly(
                        Either.forRight<String, MarkedString>(MarkedString("java", """
                            |```
                            |[`com.example.Hover`](${src.url}#83)
                            |public void method()
                            |```
                            |
                            |Method javadoc.
                            |
                            |
                            """.trimMargin())))
                    assertThat(range).isEqualTo(range(33, 16, 33, 22))
                }
            }
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

        it("should list symbols") {
            val src = fixture.find("src/main/java/com/example/Symbols.java")

            val symbols = fixture.server.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(src.url))).get()

            with (src) {
                assertThat(symbols).containsAllOf(
                    symbol("Symbols.java", File, location(0, 0, 332, 0), null),
                    symbol("com.example", Package, location(0, 0, 0, 20), "Symbols.java"),
                    symbol("java.io.*", Module, location(2, 0, 2, 17), "Symbols.java"),
                    symbol("com.example.Symbols", Class, location(6, 13, 6, 20), "Symbols.java"),
                    symbol("com.example.XXX", Interface, location(330, 10, 330, 13), "Symbols.java"),

                    symbol("@Deprecated", Property, location(5, 1, 5, 11), "com.example.Symbols"),
                    symbol("clz1", Field, location(8, 17, 8, 21), "com.example.Symbols"),
                    symbol("<init>", Constructor, location(42, 4, 47, 5), "com.example.Symbols"),
                    symbol("<init>", Constructor, location(105, 4, 107, 5), "com.example.Symbols"),
                    symbol("<init>", Constructor, location(109, 4, 111, 5), "com.example.Symbols"),
                    symbol("Symbols", Constructor, location(157, 18, 157, 25), "com.example.Symbols"),
                    symbol("ddd", Method, location(153, 20, 153, 23), "com.example.Symbols"),
                    symbol("main", Method, location(251, 23, 251, 27), "com.example.Symbols"),

                    symbol("<anonymous class>", Class, location(163, 23, 163, 24), "Symbols"),
                    symbol("i", Field, location(165, 16, 165, 17), "<anonymous class>"),
                    symbol("X", Constructor, location(167, 23, 167, 24), "<anonymous class>"),
                    symbol("m", Method, location(170, 24, 170, 25), "<anonymous class>"),

                    symbol("com.example.Symbols.Enum", SymbolKind.Enum, location(131, 16, 131, 20), "com.example.Symbols"),
                    symbol("m", Constant, location(133, 8, 133, 9), "com.example.Symbols.Enum"),
                    symbol("Enum", Constructor, location(146, 16, 146, 20), "com.example.Symbols.Enum"),

                    symbol("10", Number, location(16, 24, 16, 26), "arr"),
                    symbol("null", Constant, location(20, 20, 20, 24), "byebye"),
                    symbol("z", Variable, location(43, 12, 43, 13), "<init>"),
                    symbol("true", Boolean, location(162, 20, 162, 24), "b"),
                    symbol("false", Boolean, location(162, 30, 162, 35), "y"),
                    symbol("'c'", SymbolKind.String, location(178, 34, 178, 37), "Y")
                )
            }
        }
    }
})
