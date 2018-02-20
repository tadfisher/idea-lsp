package com.tadfisher.idea.lsp.server

import com.google.common.util.concurrent.SettableFuture
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.io.FileNotFoundException
import java.util.concurrent.Future

fun PsiElement.location(workspace: Workspace): Location = workspace.read {
    textRange.let { range ->
        Location(
            workspace.translate("file://" + containingFile.virtualFile.canonicalPath),
            Range(containingFile.position(range.startOffset), containingFile.position(range.endOffset))
        )
    }
}

fun PsiFile.position(offset: Int): Position =
    with (viewProvider.document ?: throw FileNotFoundException(virtualFile.url)) {
        val line = getLineNumber(offset)
        Position(line, offset - getLineStartOffset(line))
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
