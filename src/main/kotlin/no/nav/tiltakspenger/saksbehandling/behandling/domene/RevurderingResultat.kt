package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface RevurderingResultat : BehandlingResultat {
    data class Stans(
        val valgtHjemmel: List<ValgtHjemmelForStans>,
    ) : RevurderingResultat

    data class Innvilgelse(
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
        override val barnetillegg: Barnetillegg?,
        override val antallDagerPerMeldeperiode: Int?,
    ) : BehandlingResultat.Innvilgelse,
        RevurderingResultat
}
