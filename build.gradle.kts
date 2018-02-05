
import org.gradle.kotlin.dsl.*
import org.jetbrains.intellij.IntelliJPluginExtension

plugins {
    kotlin("jvm") version "1.2.21"
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "0.1"
    id("org.jetbrains.intellij") version "0.2.18"
    id("com.github.ben-manes.versions") version "0.17.0"
}

val ideaVersion: String by extra
val javaVersion: String by extra
val downloadIdeaSources: String by extra

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib", version = "1.2.21"))
    compile("org.eclipse.lsp4j:org.eclipse.lsp4j:0.3.0")
    compile("com.kohlschutter.junixsocket:junixsocket-native-common:2.0.4")
    compile("com.kohlschutter.junixsocket:junixsocket-common:2.0.4")
    compile("com.xenomachina:kotlin-argparser:2.0.4")
    testCompile("com.google.guava:guava:24.0-jre")
    testCompile("com.google.truth:truth:0.39")
    testCompile("com.nhaarman.mockitokotlin2:mockito-kotlin:2.0.0-alpha02")
    testCompile("com.squareup.okio:okio:1.13.0")
}

configure<IntelliJPluginExtension> {
    version = ideaVersion
    pluginName = project.displayName
    updateSinceUntilBuild = false
    downloadSources = downloadIdeaSources.toBoolean()
}

tasks.withType<Wrapper> {
    gradleVersion = "4.5"
    distributionType = Wrapper.DistributionType.ALL
}
