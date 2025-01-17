package no.nav.tiltakspenger.felles

fun <T, K> List<T>.nonDistinctBy(selector: (T) -> K): List<T> {
    return this.groupBy(selector).values.filter { it.size > 1 }.flatten()
}
