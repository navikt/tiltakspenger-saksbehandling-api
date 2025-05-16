package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode

sealed interface RevurderingUtfall {
    data class Stans(
        val virkningsperiode: Periode,
        val valgtHjemmelHarIkkeRettighet: List<ValgtHjemmelHarIkkeRettighet>,
    ) : RevurderingUtfall {

        val utfallsperioder: Periodisering<Utfallsperiode> =
            Periodisering(Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER, virkningsperiode)
    }
}
