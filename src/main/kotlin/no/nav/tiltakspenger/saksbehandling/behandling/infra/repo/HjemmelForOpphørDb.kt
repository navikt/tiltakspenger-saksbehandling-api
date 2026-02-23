package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrThrow
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør

private enum class HjemmelForOpphørDb {
    OPPHØR_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    OPPHØR_ALDER,
    OPPHØR_LIVSOPPHOLDSYTELSER,
    OPPHØR_KVALIFISERINGSPROGRAMMET,
    OPPHØR_INTRODUKSJONSPROGRAMMET,
    OPPHØR_LØNN_FRA_TILTAKSARRANGØR,
    OPPHØR_LØNN_FRA_ANDRE,
    OPPHØR_INSTITUSJONSOPPHOLD,
    OPPHØR_IKKE_LOVLIG_OPPHOLD,
    OPPHØR_FREMMET_FOR_SENT,
}

fun NonEmptySet<HjemmelForOpphør>.toHjemmelForOpphørDbJson(): String {
    return serialize(
        this.map {
            when (it) {
                HjemmelForOpphør.Alder -> HjemmelForOpphørDb.OPPHØR_ALDER
                HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForOpphørDb.OPPHØR_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
                HjemmelForOpphør.Institusjonsopphold -> HjemmelForOpphørDb.OPPHØR_INSTITUSJONSOPPHOLD
                HjemmelForOpphør.Introduksjonsprogrammet -> HjemmelForOpphørDb.OPPHØR_INTRODUKSJONSPROGRAMMET
                HjemmelForOpphør.Kvalifiseringsprogrammet -> HjemmelForOpphørDb.OPPHØR_KVALIFISERINGSPROGRAMMET
                HjemmelForOpphør.Livsoppholdytelser -> HjemmelForOpphørDb.OPPHØR_LIVSOPPHOLDSYTELSER
                HjemmelForOpphør.LønnFraAndre -> HjemmelForOpphørDb.OPPHØR_LØNN_FRA_ANDRE
                HjemmelForOpphør.LønnFraTiltaksarrangør -> HjemmelForOpphørDb.OPPHØR_LØNN_FRA_TILTAKSARRANGØR
                HjemmelForOpphør.IkkeLovligOpphold -> HjemmelForOpphørDb.OPPHØR_IKKE_LOVLIG_OPPHOLD
                HjemmelForOpphør.FremmetForSent -> HjemmelForOpphørDb.OPPHØR_FREMMET_FOR_SENT
            }
        }.sortedBy { it.name },
    )
}

fun String.tilHjemmelForOpphør(): NonEmptySet<HjemmelForOpphør> {
    return deserializeList<HjemmelForOpphørDb>(this).map { it.tilHjemmelForOpphør() }.toNonEmptySetOrThrow()
}

private fun HjemmelForOpphørDb.tilHjemmelForOpphør(): HjemmelForOpphør {
    return when (this) {
        HjemmelForOpphørDb.OPPHØR_DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK -> HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak
        HjemmelForOpphørDb.OPPHØR_ALDER -> HjemmelForOpphør.Alder
        HjemmelForOpphørDb.OPPHØR_LIVSOPPHOLDSYTELSER -> HjemmelForOpphør.Livsoppholdytelser
        HjemmelForOpphørDb.OPPHØR_KVALIFISERINGSPROGRAMMET -> HjemmelForOpphør.Kvalifiseringsprogrammet
        HjemmelForOpphørDb.OPPHØR_INTRODUKSJONSPROGRAMMET -> HjemmelForOpphør.Introduksjonsprogrammet
        HjemmelForOpphørDb.OPPHØR_LØNN_FRA_TILTAKSARRANGØR -> HjemmelForOpphør.LønnFraTiltaksarrangør
        HjemmelForOpphørDb.OPPHØR_LØNN_FRA_ANDRE -> HjemmelForOpphør.LønnFraAndre
        HjemmelForOpphørDb.OPPHØR_INSTITUSJONSOPPHOLD -> HjemmelForOpphør.Institusjonsopphold
        HjemmelForOpphørDb.OPPHØR_IKKE_LOVLIG_OPPHOLD -> HjemmelForOpphør.IkkeLovligOpphold
        HjemmelForOpphørDb.OPPHØR_FREMMET_FOR_SENT -> HjemmelForOpphør.FremmetForSent
    }
}
