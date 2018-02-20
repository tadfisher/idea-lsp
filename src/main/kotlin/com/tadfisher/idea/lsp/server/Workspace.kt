package com.tadfisher.idea.lsp.server

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiManagerEx
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class Workspace(private val projectRoot: VirtualFile) {

    private val application = ApplicationManager.getApplication()
    private val fileSystem = LocalFileSystem.getInstance()
    private val tmpDir = createLspTmpDir()
    private val workspaceRoot = sync()

    val project: Project =
        invokeAndWaitIfNeed {
            ProjectUtil.openOrImport(workspaceRoot.path, null, false)
                ?: throw IllegalStateException("Failed to import project: $projectRoot")
        }

    val dumbService by lazy { DumbService.getInstance(project) }
    val psiDocumentManager by lazy { PsiDocumentManager.getInstance(project) }
    val psiManager by lazy { PsiManagerEx.getInstance(project) }

    fun sync(source: VirtualFile = projectRoot): VirtualFile = write {
        source.refresh(false, true)
        val dest = mkdirs(source)
        log.info("Sync $source to $dest")
        if (source.isDirectory) {
            for (child in dest.children) {
                child.delete(this)
            }
        } else {
            dest.findChild(source.name)?.delete(this)
        }
        VfsUtil.copy(this, source, dest)
    }

    fun addSourcesRoot(uri: String) {
        ModuleRootModificationUtil.addContentRoot(ModuleManager.getInstance(project).modules[0], findVirtualFile(uri)!!)
    }

    fun refresh() {
        StartupManager.getInstance(project).runWhenProjectIsInitialized {
            for (id in externalSystemIds) {
                val result = project.refreshExternalSystem(id).get()
                when (result) {
                    is ExternalProjectRefreshResult.Success ->
                        log.info("project refresh success: $id")
                    is ExternalProjectRefreshResult.Failure ->
                        log.error("""
                        |project refresh error: $id
                        |${result.errorMessage}
                        |${result.errorDetails}
                    """.trimMargin())
                }
            }
        }
    }

    fun <T> read(action: () -> T): T = invokeAndWaitIfNeed {
        application.runReadAction(Computable { action() })
    }

    fun <T> readWhenSmart(action: () -> T): T = invokeAndWaitIfNeed {
        dumbService.runReadActionInSmartMode(Computable { action() })
    }

    fun <T> write(action: () -> T): T = invokeAndWaitIfNeed {
        application.runWriteAction(Computable { action() })
    }

    fun documentFor(psiFile: PsiFile): Document? = read {
        psiDocumentManager.getDocument(psiFile)
    }

    fun findDocument(uri: String): Document? =
        findPsiFile(uri)?.let {
            read { psiDocumentManager.getDocument(it) }
        }

    fun findPsiFile(uri: String): PsiFile? =
        findVirtualFile(uri)?.let {
            read { psiManager.findFile(it) }
        }

    fun findVirtualFile(uri: String): VirtualFile? = read {
        workspaceRoot.findFileByRelativePath(uri.uriToRelPath()).also { file ->
            when {
                file?.exists() == true -> log.info("Found file in workspace: $file")
                file == null -> log.error("File not found in workspace: ${uri.uriToRelPath()}")
                else -> log.error("File does not exist: $file")
            }
        }
    }

    fun clear() =
        invokeAndWaitIfNeed {
            if (ProjectUtil.closeAndDispose(project)) {
                log.info("Closed project $project")
            } else {
                log.error("Could not close project $project")
            }
            application.runWriteAction {
                workspaceRoot.delete(this)
                tmpDir.refresh(false, false)
                if (tmpDir.children.isEmpty()) {
                    tmpDir.delete(this)
                }
            }
        }

    fun translate(uri: String) = "${projectRoot.url}/${uri.removePrefix(workspaceRoot.url).trimStart('/')}"

    private fun mkdirs(projectFile: VirtualFile): VirtualFile =
        projectFile.relPath(projectRoot).split('/').filter { it.isNotBlank() }
            .let { if (!projectFile.isDirectory) it.dropLast(1) else it }
            .fold(tmpDir) { root, name ->
                root.findChild(name) ?: fileSystem.createChildDirectory(this, root, name)
            }

    private fun VirtualFile.relPath(base: VirtualFile): String = path.removePrefix(base.path).trimStart('/')
    private fun String.uriToRelPath() = removePrefix(projectRoot.url).trimStart('/')

    private fun createLspTmpDir(): VirtualFile =
        FileUtil.createTempDirectory(File(calcCanonicalTempPath()), "idea-lsp", null)
            .let { fileSystem.refreshAndFindFileByIoFile(it) }
            ?: throw FileNotFoundException("Could not create temporary workspace for project: $projectRoot")

    companion object {
        private val log = Logger.getInstance(Workspace::class.java)

        private fun calcCanonicalTempPath(): String =
            with (File(System.getProperty("java.io.tmpdir"))) {
                try { canonicalPath } catch (_: IOException) { null }
                    ?.takeIf { !SystemInfoRt.isWindows || !it.contains(" ") }
                    ?: absolutePath
            }

        private val externalSystemIds by lazy {
            ExternalSystemManager.EP_NAME.extensions.map { it.getSystemId() }
        }
    }
}
