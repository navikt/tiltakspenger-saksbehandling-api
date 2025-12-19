package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

private enum class SøknadstypeDb {
    DIGITAL,

    @Deprecated("Erstattet av mer spesifikke typer: PAPIR_SKJEMA, PAPIR_FRIHAND, MODIA og ANNET")
    PAPIR,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    OVERFORT_FRA_ARENA,
    ANNET,
}

fun Søknadstype.toDbValue(): String {
    return when (this) {
        Søknadstype.DIGITAL -> SøknadstypeDb.DIGITAL
        Søknadstype.PAPIR -> SøknadstypeDb.PAPIR
        Søknadstype.PAPIR_SKJEMA -> SøknadstypeDb.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHAND -> SøknadstypeDb.PAPIR_FRIHAND
        Søknadstype.MODIA -> SøknadstypeDb.MODIA
        Søknadstype.OVERFORT_FRA_ARENA -> SøknadstypeDb.OVERFORT_FRA_ARENA
        Søknadstype.ANNET -> SøknadstypeDb.ANNET
    }.toString()
}

fun String.toSøknadstype(): Søknadstype {
    return when (SøknadstypeDb.valueOf(this)) {
        SøknadstypeDb.DIGITAL -> Søknadstype.DIGITAL
        SøknadstypeDb.PAPIR -> Søknadstype.PAPIR
        SøknadstypeDb.PAPIR_SKJEMA -> Søknadstype.PAPIR_SKJEMA
        SøknadstypeDb.PAPIR_FRIHAND -> Søknadstype.PAPIR_FRIHAND
        SøknadstypeDb.MODIA -> Søknadstype.MODIA
        SøknadstypeDb.OVERFORT_FRA_ARENA -> Søknadstype.OVERFORT_FRA_ARENA
        SøknadstypeDb.ANNET -> Søknadstype.ANNET
    }
}
