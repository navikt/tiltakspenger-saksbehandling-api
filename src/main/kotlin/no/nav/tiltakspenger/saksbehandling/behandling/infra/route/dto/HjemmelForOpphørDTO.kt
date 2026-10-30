package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrThrow
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør

enum class HjemmelForOpphørDTO {
    DeltarIkkePåArbeidsmarkedstiltak,
    Alder,
    Livsoppholdytelser,
    Kvalifiseringsprogrammet,
    Introduksjonsprogrammet,
    LønnFraTiltaksarrangør,
    LønnFraAndre,
    Institusjonsopphold,
    IkkeLovligOpphold,
    FremmetForSent,
    RettTilDagpenger,
    ;

    fun toDomain(): HjemmelForOpphør = when (this) {
        DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak
        Alder -> HjemmelForOpphør.Alder
        Livsoppholdytelser -> HjemmelForOpphør.Livsoppholdytelser
        Kvalifiseringsprogrammet -> HjemmelForOpphør.Kvalifiseringsprogrammet
        Introduksjonsprogrammet -> HjemmelForOpphør.Introduksjonsprogrammet
        LønnFraTiltaksarrangør -> HjemmelForOpphør.LønnFraTiltaksarrangør
        LønnFraAndre -> HjemmelForOpphør.LønnFraAndre
        Institusjonsopphold -> HjemmelForOpphør.Institusjonsopphold
        IkkeLovligOpphold -> HjemmelForOpphør.IkkeLovligOpphold
        FremmetForSent -> HjemmelForOpphør.FremmetForSent
        RettTilDagpenger -> HjemmelForOpphør.RettTilDagpenger
    }
}

fun List<HjemmelForOpphørDTO>.toDomain(): NonEmptySet<HjemmelForOpphør> = this.map { it.toDomain() }.toNonEmptySetOrThrow()

fun HjemmelForOpphør.tilHjemmelForOpphørDTO(): HjemmelForOpphørDTO {
    return when (this) {
        HjemmelForOpphør.Alder -> HjemmelForOpphørDTO.Alder
        HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForOpphørDTO.DeltarIkkePåArbeidsmarkedstiltak
        HjemmelForOpphør.Institusjonsopphold -> HjemmelForOpphørDTO.Institusjonsopphold
        HjemmelForOpphør.Introduksjonsprogrammet -> HjemmelForOpphørDTO.Introduksjonsprogrammet
        HjemmelForOpphør.Kvalifiseringsprogrammet -> HjemmelForOpphørDTO.Kvalifiseringsprogrammet
        HjemmelForOpphør.Livsoppholdytelser -> HjemmelForOpphørDTO.Livsoppholdytelser
        HjemmelForOpphør.LønnFraAndre -> HjemmelForOpphørDTO.LønnFraAndre
        HjemmelForOpphør.LønnFraTiltaksarrangør -> HjemmelForOpphørDTO.LønnFraTiltaksarrangør
        HjemmelForOpphør.IkkeLovligOpphold -> HjemmelForOpphørDTO.IkkeLovligOpphold
        HjemmelForOpphør.FremmetForSent -> HjemmelForOpphørDTO.FremmetForSent
        HjemmelForOpphør.RettTilDagpenger -> HjemmelForOpphørDTO.RettTilDagpenger
    }
}

fun NonEmptySet<HjemmelForOpphør>.tilHjemmelForOpphørDTO(): NonEmptySet<HjemmelForOpphørDTO> {
    return this.map { it.tilHjemmelForOpphørDTO() }.sortedBy { it.name }.toNonEmptySetOrThrow()
}
