package com.tadfisher.idea.lsp.server

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.PsiVariable
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.SymbolKind.*
import org.jetbrains.uast.UAnchorOwner
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassInitializer
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UEnumConstant
import org.jetbrains.uast.UField
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.isBooleanLiteral
import org.jetbrains.uast.isNumberLiteral
import org.jetbrains.uast.java.JavaUFile
import org.jetbrains.uast.namePsiElement
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.withContainingElements

data class LspSymbol(val name: String, val kind: SymbolKind, val element: PsiElement, val containerName: String?)

internal class SymbolUastVisitor(
    private val onSymbol: (LspSymbol) -> Unit
) : AbstractUastVisitor() {
    override fun visitFile(node: UFile): Boolean =
        on(File, node).also {
            when (node) {
                is JavaUFile -> node.psi<PsiJavaFile>().packageStatement?.let { psi ->
                    on(Package, psi.packageName, psi, node.name())
                }
            // TODO Kotlin, Scala, etc; is there a UPackageStatement?
            }
        }

    override fun visitImportStatement(node: UImportStatement) = on(Module, node)

    override fun visitClass(node: UClass) =
        node.psi<PsiClass>().let { psi -> on(when {
            psi.isInterface -> Interface
            psi.isEnum -> SymbolKind.Enum
            else -> Class
        }, node) }

    override fun visitInitializer(node: UClassInitializer) = on(Constructor, node)

    override fun visitMethod(node: UMethod) = on(if (node.isConstructor) Constructor else Method, node)

    override fun visitVariable(node: UVariable) = on(Variable, node)

    override fun visitField(node: UField) = on(Field, node)

    override fun visitAnnotation(node: UAnnotation) =
        node.javaPsi?.let { on(Property, node) } ?: false

    override fun visitEnumConstant(node: UEnumConstant) = on(Constant, node)

    override fun visitLiteralExpression(node: ULiteralExpression) = on(when {
        node.isNull -> SymbolKind.Constant // TODO use SymbolKind.Null
        node.isBooleanLiteral() -> SymbolKind.Boolean
        node.isNumberLiteral() -> SymbolKind.Number
        else -> SymbolKind.String
    }, node)

    // TODO Operator
    // TODO TypeParameter

    private fun on(kind: SymbolKind, node: UElement): Boolean =
        on(kind, node.name() ?: "<error>", node.anchor().psi(), node.containerName())

    private fun on(kind: SymbolKind, name: String, anchor: PsiElement, containerName: String?): Boolean =
        onSymbol(LspSymbol(name, kind, anchor, containerName)).let { false }
}

private fun UElement.name(): String? = when (this) {
    is UFile -> psi<PsiFile>().name
    is UImportStatement -> importReference?.asRenderString()?.plus(if (isOnDemand) ".*" else "") ?: "<error>"
    is UClass -> qualifiedName ?: "<anonymous class>"
    is UClassInitializer -> psi<PsiClassInitializer>().name ?: "<init>"
    is UMethod -> name
    is UVariable -> name ?: "<unknown variable>"
    is UField -> name
    is UAnnotation -> qualifiedName?.let { "@$it"} ?: namePsiElement?.text?.let { "@$it" } ?: "<unknown annotation>"
    is UEnumConstant -> name
    is ULiteralExpression -> asRenderString()
    else -> null
}

private fun UElement.anchor(): UElement =
    ((this as? UAnchorOwner)?.uastAnchor) ?: this

private fun UElement.containerName(): String? =
    uastParent?.withContainingElements
        ?.mapNotNull { it.name() }
        ?.firstOrNull()

private inline fun <reified T : PsiElement> UElement.psi(): T =
    sourcePsi as? T ?: javaPsi as? T ?: throw TypeCastException("The psi of ${this::class} is not ${T::class}")

// TODO Implement this as a fallback for non-UAST language plugins (e.g. Scala)
internal class SymbolPsiVisitor(
    private val onSymbol: (LspSymbol) -> Unit
): PsiElementVisitor(), PsiRecursiveVisitor {
    override fun visitElement(element: PsiElement) {
        when (element) {
            is PsiFile -> TODO()
            is PsiPackageStatement -> TODO()
            is PsiImportStatement -> TODO()
            is PsiClass -> TODO()
            is PsiClassInitializer -> TODO()
            is PsiMethod -> TODO()
            is PsiVariable -> TODO()
            is PsiField -> TODO()
            is PsiAnnotation -> TODO()
            is PsiEnumConstant -> TODO()
            is PsiLiteralExpression -> TODO()
        }
    }
}
