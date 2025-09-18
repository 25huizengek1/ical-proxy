package me.huizengek.icalproxy.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    val debug = System.getenv("DEBUG") == "1"
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val host = if (debug) "0.0.0.0" else "127.0.0.1"

    println("Starting Ktor server on ${host}:${port}")
    println("Please note this server should always be behind a trusted reverse proxy, since this server assumes the X-Forwarded-For header is valid")

    embeddedServer(
        factory = Netty,
        port = port,
        host = host
    ) {
        processing()
        monitoring()
        contentNegotation()

        routing {
            mainRouter()
        }
    }.start(wait = true)
}

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = true
}

private fun Application.contentNegotation() {
    install(ContentNegotiation) {
        json(json)
    }
}

private fun Application.monitoring() {
    install(CallLogging) {
        level = Level.INFO
    }
}

private fun Application.processing() {
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        // TODO: define constraints before going in prod
        anyHost()
    }
    install(ConditionalHeaders)
    install(Compression) {
        gzip {
            priority = 1.0
        }
    }
}
