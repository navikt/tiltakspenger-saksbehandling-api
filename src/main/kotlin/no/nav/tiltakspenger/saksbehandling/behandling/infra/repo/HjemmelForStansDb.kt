package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans

private enum class HjemmelForStansDb {
    STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    STANS_ALDER,
    STANS_LIVSOPPHOLDSYTELSER,
    STANS_KVALIFISERINGSPROGRAMMET,
    STANS_INTRODUKSJONSPROGRAMMET,
    STANS_LØNN_FRA_TILTAKSARRANGØR,
    STANS_LØNN_FRA_ANDRE,
    STANS_INSTITUSJONSOPPHOLD,
    STANS_IKKE_LOVLIG_OPPHOLD,
}

fun NonEmptySet<HjemmelForStans>?.toHjemmelForStansDbJson(): String {
    if (this == null) return "[]"
    return serialize(
        this.map {
            when (it) {
                HjemmelForStans.Alder -> HjemmelForStansDb.STANS_ALDER
                HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForStansDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
                HjemmelForStans.Institusjonsopphold -> HjemmelForStansDb.STANS_INSTITUSJONSOPPHOLD
                HjemmelForStans.Introduksjonsprogrammet -> HjemmelForStansDb.STANS_INTRODUKSJONSPROGRAMMET
                HjemmelForStans.Kvalifiseringsprogrammet -> HjemmelForStansDb.STANS_KVALIFISERINGSPROGRAMMET
                HjemmelForStans.Livsoppholdytelser -> HjemmelForStansDb.STANS_LIVSOPPHOLDSYTELSER
                HjemmelForStans.LønnFraAndre -> HjemmelForStansDb.STANS_LØNN_FRA_ANDRE
                HjemmelForStans.LønnFraTiltaksarrangør -> HjemmelForStansDb.STANS_LØNN_FRA_TILTAKSARRANGØR
                HjemmelForStans.IkkeLovligOpphold -> HjemmelForStansDb.STANS_IKKE_LOVLIG_OPPHOLD
            }
        }.sortedBy { it.name },
    )
}

fun String.tilHjemmelForStans(): List<HjemmelForStans> {
    return deserializeList<HjemmelForStansDb>(this).map { it.tilHjemmelForStans() }
}

private fun HjemmelForStansDb.tilHjemmelForStans(): HjemmelForStans {
    return when (this) {
        HjemmelForStansDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak
        HjemmelForStansDb.STANS_ALDER -> HjemmelForStans.Alder
        HjemmelForStansDb.STANS_LIVSOPPHOLDSYTELSER -> HjemmelForStans.Livsoppholdytelser
        HjemmelForStansDb.STANS_KVALIFISERINGSPROGRAMMET -> HjemmelForStans.Kvalifiseringsprogrammet
        HjemmelForStansDb.STANS_INTRODUKSJONSPROGRAMMET -> HjemmelForStans.Introduksjonsprogrammet
        HjemmelForStansDb.STANS_LØNN_FRA_TILTAKSARRANGØR -> HjemmelForStans.LønnFraTiltaksarrangør
        HjemmelForStansDb.STANS_LØNN_FRA_ANDRE -> HjemmelForStans.LønnFraAndre
        HjemmelForStansDb.STANS_INSTITUSJONSOPPHOLD -> HjemmelForStans.Institusjonsopphold
        HjemmelForStansDb.STANS_IKKE_LOVLIG_OPPHOLD -> HjemmelForStans.IkkeLovligOpphold
    }
}
