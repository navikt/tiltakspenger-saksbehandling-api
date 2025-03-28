package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.ValgtHjemmelForAvslag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.ValgtHjemmelType

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
            ValgtHjemmelForAvslag.DeltarIkkePåArbeidsmarkedstiltak -> "DeltarIkkePåArbeidsmarkedstiltak"
            ValgtHjemmelForAvslag.Alder -> "Alder"
            ValgtHjemmelForAvslag.Livsoppholdytelser -> "Livsoppholdytelser"
            ValgtHjemmelForAvslag.Kvalifiseringsprogrammet -> "Kvalifiseringsprogrammet"
            ValgtHjemmelForAvslag.Introduksjonsprogrammet -> "Introduksjonsprogrammet"
            ValgtHjemmelForAvslag.LønnFraTiltaksarrangør -> "LønnFraTiltaksarrangør"
            ValgtHjemmelForAvslag.LønnFraAndre -> "LønnFraAndre"
            ValgtHjemmelForAvslag.Institusjonsopphold -> "Institusjonsopphold"
            else -> throw IllegalArgumentException("Ukjent kode for ValgtHjemmelForAvslag: $this")
        }
    }
}
