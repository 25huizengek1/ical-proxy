package me.huizengek.icalproxy.server

import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

val httpClient = HttpClient(CIO) {
  install(Logging) { level = LogLevel.ALL }
  install(ContentNegotiation) { json(json) }
}

val redisClient =
  runCatching { newClient(Endpoint.from(Environment.REDIS_ENDPOINT)) }
    .printException()
    .getOrNull()
