package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

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
    private val ignoredTypes = setOf(type, *otherTypes)

    override fun test(value: T): MatcherResult {
        val failures = compareRecursively(value, other, path = "")
        return MatcherResult(
            failures.isEmpty(),
            { "Objects differ:\n${failures.joinToString("\n")}" },
            { "Objects should differ but were equal ignoring ${ignoredTypes.map { it.simpleName }}" },
        )
    }

    fun compareRecursively(value: Any?, other: Any?, path: String): List<String> {
        val normalizedValue = normalizeBoxedNullValueClass(value)
        val normalizedOther = normalizeBoxedNullValueClass(other)

        if (normalizedValue == null && normalizedOther == null) return emptyList()
        if (normalizedValue == null || normalizedOther == null) return listOf(mismatch(path, normalizedValue, normalizedOther))

        compareMap(normalizedValue, normalizedOther, path)?.let { return it }
        compareSet(normalizedValue, normalizedOther, path)?.let { return it }
        compareIterable(normalizedValue, normalizedOther, path)?.let { return it }

        if (normalizedValue::class != normalizedOther::class) return listOf(typeMismatch(path, normalizedValue, normalizedOther))
        if (normalizedValue::class in ignoredTypes) return emptyList()
        if (normalizedValue::class.isValue) return compareBuiltIn(normalizedValue, normalizedOther, path)
        if (typeIsJavaOrKotlinBuiltIn(normalizedValue)) return compareBuiltIn(normalizedValue, normalizedOther, path)

        return compareObject(normalizedValue, normalizedOther, path)
    }

    private fun compareMap(value: Any, other: Any, path: String): List<String>? {
        if (value !is Map<*, *> && other !is Map<*, *>) return null

        val valueMap = value as? Map<*, *> ?: return listOf(mismatch(path, value, other))
        val otherMap = other as? Map<*, *> ?: return listOf(mismatch(path, value, other))

        if (valueMap.size != otherMap.size) return listOf(sizeMismatch(path, valueMap.size, otherMap.size))

        return valueMap.keys.sortedBy { it.toString() }.flatMap { key ->
            if (!otherMap.containsKey(key)) {
                listOf("$path[$key]: missing key")
            } else {
                compareRecursively(valueMap[key], otherMap[key], "$path[$key]")
            }
        }
    }

    private fun compareSet(value: Any, other: Any, path: String): List<String>? {
        if (value !is Set<*> && other !is Set<*>) return null

        val valueSet = value as? Set<*> ?: return listOf(mismatch(path, value, other))
        val otherSet = other as? Set<*> ?: return listOf(mismatch(path, value, other))

        if (valueSet.size != otherSet.size) return listOf(sizeMismatch(path, valueSet.size, otherSet.size))

        val remaining = otherSet.toMutableList()
        return buildList {
            valueSet.forEach { actualElement ->
                val matchIndex = remaining.indexOfFirst { expectedElement ->
                    compareRecursively(actualElement, expectedElement, path).isEmpty()
                }

                if (matchIndex == -1) {
                    add("$path: missing element ${safeValueString(actualElement)}")
                } else {
                    remaining.removeAt(matchIndex)
                }
            }
        }
    }

    private fun compareIterable(value: Any, other: Any, path: String): List<String>? {
        if (value !is Iterable<*> && other !is Iterable<*>) return null

        val valueList = (value as? Iterable<*>)?.toListOrNullForBoxedNullValueClass()
        val otherList = (other as? Iterable<*>)?.toListOrNullForBoxedNullValueClass()

        if (valueList == null && otherList == null) return emptyList()
        if (valueList == null || otherList == null) return listOf(mismatch(path, value, other))

        if (valueList.size != otherList.size) return listOf(sizeMismatch(path, valueList.size, otherList.size))

        return valueList.zip(otherList).flatMapIndexed { index, (actualElement, expectedElement) ->
            compareRecursively(actualElement, expectedElement, "$path[$index]")
        }
    }

    private fun compareBuiltIn(value: Any, other: Any, path: String): List<String> =
        if (value != other) listOf(mismatch(path, value, other)) else emptyList()

    private fun compareObject(value: Any, other: Any, path: String): List<String> =
        propertiesToCompare(value::class)
            .sortedBy { it.name }
            .flatMap { prop ->
                prop.isAccessible = true
                val actualValue = prop.getter.call(value)
                val expectedValue = prop.getter.call(other)
                val fieldPath = if (path.isEmpty()) prop.name else "$path.${prop.name}"

                if (prop.returnType.classifier in ignoredTypes) {
                    compareIgnoredField(actualValue, expectedValue, fieldPath)
                } else {
                    compareRecursively(actualValue, expectedValue, fieldPath)
                }
            }

    private fun propertiesToCompare(type: KClass<*>) =
        type.memberProperties.filter { prop ->
            prop.visibility == KVisibility.PUBLIC || prop.name in constructorPropertyNames(type)
        }

    private fun constructorPropertyNames(type: KClass<*>) =
        type.primaryConstructor?.parameters
            ?.mapNotNull { it.name }
            ?.toSet()
            .orEmpty()

    private fun compareIgnoredField(value: Any?, other: Any?, path: String): List<String> =
        when {
            value == null && other == null -> emptyList()
            value == null || other == null -> listOf(mismatch(path, value, other))
            else -> emptyList()
        }

    private fun mismatch(path: String, value: Any?, other: Any?): String =
        "$path: expected ${safeValueString(other)} but was ${safeValueString(value)}"

    private fun sizeMismatch(path: String, valueSize: Int, otherSize: Int): String =
        "$path: expected size $otherSize but was $valueSize"

    private fun typeMismatch(path: String, value: Any, other: Any): String =
        "$path: expected type ${other::class.simpleName} but was ${value::class.simpleName}"
}

private fun normalizeBoxedNullValueClass(value: Any?): Any? {
    if (value == null || !value::class.isValue) return value

    val underlyingProperty = value::class.memberProperties.singleOrNull() ?: return value
    underlyingProperty.isAccessible = true

    val underlyingValue = runCatching { underlyingProperty.getter.call(value) }.getOrElse { return value }
    return if (underlyingValue == null) null else value
}

private fun Iterable<*>.toListOrNullForBoxedNullValueClass(): List<Any?>? =
    runCatching { this.toList() }
        .getOrElse {
            if (it is NullPointerException && this::class.isValue) {
                null
            } else {
                throw it
            }
        }

private fun safeValueString(value: Any?): String =
    runCatching { value.toString() }
        .getOrElse { value?.let { "${it::class.simpleName}(unprintable)" } ?: "null" }

private fun typeIsJavaOrKotlinBuiltIn(value: Any): Boolean {
    val typeName = value::class.java.canonicalName ?: return false
    return typeName.startsWith("kotlin.") || typeName.startsWith("java.")
}
