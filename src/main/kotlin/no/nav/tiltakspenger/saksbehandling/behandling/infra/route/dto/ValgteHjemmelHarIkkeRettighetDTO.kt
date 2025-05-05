package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelType

fun List<ValgtHjemmelHarIkkeRettighet>.toDTO(type: Behandlingstype): List<String> {
    val valgtHjemmelType = when (type) {
        Behandlingstype.FØRSTEGANGSBEHANDLING -> ValgtHjemmelType.AVSLAG
        Behandlingstype.REVURDERING -> ValgtHjemmelType.STANS
    }

    return this.map { it.toDTO(valgtHjemmelType) }
}

private fun ValgtHjemmelHarIkkeRettighet.toDTO(
    type: ValgtHjemmelType,
): String {
    return when (type) {
        ValgtHjemmelType.STANS -> when (this) {
            ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak -> "DeltarIkkePåArbeidsmarkedstiltak"
            ValgtHjemmelForStans.Alder -> "Alder"
            ValgtHjemmelForStans.Livsoppholdytelser -> "Livsoppholdytelser"
            ValgtHjemmelForStans.Kvalifiseringsprogrammet -> "Kvalifiseringsprogrammet"
            ValgtHjemmelForStans.Introduksjonsprogrammet -> "Introduksjonsprogrammet"
            ValgtHjemmelForStans.LønnFraTiltaksarrangør -> "LønnFraTiltaksarrangør"
            ValgtHjemmelForStans.LønnFraAndre -> "LønnFraAndre"
            ValgtHjemmelForStans.Institusjonsopphold -> "Institusjonsopphold"
            else -> throw IllegalArgumentException("Ukjent kode for ValgtHjemmelForStans: $this")
        }

        ValgtHjemmelType.AVSLAG -> when (this) {
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak -> "DeltarIkkePåArbeidsmarkedstiltak"
            Avslagsgrunnlag.Alder -> "Alder"
            Avslagsgrunnlag.Livsoppholdytelser -> "Livsoppholdytelser"
            Avslagsgrunnlag.Kvalifiseringsprogrammet -> "Kvalifiseringsprogrammet"
            Avslagsgrunnlag.Introduksjonsprogrammet -> "Introduksjonsprogrammet"
            Avslagsgrunnlag.LønnFraTiltaksarrangør -> "LønnFraTiltaksarrangør"
            Avslagsgrunnlag.LønnFraAndre -> "LønnFraAndre"
            Avslagsgrunnlag.Institusjonsopphold -> "Institusjonsopphold"
            else -> throw IllegalArgumentException("Ukjent kode for ValgtHjemmelForAvslag: $this")
        }
    }
}
