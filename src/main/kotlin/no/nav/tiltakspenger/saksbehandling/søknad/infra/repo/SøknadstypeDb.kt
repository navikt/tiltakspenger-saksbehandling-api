package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

private enum class SøknadstypeDb {
    DIGITAL,
    PAPIR,
}

fun Søknadstype.toDbValue(): String {
    return when (this) {
        Søknadstype.DIGITAL -> SøknadstypeDb.DIGITAL
        Søknadstype.PAPIR -> SøknadstypeDb.PAPIR
    }.toString()
}

fun String.toSøknadstype(): Søknadstype {
    return when (SøknadstypeDb.valueOf(this)) {
        SøknadstypeDb.DIGITAL -> Søknadstype.DIGITAL
        SøknadstypeDb.PAPIR -> Søknadstype.PAPIR
    }
}
