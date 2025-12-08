package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype

private enum class SøknadstypeDb {
    DIGITAL,
    PAPIR,
    MANUELT_REGISTRERT_SØKNAD,
}

fun Søknadstype.toDbValue(): String {
    return when (this) {
        Søknadstype.DIGITAL -> SøknadstypeDb.DIGITAL
        Søknadstype.PAPIR -> SøknadstypeDb.PAPIR
        Søknadstype.MANUELT_REGISTRERT_SØKNAD -> SøknadstypeDb.MANUELT_REGISTRERT_SØKNAD
    }.toString()
}

fun String.toSøknadstype(): Søknadstype {
    return when (SøknadstypeDb.valueOf(this)) {
        SøknadstypeDb.DIGITAL -> Søknadstype.DIGITAL
        SøknadstypeDb.PAPIR -> Søknadstype.PAPIR
        SøknadstypeDb.MANUELT_REGISTRERT_SØKNAD -> Søknadstype.MANUELT_REGISTRERT_SØKNAD
    }
}
