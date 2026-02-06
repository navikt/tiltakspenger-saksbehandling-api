package no.nav.tiltakspenger.saksbehandling.felles

import arrow.core.Either

fun <A, B> Either<A, B>.getOrThrow(): B {
    return fold(
        { throw NoSuchElementException("Either.Left: $it") },
        { it },
    )
}
