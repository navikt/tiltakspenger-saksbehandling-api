package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

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
