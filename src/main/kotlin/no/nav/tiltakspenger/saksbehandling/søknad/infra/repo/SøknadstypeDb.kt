package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

private enum class SøknadstypeDb {
    DIGITAL,

    @Deprecated("Erstattet av mer spesifikke typer: PAPIR_SKJEMA, PAPIR_FRIHÅND, MODIA og ANNET")
    PAPIR,
    PAPIR_SKJEMA,
    PAPIR_FRIHÅND,
    MODIA,
    ANNET,
}

fun Søknadstype.toDbValue(): String {
    return when (this) {
        Søknadstype.DIGITAL -> SøknadstypeDb.DIGITAL
        Søknadstype.PAPIR -> SøknadstypeDb.PAPIR
        Søknadstype.PAPIR_SKJEMA -> SøknadstypeDb.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHÅND -> SøknadstypeDb.PAPIR_FRIHÅND
        Søknadstype.MODIA -> SøknadstypeDb.MODIA
        Søknadstype.ANNET -> SøknadstypeDb.ANNET
    }.toString()
}

fun String.toSøknadstype(): Søknadstype {
    return when (SøknadstypeDb.valueOf(this)) {
        SøknadstypeDb.DIGITAL -> Søknadstype.DIGITAL
        SøknadstypeDb.PAPIR -> Søknadstype.PAPIR
        SøknadstypeDb.PAPIR_SKJEMA -> Søknadstype.PAPIR_SKJEMA
        SøknadstypeDb.PAPIR_FRIHÅND -> Søknadstype.PAPIR_SKJEMA
        SøknadstypeDb.MODIA -> Søknadstype.MODIA
        SøknadstypeDb.ANNET -> Søknadstype.ANNET
    }
}
