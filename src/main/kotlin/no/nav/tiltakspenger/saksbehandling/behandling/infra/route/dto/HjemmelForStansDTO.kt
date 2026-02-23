package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans

enum class HjemmelForStansDTO {
    DeltarIkkePåArbeidsmarkedstiltak,
    Alder,
    Livsoppholdytelser,
    Kvalifiseringsprogrammet,
    Introduksjonsprogrammet,
    LønnFraTiltaksarrangør,
    LønnFraAndre,
    Institusjonsopphold,
    IkkeLovligOpphold,
    ;

    fun toDomain(): HjemmelForStans = when (this) {
        DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak
        Alder -> HjemmelForStans.Alder
        Livsoppholdytelser -> HjemmelForStans.Livsoppholdytelser
        Kvalifiseringsprogrammet -> HjemmelForStans.Kvalifiseringsprogrammet
        Introduksjonsprogrammet -> HjemmelForStans.Introduksjonsprogrammet
        LønnFraTiltaksarrangør -> HjemmelForStans.LønnFraTiltaksarrangør
        LønnFraAndre -> HjemmelForStans.LønnFraAndre
        Institusjonsopphold -> HjemmelForStans.Institusjonsopphold
        IkkeLovligOpphold -> HjemmelForStans.IkkeLovligOpphold
    }
}

fun List<HjemmelForStansDTO>.toDomain(): List<HjemmelForStans> = this.map { it.toDomain() }

fun HjemmelForStans.tilDTO(): HjemmelForStansDTO {
    return when (this) {
        HjemmelForStans.Alder -> HjemmelForStansDTO.Alder
        HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> HjemmelForStansDTO.DeltarIkkePåArbeidsmarkedstiltak
        HjemmelForStans.Institusjonsopphold -> HjemmelForStansDTO.Institusjonsopphold
        HjemmelForStans.Introduksjonsprogrammet -> HjemmelForStansDTO.Introduksjonsprogrammet
        HjemmelForStans.Kvalifiseringsprogrammet -> HjemmelForStansDTO.Kvalifiseringsprogrammet
        HjemmelForStans.Livsoppholdytelser -> HjemmelForStansDTO.Livsoppholdytelser
        HjemmelForStans.LønnFraAndre -> HjemmelForStansDTO.LønnFraAndre
        HjemmelForStans.LønnFraTiltaksarrangør -> HjemmelForStansDTO.LønnFraTiltaksarrangør
        HjemmelForStans.IkkeLovligOpphold -> HjemmelForStansDTO.IkkeLovligOpphold
    }
}

fun NonEmptySet<HjemmelForStans>.tilHjemmelForStansDTO(): List<HjemmelForStansDTO> {
    return this.map { it.tilDTO() }.sortedBy { it.name }
}
