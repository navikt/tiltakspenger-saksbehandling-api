package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør

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

fun NonEmptySet<HjemmelForStansEllerOpphør>?.toHjemmelForStansDbJson(): String {
    if (this == null) return "[]"
    return serialize(
        this.map {
            when (it) {
                HjemmelForStansEllerOpphør.Alder -> HjemmelForStansDb.STANS_ALDER
                HjemmelForStansEllerOpphør.DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForStansDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
                HjemmelForStansEllerOpphør.Institusjonsopphold -> HjemmelForStansDb.STANS_INSTITUSJONSOPPHOLD
                HjemmelForStansEllerOpphør.Introduksjonsprogrammet -> HjemmelForStansDb.STANS_INTRODUKSJONSPROGRAMMET
                HjemmelForStansEllerOpphør.Kvalifiseringsprogrammet -> HjemmelForStansDb.STANS_KVALIFISERINGSPROGRAMMET
                HjemmelForStansEllerOpphør.Livsoppholdytelser -> HjemmelForStansDb.STANS_LIVSOPPHOLDSYTELSER
                HjemmelForStansEllerOpphør.LønnFraAndre -> HjemmelForStansDb.STANS_LØNN_FRA_ANDRE
                HjemmelForStansEllerOpphør.LønnFraTiltaksarrangør -> HjemmelForStansDb.STANS_LØNN_FRA_TILTAKSARRANGØR
                HjemmelForStansEllerOpphør.IkkeLovligOpphold -> HjemmelForStansDb.STANS_IKKE_LOVLIG_OPPHOLD
            }
        }.sortedBy { it.name },
    )
}

fun String.tilHjemmelForStans(): List<HjemmelForStansEllerOpphør> {
    return deserializeList<HjemmelForStansDb>(this).map { it.tilHjemmelForStans() }
}

private fun HjemmelForStansDb.tilHjemmelForStans(): HjemmelForStansEllerOpphør {
    return when (this) {
        HjemmelForStansDb.STANS_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> HjemmelForStansEllerOpphør.DeltarIkkePåArbeidsmarkedstiltak
        HjemmelForStansDb.STANS_ALDER -> HjemmelForStansEllerOpphør.Alder
        HjemmelForStansDb.STANS_LIVSOPPHOLDSYTELSER -> HjemmelForStansEllerOpphør.Livsoppholdytelser
        HjemmelForStansDb.STANS_KVALIFISERINGSPROGRAMMET -> HjemmelForStansEllerOpphør.Kvalifiseringsprogrammet
        HjemmelForStansDb.STANS_INTRODUKSJONSPROGRAMMET -> HjemmelForStansEllerOpphør.Introduksjonsprogrammet
        HjemmelForStansDb.STANS_LØNN_FRA_TILTAKSARRANGØR -> HjemmelForStansEllerOpphør.LønnFraTiltaksarrangør
        HjemmelForStansDb.STANS_LØNN_FRA_ANDRE -> HjemmelForStansEllerOpphør.LønnFraAndre
        HjemmelForStansDb.STANS_INSTITUSJONSOPPHOLD -> HjemmelForStansEllerOpphør.Institusjonsopphold
        HjemmelForStansDb.STANS_IKKE_LOVLIG_OPPHOLD -> HjemmelForStansEllerOpphør.IkkeLovligOpphold
    }
}
