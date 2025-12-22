package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans

enum class ValgtHjemmelForStansDTO {
    DeltarIkkePåArbeidsmarkedstiltak,
    Alder,
    Livsoppholdytelser,
    Kvalifiseringsprogrammet,
    Introduksjonsprogrammet,
    LønnFraTiltaksarrangør,
    LønnFraAndre,
    Institusjonsopphold,
    ;

    fun toDomain(): ValgtHjemmelForStans = when (this) {
        DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak
        Alder -> ValgtHjemmelForStans.Alder
        Livsoppholdytelser -> ValgtHjemmelForStans.Livsoppholdytelser
        Kvalifiseringsprogrammet -> ValgtHjemmelForStans.Kvalifiseringsprogrammet
        Introduksjonsprogrammet -> ValgtHjemmelForStans.Introduksjonsprogrammet
        LønnFraTiltaksarrangør -> ValgtHjemmelForStans.LønnFraTiltaksarrangør
        LønnFraAndre -> ValgtHjemmelForStans.LønnFraAndre
        Institusjonsopphold -> ValgtHjemmelForStans.Institusjonsopphold
    }
}

fun List<ValgtHjemmelForStansDTO>.toDomain(): List<ValgtHjemmelForStans> = this.map { it.toDomain() }

fun NonEmptySet<ValgtHjemmelForStans>.tilValgtHjemmelForStansDTO(): List<ValgtHjemmelForStansDTO> {
    return this.map {
        when (it) {
            ValgtHjemmelForStans.Alder -> ValgtHjemmelForStansDTO.Alder
            ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelForStansDTO.DeltarIkkePåArbeidsmarkedstiltak
            ValgtHjemmelForStans.Institusjonsopphold -> ValgtHjemmelForStansDTO.Institusjonsopphold
            ValgtHjemmelForStans.Introduksjonsprogrammet -> ValgtHjemmelForStansDTO.Introduksjonsprogrammet
            ValgtHjemmelForStans.Kvalifiseringsprogrammet -> ValgtHjemmelForStansDTO.Kvalifiseringsprogrammet
            ValgtHjemmelForStans.Livsoppholdytelser -> ValgtHjemmelForStansDTO.Livsoppholdytelser
            ValgtHjemmelForStans.LønnFraAndre -> ValgtHjemmelForStansDTO.LønnFraAndre
            ValgtHjemmelForStans.LønnFraTiltaksarrangør -> ValgtHjemmelForStansDTO.LønnFraTiltaksarrangør
        }
    }.sortedBy { it.name }
}
