package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør

enum class HjemmelForStansEllerOpphørDTO {
    DeltarIkkePåArbeidsmarkedstiltak,
    Alder,
    Livsoppholdytelser,
    Kvalifiseringsprogrammet,
    Introduksjonsprogrammet,
    LønnFraTiltaksarrangør,
    LønnFraAndre,
    Institusjonsopphold,
    ;

    fun toDomain(): HjemmelForStansEllerOpphør = when (this) {
        DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForStansEllerOpphør.DeltarIkkePåArbeidsmarkedstiltak
        Alder -> HjemmelForStansEllerOpphør.Alder
        Livsoppholdytelser -> HjemmelForStansEllerOpphør.Livsoppholdytelser
        Kvalifiseringsprogrammet -> HjemmelForStansEllerOpphør.Kvalifiseringsprogrammet
        Introduksjonsprogrammet -> HjemmelForStansEllerOpphør.Introduksjonsprogrammet
        LønnFraTiltaksarrangør -> HjemmelForStansEllerOpphør.LønnFraTiltaksarrangør
        LønnFraAndre -> HjemmelForStansEllerOpphør.LønnFraAndre
        Institusjonsopphold -> HjemmelForStansEllerOpphør.Institusjonsopphold
    }
}

fun List<HjemmelForStansEllerOpphørDTO>.toDomain(): List<HjemmelForStansEllerOpphør> = this.map { it.toDomain() }

fun HjemmelForStansEllerOpphør.tilDTO(): HjemmelForStansEllerOpphørDTO {
    return when (this) {
        HjemmelForStansEllerOpphør.Alder -> HjemmelForStansEllerOpphørDTO.Alder
        HjemmelForStansEllerOpphør.DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForStansEllerOpphørDTO.DeltarIkkePåArbeidsmarkedstiltak
        HjemmelForStansEllerOpphør.Institusjonsopphold -> HjemmelForStansEllerOpphørDTO.Institusjonsopphold
        HjemmelForStansEllerOpphør.Introduksjonsprogrammet -> HjemmelForStansEllerOpphørDTO.Introduksjonsprogrammet
        HjemmelForStansEllerOpphør.Kvalifiseringsprogrammet -> HjemmelForStansEllerOpphørDTO.Kvalifiseringsprogrammet
        HjemmelForStansEllerOpphør.Livsoppholdytelser -> HjemmelForStansEllerOpphørDTO.Livsoppholdytelser
        HjemmelForStansEllerOpphør.LønnFraAndre -> HjemmelForStansEllerOpphørDTO.LønnFraAndre
        HjemmelForStansEllerOpphør.LønnFraTiltaksarrangør -> HjemmelForStansEllerOpphørDTO.LønnFraTiltaksarrangør
    }
}

fun NonEmptySet<HjemmelForStansEllerOpphør>.tilHjemmelForStansEllerOpphørDTO(): List<HjemmelForStansEllerOpphørDTO> {
    return this.map { it.tilDTO() }.sortedBy { it.name }
}
