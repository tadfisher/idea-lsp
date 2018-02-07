package com.tadfisher.idea.lsp.vfs

import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem
import java.io.InputStream
import java.io.OutputStream

/**
 * Read-only filesystem implementation. Changes are persisted in memory only,
 * and files are synchronized explicitly by contents.
 */
class OverlayVirtualFileSystem(val delegate: NewVirtualFileSystem) : NewVirtualFileSystem() {



    override fun list(file: VirtualFile): Array<String> {
        TODO("not implemented")
    }

    override fun getTimeStamp(file: VirtualFile): Long {
        TODO("not implemented")
    }

    override fun findFileByPathIfCached(path: String): VirtualFile? {
        TODO("not implemented")
    }

    override fun getProtocol(): String {
        TODO("not implemented")
    }

    override fun contentsToByteArray(file: VirtualFile): ByteArray {
        TODO("not implemented")
    }

    override fun getAttributes(file: VirtualFile): FileAttributes? {
        TODO("not implemented")
    }

    override fun getRank(): Int {
        TODO("not implemented")
    }

    override fun findFileByPath(path: String): VirtualFile? {
        TODO("not implemented")
    }

    override fun renameFile(requestor: Any?, file: VirtualFile, newName: String) {
        TODO("not implemented")
    }

    override fun createChildFile(requestor: Any?, parent: VirtualFile, file: String): VirtualFile {
        TODO("not implemented")
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        TODO("not implemented")
    }

    override fun copyFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile {
        TODO("not implemented")
    }

    override fun extractRootPath(path: String): String {
        TODO("not implemented")
    }

    override fun refresh(asynchronous: Boolean) {
        TODO("not implemented")
    }

    override fun getInputStream(file: VirtualFile): InputStream {
        TODO("not implemented")
    }

    override fun setWritable(file: VirtualFile, writableFlag: Boolean) {
        TODO("not implemented")
    }

    override fun getLength(file: VirtualFile): Long {
        TODO("not implemented")
    }

    override fun deleteFile(requestor: Any?, file: VirtualFile) {
        TODO("not implemented")
    }

    override fun setTimeStamp(file: VirtualFile, timeStamp: Long) {
        TODO("not implemented")
    }

    override fun createChildDirectory(requestor: Any?, parent: VirtualFile, dir: String): VirtualFile {
        TODO("not implemented")
    }

    override fun isWritable(file: VirtualFile): Boolean {
        TODO("not implemented")
    }

    override fun isDirectory(file: VirtualFile): Boolean {
        TODO("not implemented")
    }

    override fun getOutputStream(file: VirtualFile, requestor: Any?, modStamp: Long, timeStamp: Long): OutputStream {
        TODO("not implemented")
    }

    override fun moveFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile) {
        TODO("not implemented")
    }

    override fun exists(file: VirtualFile): Boolean {
        TODO("not implemented")
    }
}
