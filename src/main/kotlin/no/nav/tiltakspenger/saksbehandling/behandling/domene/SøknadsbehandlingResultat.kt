package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface SøknadsbehandlingResultat : BehandlingResultat {
    data class Avslag(
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : SøknadsbehandlingResultat

    data class Innvilgelse(
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser,
        override val barnetillegg: Barnetillegg?,
        override val antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode>?,
    ) : BehandlingResultat.Innvilgelse,
        SøknadsbehandlingResultat
}
