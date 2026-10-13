package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.saksbehandling.søknad.domene.Oppholdssted

private enum class OppholdsstedDb {
    NORGE,
    ANNET_EØS_LAND,
    IKKE_EØS_LAND,
}

fun Oppholdssted.toDbValue(): String {
    return when (this) {
        Oppholdssted.NORGE -> OppholdsstedDb.NORGE
        Oppholdssted.ANNET_EØS_LAND -> OppholdsstedDb.ANNET_EØS_LAND
        Oppholdssted.IKKE_EØS_LAND -> OppholdsstedDb.IKKE_EØS_LAND
    }.toString()
}

fun String.toOppholdssted(): Oppholdssted {
    return when (OppholdsstedDb.valueOf(this)) {
        OppholdsstedDb.NORGE -> Oppholdssted.NORGE
        OppholdsstedDb.ANNET_EØS_LAND -> Oppholdssted.ANNET_EØS_LAND
        OppholdsstedDb.IKKE_EØS_LAND -> Oppholdssted.IKKE_EØS_LAND
    }
}
