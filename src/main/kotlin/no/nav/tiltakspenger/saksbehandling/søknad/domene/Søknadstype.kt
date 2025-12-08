package no.nav.tiltakspenger.saksbehandling.søknad.domene

enum class Søknadstype {
    DIGITAL,

    @Deprecated("Erstattet av MANUELT_REGISTRERT_SØKNAD, beholdes til migrering er fullført")
    PAPIR,
    MANUELT_REGISTRERT_SØKNAD,
}
