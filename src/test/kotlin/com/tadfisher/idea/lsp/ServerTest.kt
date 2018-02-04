package com.tadfisher.idea.lsp

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import okio.Buffer
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage
import org.junit.Test

class ServerTest : LightCodeInsightFixtureTestCase() {

  val server = LspServerImpl()
  val input = Buffer()
  val output = Buffer()

  override fun setUp() {
    super.setUp()

    server.connect(Connection(input.inputStream(), output.outputStream()))
  }

  @Test
  fun testStuff() {
    input.writeUtf8(RequestMessage().apply {

    }.toString())
  }
}