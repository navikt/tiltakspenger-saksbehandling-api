package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

fun <T : Any> T.shouldBeEqualToIgnoringLocalDateTime(
    other: T,
) = shouldBeEqualToIgnoringFieldsOfTypeRecursively(
    other = other,
    type = LocalDateTime::class,
)

fun <T : Any> T.shouldBeEqualToIgnoringFieldsOfTypeRecursively(
    other: T,
    type: KClass<*>,
    vararg otherTypes: KClass<*>,
) = this should beEqualToIgnoringFieldsOfTypeRecursively(other, type, *otherTypes)

fun <T : Any> beEqualToIgnoringFieldsOfTypeRecursively(
    other: T,
    type: KClass<*>,
    vararg otherTypes: KClass<*>,
): Matcher<T> = object : Matcher<T> {
    val types = listOf(type) + otherTypes

    override fun test(value: T): MatcherResult {
        val failures = compareRecursively(value, other, path = "")
        return MatcherResult(
            failures.isEmpty(),
            { "Objects differ:\n${failures.joinToString("\n")}" },
            { "Objects should differ but were equal ignoring ${types.map { it.simpleName }}" },
        )
    }

    fun compareRecursively(value: Any?, other: Any?, path: String): List<String> {
        if (value == null && other == null) return emptyList()
        if (value == null || other == null) return listOf("$path: expected $other but was $value")

        // Handle lists before class check — listOf() vs mutableListOf() have different runtime classes
        if (value is List<*> && other is List<*>) {
            if (value.size != other.size) return listOf("$path: expected size ${other.size} but was ${value.size}")
            return value.zip(other).flatMapIndexed { i, (a, e) ->
                compareRecursively(a, e, "$path[$i]")
            }
        }

        if (value::class != other::class) return listOf("$path: expected type ${other::class.simpleName} but was ${value::class.simpleName}")

        if (typeIsJavaOrKotlinBuiltIn(value)) {
            return if (value != other) {
                listOf("$path: expected $other but was $value")
            } else {
                emptyList()
            }
        }

        if (types.any { it == value::class }) return emptyList()

        val propsToCompare = value::class.memberProperties
            .filter { prop -> types.none { it == prop.returnType.classifier } }
            .filter { it.visibility == KVisibility.PUBLIC }

        return propsToCompare.flatMap { prop ->
            val actualVal = prop.getter.call(value)
            val expectedVal = prop.getter.call(other)
            val fieldPath = if (path.isEmpty()) prop.name else "$path.${prop.name}"
            compareRecursively(actualVal, expectedVal, fieldPath)
        }
    }
}

private fun typeIsJavaOrKotlinBuiltIn(value: Any): Boolean {
    val typeName = value::class.java.canonicalName ?: return false
    return typeName.startsWith("kotlin.") || typeName.startsWith("java.")
}
