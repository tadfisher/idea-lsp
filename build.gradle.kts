
import org.gradle.kotlin.dsl.*
import org.jetbrains.intellij.IntelliJPluginExtension

plugins {
  kotlin("jvm")
  idea
  id("org.jetbrains.intellij") version "0.2.17"
  id("com.github.ben-manes.versions") version "0.15.0"
}

val ideaVersion: String by extra
val javaVersion: String by extra
val downloadIdeaSources: String by extra

repositories {
  jcenter()
}

dependencies {
  compile(kotlin("stdlib"))
  compile("org.eclipse.lsp4j:org.eclipse.lsp4j:0.3.0.RC1")
  compile("com.kohlschutter.junixsocket:junixsocket-native-common:2.0.4")
  compile("com.kohlschutter.junixsocket:junixsocket-common:2.0.4")
  compile("com.xenomachina:kotlin-argparser:2.0.3")
  testCompile("com.google.guava:guava:23.0")
  testCompile("com.google.truth:truth:0.35")
  testCompile("com.nhaarman:mockito-kotlin-kt1.1:1.5.0")
  testCompile("com.squareup.okio:okio:1.13.0")
}

configure<IntelliJPluginExtension> {
  version = ideaVersion
  pluginName = project.displayName
  updateSinceUntilBuild = false
  downloadSources = downloadIdeaSources.toBoolean()
  sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
}
