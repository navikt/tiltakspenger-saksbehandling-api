package no.nav.tiltakspenger.saksbehandling.søknad.domene

enum class Søknadstype {
    DIGITAL,

    @Deprecated("Erstattet av mer spesifikke typer: PAPIR_SKJEMA, PAPIR_FRIHAND, MODIA og ANNET")
    PAPIR,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    OVERFORT_FRA_ARENA,

    // Dekker cases hvor ingen av de andre typene passer.
    ANNET,
}
