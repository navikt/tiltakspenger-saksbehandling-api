package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans

private enum class ValgtHjemmelForStansDb {
    STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    STANS_ALDER,
    STANS_LIVSOPPHOLDSYTELSER,
    STANS_KVALIFISERINGSPROGRAMMET,
    STANS_INTRODUKSJONSPROGRAMMET,
    STANS_LØNN_FRA_TILTAKSARRANGØR,
    STANS_LØNN_FRA_ANDRE,
    STANS_INSTITUSJONSOPPHOLD,
}

fun List<ValgtHjemmelForStans>.toDbJson(): String {
    return serialize(
        this.map {
            when (it) {
                ValgtHjemmelForStans.Alder -> ValgtHjemmelForStansDb.STANS_ALDER
                ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelForStansDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
                ValgtHjemmelForStans.Institusjonsopphold -> ValgtHjemmelForStansDb.STANS_INSTITUSJONSOPPHOLD
                ValgtHjemmelForStans.Introduksjonsprogrammet -> ValgtHjemmelForStansDb.STANS_INTRODUKSJONSPROGRAMMET
                ValgtHjemmelForStans.Kvalifiseringsprogrammet -> ValgtHjemmelForStansDb.STANS_KVALIFISERINGSPROGRAMMET
                ValgtHjemmelForStans.Livsoppholdytelser -> ValgtHjemmelForStansDb.STANS_LIVSOPPHOLDSYTELSER
                ValgtHjemmelForStans.LønnFraAndre -> ValgtHjemmelForStansDb.STANS_LØNN_FRA_ANDRE
                ValgtHjemmelForStans.LønnFraTiltaksarrangør -> ValgtHjemmelForStansDb.STANS_LØNN_FRA_TILTAKSARRANGØR
            }
        },
    )
}

fun String.tilHjemmelForStans(): List<ValgtHjemmelForStans> {
    return deserializeList<ValgtHjemmelForStansDb>(this).map { it.tilHjemmelForStans() }
}

private fun ValgtHjemmelForStansDb.tilHjemmelForStans(): ValgtHjemmelForStans {
    return when (this) {
        ValgtHjemmelForStansDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak
        ValgtHjemmelForStansDb.STANS_ALDER -> ValgtHjemmelForStans.Alder
        ValgtHjemmelForStansDb.STANS_LIVSOPPHOLDSYTELSER -> ValgtHjemmelForStans.Livsoppholdytelser
        ValgtHjemmelForStansDb.STANS_KVALIFISERINGSPROGRAMMET -> ValgtHjemmelForStans.Kvalifiseringsprogrammet
        ValgtHjemmelForStansDb.STANS_INTRODUKSJONSPROGRAMMET -> ValgtHjemmelForStans.Introduksjonsprogrammet
        ValgtHjemmelForStansDb.STANS_LØNN_FRA_TILTAKSARRANGØR -> ValgtHjemmelForStans.LønnFraTiltaksarrangør
        ValgtHjemmelForStansDb.STANS_LØNN_FRA_ANDRE -> ValgtHjemmelForStans.LønnFraAndre
        ValgtHjemmelForStansDb.STANS_INSTITUSJONSOPPHOLD -> ValgtHjemmelForStans.Institusjonsopphold
    }
}
