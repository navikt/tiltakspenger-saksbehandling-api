package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrNull
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag

enum class ValgtHjemmelForAvslagDTO {
    DeltarIkkePåArbeidsmarkedstiltak,
    Alder,
    Livsoppholdytelser,
    Kvalifiseringsprogrammet,
    Introduksjonsprogrammet,
    LønnFraTiltaksarrangør,
    LønnFraAndre,
    Institusjonsopphold,
    FremmetForSent,
    ;

    fun toDomain(): Avslagsgrunnlag = when (this) {
        DeltarIkkePåArbeidsmarkedstiltak -> Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak
        Alder -> Avslagsgrunnlag.Alder
        Livsoppholdytelser -> Avslagsgrunnlag.Livsoppholdytelser
        Kvalifiseringsprogrammet -> Avslagsgrunnlag.Kvalifiseringsprogrammet
        Introduksjonsprogrammet -> Avslagsgrunnlag.Introduksjonsprogrammet
        LønnFraTiltaksarrangør -> Avslagsgrunnlag.LønnFraTiltaksarrangør
        LønnFraAndre -> Avslagsgrunnlag.LønnFraAndre
        Institusjonsopphold -> Avslagsgrunnlag.Institusjonsopphold
        FremmetForSent -> Avslagsgrunnlag.FremmetForSent
    }
}

fun List<ValgtHjemmelForAvslagDTO>.toAvslagsgrunnlag(): NonEmptySet<Avslagsgrunnlag> =
    this.map { it.toDomain() }.toNonEmptySetOrNull()!!

fun Set<Avslagsgrunnlag>.toValgtHjemmelForAvslagDTO(): List<ValgtHjemmelForAvslagDTO> =
    this.map { avslagsgrunnlag ->
        when (avslagsgrunnlag) {
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak -> ValgtHjemmelForAvslagDTO.DeltarIkkePåArbeidsmarkedstiltak
            Avslagsgrunnlag.Alder -> ValgtHjemmelForAvslagDTO.Alder
            Avslagsgrunnlag.Livsoppholdytelser -> ValgtHjemmelForAvslagDTO.Livsoppholdytelser
            Avslagsgrunnlag.Kvalifiseringsprogrammet -> ValgtHjemmelForAvslagDTO.Kvalifiseringsprogrammet
            Avslagsgrunnlag.Introduksjonsprogrammet -> ValgtHjemmelForAvslagDTO.Introduksjonsprogrammet
            Avslagsgrunnlag.LønnFraTiltaksarrangør -> ValgtHjemmelForAvslagDTO.LønnFraTiltaksarrangør
            Avslagsgrunnlag.LønnFraAndre -> ValgtHjemmelForAvslagDTO.LønnFraAndre
            Avslagsgrunnlag.Institusjonsopphold -> ValgtHjemmelForAvslagDTO.Institusjonsopphold
            Avslagsgrunnlag.FremmetForSent -> ValgtHjemmelForAvslagDTO.FremmetForSent
        }
    }
