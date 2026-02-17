package no.nav.tiltakspenger.saksbehandling.søknad.domene

enum class Søknadstype {
    DIGITAL,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,

    // Dekker cases hvor ingen av de andre typene passer.
    ANNET,
}
