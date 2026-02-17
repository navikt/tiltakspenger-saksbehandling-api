package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

private enum class SøknadstypeDb {
    DIGITAL,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    ANNET,
}

fun Søknadstype.toDbValue(): String {
    return when (this) {
        Søknadstype.DIGITAL -> SøknadstypeDb.DIGITAL
        Søknadstype.PAPIR_SKJEMA -> SøknadstypeDb.PAPIR_SKJEMA
        Søknadstype.PAPIR_FRIHAND -> SøknadstypeDb.PAPIR_FRIHAND
        Søknadstype.MODIA -> SøknadstypeDb.MODIA
        Søknadstype.ANNET -> SøknadstypeDb.ANNET
    }.toString()
}

fun String.toSøknadstype(): Søknadstype {
    return when (SøknadstypeDb.valueOf(this)) {
        SøknadstypeDb.DIGITAL -> Søknadstype.DIGITAL
        SøknadstypeDb.PAPIR_SKJEMA -> Søknadstype.PAPIR_SKJEMA
        SøknadstypeDb.PAPIR_FRIHAND -> Søknadstype.PAPIR_FRIHAND
        SøknadstypeDb.MODIA -> Søknadstype.MODIA
        SøknadstypeDb.ANNET -> Søknadstype.ANNET
    }
}
