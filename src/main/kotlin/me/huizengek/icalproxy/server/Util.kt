package me.huizengek.icalproxy.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.routing.RoutingContext
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.*

fun RoutingContext.nullableQueryParameter(parameter: String) =
  call.queryParameters[parameter]?.trim()?.takeIf { it.isNotEmpty() }

fun RoutingContext.queryParameter(parameter: String) = nullableQueryParameter(parameter)
    ?: badRequest("Invalid parameter ${parameter}.")

class HttpResponseException(val code: HttpStatusCode, val body: String, cause: Throwable? = null) :
  RuntimeException(body, cause)

fun badRequest(message: String, code: HttpStatusCode = HttpStatusCode.BadRequest): Nothing =
  throw HttpResponseException(code, message)

fun notFound(): Nothing = throw NotFoundException()

fun <T> Result<T>.printException() = also { it.exceptionOrNull()?.printStackTrace() }

fun Instant.toDate(): Date = Date.from(toJavaInstant())
