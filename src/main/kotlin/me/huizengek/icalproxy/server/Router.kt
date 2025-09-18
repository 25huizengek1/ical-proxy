package me.huizengek.icalproxy.server

import io.ktor.http.ContentType
import io.ktor.server.request.queryString
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.mainRouter() {
  get("/") {
    val url = queryParameter("url")
    val ttl = call.queryParameters["ttl"]?.toLongOrNull() ?: 3600L

    call.respondText(
      text = getCalendarFiltered(
        url = url,
        filter = call.request.queryString(),
        ttl = ttl
      ),
      contentType = ContentType("text", "calendar")
    )
  }
}
