plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ktor)
}

group = "me.huizengek"
version = "1.0.0"

configurations.all {
  resolutionStrategy {
    failOnNonReproducibleResolution()
  }
}

tasks.withType<AbstractArchiveTask>().configureEach {
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
  dirPermissions { unix("755") }
  filePermissions { unix("644") }
}

dependencies {
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.callLogging)
  implementation(libs.ktor.server.forwardedHeader)
  implementation(libs.ktor.server.defaultHeaders)
  implementation(libs.ktor.server.conditionalHeaders)
  implementation(libs.ktor.server.cors)
  implementation(libs.ktor.server.compression)
  implementation(libs.ktor.server.statusPages)

  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.logging)

  implementation(libs.ktor.serialization.client)
  implementation(libs.ktor.serialization.server)
  implementation(libs.ktor.serialization.json)

  implementation(libs.kreds)
  implementation(libs.biweekly)

  implementation(libs.logback.classic)
}

kotlin {
  jvmToolchain(21)
}

application {
  mainClass.set("me.huizengek.icalproxy.server.ServerKt")
}
