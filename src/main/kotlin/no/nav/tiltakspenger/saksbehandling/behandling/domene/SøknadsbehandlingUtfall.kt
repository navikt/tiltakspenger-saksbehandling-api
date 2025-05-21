package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface SøknadsbehandlingUtfall : BehandlingUtfall {
    data class Innvilgelse(
        val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
        val barnetillegg: Barnetillegg?,
    ) : SøknadsbehandlingUtfall

    data class Avslag(
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : SøknadsbehandlingUtfall
}
