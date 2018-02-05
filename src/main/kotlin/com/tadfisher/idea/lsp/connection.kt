package com.tadfisher.idea.lsp

import org.jdesktop.swingx.util.OS
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.nio.channels.Channels

data class Connection(val input: InputStream,
    val output: OutputStream)

interface ConnectionFactory {
    fun open(): Connection
}

class StdioConnectionFactory : ConnectionFactory {
    override fun open(): Connection = Connection(System.`in`, System.out)
}

class SocketConnectionFactory(private val port: Int) : ConnectionFactory {
    override fun open(): Connection {
        val serverSocket = ServerSocket(port)
        val clientSocket = serverSocket.accept()
        return Connection(clientSocket.inputStream, clientSocket.outputStream)
    }
}

class PipeConnectionFactory(private val read: File, private val write: File) : ConnectionFactory {
    override fun open(): Connection =
        if (OS.isWindows()) {
            Connection(Channels.newInputStream(RandomAccessFile(read, "rwd").channel),
                Channels.newOutputStream(RandomAccessFile(write, "rwd").channel))
        } else {
            val readSocket = AFUNIXSocket.newInstance()
            readSocket.connect(AFUNIXSocketAddress(read))
            val writeSocket = AFUNIXSocket.newInstance()
            writeSocket.connect(AFUNIXSocketAddress(write))
            Connection(readSocket.inputStream, writeSocket.outputStream)
        }
}
