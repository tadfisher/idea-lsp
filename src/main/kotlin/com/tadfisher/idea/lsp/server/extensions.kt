package com.tadfisher.idea.lsp.server

import com.google.common.util.concurrent.SettableFuture
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.ide.util.EditSourceUtil
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.editor.Document
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.GeneratedSourcesFilter
import com.intellij.pom.PomTargetPsiElement
import com.intellij.pom.references.PomService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtilCore
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.io.FileNotFoundException
import java.util.concurrent.Future

fun Document.offset(line: Int, char: Int): Int = getLineStartOffset(line) + char

fun PsiElement.collectAllOriginalElements(): List<PsiElement> =
    GeneratedSourcesFilter.EP_NAME.extensions
        .flatMap { it.getOriginalElements(this) }
        .filterNotNull()

fun PsiElement.asNamed(offset: Int = 0): PsiNamedElement? =
    TargetElementUtil.getInstance().getNamedElement(this, offset) as? PsiNamedElement

fun PsiElement.asNameIdentifierOwner(): PsiNameIdentifierOwner? =
    this as? PsiNameIdentifierOwner

fun PsiElement.navigationTarget(): PsiElement? {
    val element = collectAllOriginalElements()
        .firstOrNull(EditSourceUtil::canNavigate)
        ?: takeIf(EditSourceUtil::canNavigate)
        ?: return null

    if (element is PomTargetPsiElement) {
        return PomService.convertToPsi(element.project, element.target)
    }

    val navElement = element.navigationElement
    if (navElement is PomTargetPsiElement) {
        return PomService.convertToPsi(navElement.project, navElement.target)
    }

    PsiUtilCore.getVirtualFile(navigationElement)
        ?.takeIf { it.isValid }
        ?.let { return navElement }

    return null
}

fun PsiElement.location(workspace: Workspace): Location = workspace.read {
    Location(workspace.translate("file://" + containingFile.virtualFile.canonicalPath),
        (asNameIdentifierOwner()?.nameIdentifier ?: this).textRange.run {
            Range(containingFile.position(startOffset), containingFile.position(endOffset))
        })
}

fun PsiFile.position(offset: Int): Position =
    with (viewProvider.document ?: throw FileNotFoundException(virtualFile.url)) {
        val line = getLineNumber(offset)
        Position(line, offset - getLineStartOffset(line))
    }

fun PsiElement.findReferences(): List<PsiElement> =
    invokeAndWaitIfNeed {
        ReferencesSearch.search(this)
            .findAll()
            .map { it.element }
    }

fun PsiReference.resolveAll(): List<PsiElement> =
    resolve()?.let { listOf(it) }
        ?: if (this is PsiPolyVariantReference) {
            multiResolve(true).mapNotNull { it.element }
        } else {
            emptyList()
        }

sealed class ExternalProjectRefreshResult {
    data class Success(val externalProject: DataNode<ProjectData>?) : ExternalProjectRefreshResult()
    data class Failure(val errorMessage: String, val errorDetails: String?) : ExternalProjectRefreshResult()
}

fun Project.refreshExternalSystem(projectSystemId: ProjectSystemId): Future<in ExternalProjectRefreshResult> {
    val future = SettableFuture.create<ExternalProjectRefreshResult>()
    ExternalSystemUtil.refreshProjects(
        ImportSpecBuilder(this, projectSystemId)
            .forceWhenUptodate(true)
            .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
            .callback(object : ExternalProjectRefreshCallback {
                override fun onSuccess(externalProject: DataNode<ProjectData>?) {
                    future.set(ExternalProjectRefreshResult.Success(externalProject))
                }

                override fun onFailure(errorMessage: String, errorDetails: String?) {
                    future.set(ExternalProjectRefreshResult.Failure(errorMessage, errorDetails))
                }
            }))
    return future
}
