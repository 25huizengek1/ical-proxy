package me.huizengek.icalproxy.server

import biweekly.Biweekly
import biweekly.ICalendar
import io.github.crackthecodeabhi.kreds.args.SetOption
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val coroutineScope = CoroutineScope(Dispatchers.IO)

private suspend fun getRawCachedOrFetch(url: String, ttl: Long) =
  runCatching { redisClient?.get("raw/$url") }.printException().getOrNull()
    ?: runCatching {
      httpClient.get(url) { accept(ContentType("text", "calendar")) }.bodyAsText()
    }
      .printException()
      .getOrNull()
      ?.takeIf { it.isNotBlank() }
      ?.also {
        coroutineScope.launch {
          runCatching {
            redisClient?.set(
              key = "raw/$url",
              value = it,
              setOption = SetOption.Builder().exSeconds(ttl.toULong()).build()
            )
          }
        }
      }

private suspend fun getCalendar(url: String, ttl: Long): List<ICalendar> {
  val raw = getRawCachedOrFetch(url, ttl) ?: notFound()
  return Biweekly.parse(raw)?.all()?.takeIf { it.isNotEmpty() } ?: badRequest("Invalid calendar")
}

suspend fun getCalendarFiltered(url: String, filter: String, ttl: Long): String =
  runCatching { redisClient?.get("result/$url/$filter") }.printException().getOrNull()
    ?: run {
      val calendars = getCalendar(url, ttl)
      calendars.forEach {
        val result = it.events.filter(filter)
        it.events.clear()
        it.events.addAll(result)
      }
      Biweekly.write(calendars).go()
    }
      .also { result ->
        coroutineScope.launch {
          runCatching {
            redisClient?.set(
              key = "result/$url/$filter",
              value = result,
              setOption =
                SetOption.Builder()
                  .let { builder ->
                    redisClient
                      .expireTime("raw/$url")
                      .takeIf { it > 0L }
                      ?.let { builder.exatTimestamp(it.toULong()) }
                      ?: builder.exSeconds(ttl.toULong())
                  }
                  .build())
          }
        }
      }
