package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import arrow.core.Nel
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse

sealed interface Ytelser {
    val oppslagsperiode: Periode?

    /** Bruker har en eller flere treff innenfor oppslagsperioden. */
    data class Treff(
        val value: Nel<Ytelse>,
        override val oppslagsperiode: Periode,
    ) : Ytelser,
        List<Ytelse> by value

    /** Bruker har ingen treff innenfor oppslagsperioden */
    data class IngenTreff(override val oppslagsperiode: Periode) : Ytelser

    /** Vi hadde ikke behandlingsgrunnlag for å slå opp ytelser. Typisk at vi ikke har en tiltaksdeltagelsesperiode enda. */
    object IkkeBehandlingsgrunnlag : Ytelser {
        override val oppslagsperiode = null
    }

    /** Denne featuren ble ikke implementert før medio juni 2025 og behandlinger før dette har ikke gjort oppslaget. */
    object BehandletFørFeature : Ytelser {
        override val oppslagsperiode = null
    }

    companion object {
        fun fromList(ytelser: List<Ytelse>, oppslagsperiode: Periode): Ytelser {
            return ytelser.toNonEmptyListOrNull()?.let { Ytelser.Treff(it, oppslagsperiode) } ?: Ytelser.IngenTreff(
                oppslagsperiode,
            )
        }
    }
}
