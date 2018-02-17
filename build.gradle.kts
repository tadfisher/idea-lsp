
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include
import org.gradle.kotlin.dsl.*
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.platform.gradle.plugin.EnginesExtension

plugins {
    kotlin("jvm") version "1.2.21"
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "0.1"
    id("org.jetbrains.intellij") version "0.2.18"
    id("org.junit.platform.gradle.plugin") version "1.1.0-RC1"
    id("com.github.ben-manes.versions") version "0.17.0"
}

val ideaVersion: String by extra
val javaVersion: String by extra
val kotlinVersion: String by extra
val spekVersion: String by extra
val downloadIdeaSources: String by extra

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://oss.jfrog.org/artifactory/libs-snapshot") }
}

dependencies {
    implementation(kotlin("stdlib-jre8", version = kotlinVersion))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.3.0")
    implementation("com.kohlschutter.junixsocket:junixsocket-native-common:2.0.4")
    implementation("com.kohlschutter.junixsocket:junixsocket-common:2.0.4")
    implementation("com.xenomachina:kotlin-argparser:2.0.4")

    testImplementation("com.google.guava:guava:24.0-jre")
    testImplementation("com.google.truth:truth:0.39")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.0.0-alpha02")
    testImplementation("com.squareup.okio:okio:1.13.0")
    testImplementation(kotlin("reflect", version = kotlinVersion))
    testImplementation(kotlin("test", version = kotlinVersion))
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
        exclude(group = "org.junit.platform")
        exclude(group = "org.jetbrains.kotlin")
    }
}

intellij {
    version = ideaVersion
    pluginName = project.displayName
    updateSinceUntilBuild = false
    downloadSources = downloadIdeaSources.toBoolean()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Wrapper> {
    gradleVersion = "4.5"
    distributionType = Wrapper.DistributionType.ALL
}

junitPlatform {
    filters {
        engines {
            include("spek")
        }
    }
}
