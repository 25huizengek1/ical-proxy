package me.huizengek.icalproxy.server

import biweekly.component.VEvent
import io.ktor.http.parseUrlEncodedParameters
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.*

private typealias Parser<T> = (String) -> T

val string: Parser<String> = { it }
val boolean: Parser<Boolean> = { it != "false" && it != "0" }
val int: Parser<Int> = { it.toIntOrNull() ?: 0 }
val float: Parser<Float> = { it.toFloatOrNull() ?: 0f }
val instant: Parser<Instant> = { input ->
  input.toLongOrNull()?.let { Instant.fromEpochSeconds(it, 0) } ?: Clock.System.now()
}
val date: Parser<Date> = { input -> instant(input).toDate() }

fun <T, U> pair(first: (String) -> T, second: (String) -> U): Parser<Pair<T, U>> = { input ->
  runCatching {
    val (firstValue, secondValue) = input.split(",")
    first(firstValue) to second(secondValue)
  }
    .getOrNull() ?: badRequest("Invalid pair")
}

data class Filter<T>(
  val name: String,
  val parse: (String) -> T,
  val predicate: VEvent.(T) -> Boolean
) {
  fun apply(arg: String, invert: Boolean) = { event: VEvent ->
    val result = predicate(event, parse(arg))
    if (invert) !result else result
  }
}

val filters =
  listOf(
    Filter(
      name = "before",
      parse = date,
      predicate = { dateStart?.value?.before(it) == true }
    ),
    Filter(
      name = "after",
      parse = date,
      predicate = { dateStart?.value?.after(it) == true }
    ),
    Filter(
      name = "endsBefore",
      parse = date,
      predicate = { dateEnd?.value?.before(it) == true }
    ),
    Filter(
      name = "endsAfter",
      parse = date,
      predicate = { dateEnd?.value?.after(it) == true }
    ),
    Filter(
      name = "summaryContains",
      parse = string,
      predicate = { summary?.value?.contains(it) == true }
    ),
    Filter(
      name = "descriptionContains",
      parse = string,
      predicate = { description?.value?.contains(it) == true }
    ),
    Filter(
      name = "locationContains",
      parse = string,
      predicate = { location?.value?.contains(it) == true }
    ),
    Filter(
      name = "organizerContains",
      parse = string,
      predicate = { organizer?.email?.contains(it) == true
      }
    ),
    Filter(
      name = "fieldContains",
      parse = pair(string, string),
      predicate = { (propertyName, value) ->
        experimentalProperties
          .firstOrNull { it.name == propertyName }
          ?.value
          ?.let { value in it } == true
      }
    ),
    Filter(
      name = "summaryContainsIgnoreCase",
      parse = string,
      predicate = { summary?.value?.contains(it, ignoreCase = true) == true }
    ),
    Filter(
      name = "descriptionContainsIgnoreCase",
      parse = string,
      predicate = { description?.value?.contains(it, ignoreCase = true) == true }
    ),
    Filter(
      name = "locationContainsIgnoreCase",
      parse = string,
      predicate = { location?.value?.contains(it, ignoreCase = true) == true }
    ),
    Filter(
      name = "organizerContainsIgnoreCase",
      parse = string,
      predicate = { organizer?.email?.contains(it, ignoreCase = true) == true }
    ),
    Filter(
      name = "fieldContainsIgnoreCase",
      parse = pair(string, string),
      predicate = { (propertyName, value) ->
        experimentalProperties
          .firstOrNull { it.name.equals(propertyName, ignoreCase = true) }
          ?.value
          ?.contains(value, ignoreCase = true) == true
      }
    )
  ).associateBy { it.name.lowercase() }

fun List<VEvent>.filter(query: String): List<VEvent> {
  val parameters = query.parseUrlEncodedParameters()
  val appliedFilters = mutableListOf<(VEvent) -> Boolean>()

  parameters.forEach { key, values ->
    val not = key.startsWith("!")
    val filterName = key.trim().lowercase().let { if (not) it.substring(1) else it }
    if (filterName == "url" || filterName == "ttl") return@forEach

    val filter = filters[filterName] ?: return@forEach
    appliedFilters += filter.apply(values.last(), not)
  }

  return filter { event -> appliedFilters.all { it(event) } }
}
