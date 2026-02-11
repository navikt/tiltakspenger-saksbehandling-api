package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrThrow
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør

private enum class HjemmelForOpphørDb {
    OPPHØR_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    OPPHØR_ALDER,
    OPPHØR_LIVSOPPHOLDSYTELSER,
    OPPHØR_KVALIFISERINGSPROGRAMMET,
    OPPHØR_INTRODUKSJONSPROGRAMMET,
    OPPHØR_LØNN_FRA_TILTAKSARRANGØR,
    OPPHØR_LØNN_FRA_ANDRE,
    OPPHØR_INSTITUSJONSOPPHOLD,
}

fun NonEmptySet<HjemmelForStansEllerOpphør>.toHjemmelForOpphørDbJson(): String {
    return serialize(
        this.map {
            when (it) {
                HjemmelForStansEllerOpphør.Alder -> HjemmelForOpphørDb.OPPHØR_ALDER
                HjemmelForStansEllerOpphør.DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForOpphørDb.OPPHØR_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
                HjemmelForStansEllerOpphør.Institusjonsopphold -> HjemmelForOpphørDb.OPPHØR_INSTITUSJONSOPPHOLD
                HjemmelForStansEllerOpphør.Introduksjonsprogrammet -> HjemmelForOpphørDb.OPPHØR_INTRODUKSJONSPROGRAMMET
                HjemmelForStansEllerOpphør.Kvalifiseringsprogrammet -> HjemmelForOpphørDb.OPPHØR_KVALIFISERINGSPROGRAMMET
                HjemmelForStansEllerOpphør.Livsoppholdytelser -> HjemmelForOpphørDb.OPPHØR_LIVSOPPHOLDSYTELSER
                HjemmelForStansEllerOpphør.LønnFraAndre -> HjemmelForOpphørDb.OPPHØR_LØNN_FRA_ANDRE
                HjemmelForStansEllerOpphør.LønnFraTiltaksarrangør -> HjemmelForOpphørDb.OPPHØR_LØNN_FRA_TILTAKSARRANGØR
            }
        }.sortedBy { it.name },
    )
}

fun String.tilHjemmelForOpphør(): NonEmptySet<HjemmelForStansEllerOpphør> {
    return deserializeList<HjemmelForOpphørDb>(this).map { it.tilHjemmelForOpphør() }.toNonEmptySetOrThrow()
}

private fun HjemmelForOpphørDb.tilHjemmelForOpphør(): HjemmelForStansEllerOpphør {
    return when (this) {
        HjemmelForOpphørDb.OPPHØR_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> HjemmelForStansEllerOpphør.DeltarIkkePåArbeidsmarkedstiltak
        HjemmelForOpphørDb.OPPHØR_ALDER -> HjemmelForStansEllerOpphør.Alder
        HjemmelForOpphørDb.OPPHØR_LIVSOPPHOLDSYTELSER -> HjemmelForStansEllerOpphør.Livsoppholdytelser
        HjemmelForOpphørDb.OPPHØR_KVALIFISERINGSPROGRAMMET -> HjemmelForStansEllerOpphør.Kvalifiseringsprogrammet
        HjemmelForOpphørDb.OPPHØR_INTRODUKSJONSPROGRAMMET -> HjemmelForStansEllerOpphør.Introduksjonsprogrammet
        HjemmelForOpphørDb.OPPHØR_LØNN_FRA_TILTAKSARRANGØR -> HjemmelForStansEllerOpphør.LønnFraTiltaksarrangør
        HjemmelForOpphørDb.OPPHØR_LØNN_FRA_ANDRE -> HjemmelForStansEllerOpphør.LønnFraAndre
        HjemmelForOpphørDb.OPPHØR_INSTITUSJONSOPPHOLD -> HjemmelForStansEllerOpphør.Institusjonsopphold
    }
}
