package no.nav.tiltakspenger.saksbehandling.felles

/**
 * @return null, if the iterable is empty. If the iterable has more than one element, throws an [IllegalArgumentException]
 */
inline fun <T> Iterable<T>.singleOrNullOrThrow(predicate: (T) -> Boolean): T? {
    val r = this.filter(predicate)
    return when {
        r.isEmpty() -> null
        r.size == 1 -> r.single()
        else -> throw IllegalArgumentException("Collection contains more than one matching element.")
    }
}

fun <T> Collection<T>.singleOrNullOrThrow(): T? {
    return when {
        this.isEmpty() -> null
        this.size == 1 -> this.single()
        else -> throw IllegalArgumentException("Collection contains more than one matching element.")
    }
}
