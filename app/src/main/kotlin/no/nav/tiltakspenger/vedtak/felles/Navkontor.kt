package no.nav.tiltakspenger.vedtak.felles

/**
 * Også kallt kontornummer/enhetsnummer.
 * Inngang befolkning: https://www.nav.no/sok-nav-kontor eksempel https://www.nav.no/kontor/nav-asker
 * Se også etterlatte sin take på det samme: https://github.com/navikt/pensjon-etterlatte-saksbehandling/blob/main/libs/saksbehandling-common/src/main/kotlin/Enhetsnummer.kt
 */
data class Navkontor(
    val kontornummer: String,
    val kontornavn: String?,
)
