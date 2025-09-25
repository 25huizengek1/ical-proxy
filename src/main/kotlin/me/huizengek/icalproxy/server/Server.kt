package me.huizengek.icalproxy.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import kotlin.properties.ReadOnlyProperty

object Environment {
  private fun env(default: String) =
    ReadOnlyProperty<Any?, String> { _, property -> System.getenv(property.name) ?: default }

  val DEBUG by env("0")
  val PORT by env("8080")
  val HOST by env(if (DEBUG == "1") "0.0.0.0" else "127.0.0.1")
  val REDIS_ENDPOINT by env("127.0.0.1:6379")
  val ROOT_URL by env("https://omeduostuurcentenneef.nl")
}

fun main() {
  println("Starting Ktor server on ${Environment.HOST}:${Environment.PORT}")
  println(
    "Please note this server should always be behind a trusted reverse proxy, since this server assumes the X-Forwarded-For header is valid"
  )

  embeddedServer(
    factory = Netty, port = Environment.PORT.toIntOrNull() ?: 8080, host = Environment.HOST
  ) {
    errors()
    processing()
    monitoring()
    contentNegotation()

    routing { mainRouter() }
  }.start(wait = true)
}

val json = Json {
  isLenient = true
  ignoreUnknownKeys = true
  prettyPrint = true
  encodeDefaults = true
  explicitNulls = true
}

private fun Application.errors() {
  install(StatusPages) {
    exception<Throwable> { call, cause ->
      when (cause) {
        is HttpResponseException -> call.respondText(text = cause.body, status = cause.code)
        is NotFoundException ->
          call.respondText(
            text = "404: ${call.request.path()} not found", status = HttpStatusCode.NotFound
          )

        else -> call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
      }
    }
  }
}

private fun Application.contentNegotation() {
  install(ContentNegotiation) { json(json) }
}

private fun Application.monitoring() {
  install(CallLogging) { level = Level.INFO }
}

private fun Application.processing() {
  install(ForwardedHeaders)
  install(XForwardedHeaders)
  install(DefaultHeaders) { header("X-Engine", "Ktor") }
  install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowHeader(HttpHeaders.Authorization)
    allowNonSimpleContentTypes = true
    allowCredentials = true
    anyHost()
  }
  install(ConditionalHeaders)
  install(Compression) { gzip { priority = 1.0 } }
}
