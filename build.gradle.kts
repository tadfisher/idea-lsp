plugins {
  kotlin("jvm")
  id("org.jetbrains.intellij") version "0.2.11"
}

intellij {
  updateSinceUntilBuild(false)
  version("IC-2017.1.3")
  pluginName = "idea-lsp"
}

dependencies {
  compile(kotlin("stdlib"))
}

repositories {
  jcenter()
}

task(name = "wrapper", type = Wrapper) {
  gradleVersion = "4.0"
}