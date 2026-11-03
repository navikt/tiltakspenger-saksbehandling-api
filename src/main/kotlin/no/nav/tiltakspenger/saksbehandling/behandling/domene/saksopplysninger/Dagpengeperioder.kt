package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import arrow.core.Nel
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import java.time.LocalDateTime

/**
 * Periodene bruker har rett til dagpenger, hentet fra dp-datadeling for samme periode som tiltakspengevedtakene fra Arena.
 */
sealed interface Dagpengeperioder : List<Dagpengeperiode> {
    val oppslagsperiode: Periode?
    val oppslagstidspunkt: LocalDateTime?
    val value: List<Dagpengeperiode>

    data class Treff(
        override val value: Nel<Dagpengeperiode>,
        override val oppslagsperiode: Periode,
        override val oppslagstidspunkt: LocalDateTime,
    ) : Dagpengeperioder,
        List<Dagpengeperiode> by value

    data class IngenTreff(
        override val oppslagsperiode: Periode,
        override val oppslagstidspunkt: LocalDateTime,
    ) : Dagpengeperioder,
        List<Dagpengeperiode> by emptyList() {
        override val value = emptyList<Dagpengeperiode>()
    }

    object IkkeBehandlingsgrunnlag : Dagpengeperioder, List<Dagpengeperiode> by emptyList() {
        override val oppslagsperiode = null
        override val value = emptyList<Dagpengeperiode>()
        override val oppslagstidspunkt = null
    }

    /** Oppslaget mot dp-datadeling ble lagt til i 2026, og behandlinger før dette har ikke gjort oppslaget. */
    object BehandletFørFeature : Dagpengeperioder, List<Dagpengeperiode> by emptyList() {
        override val oppslagsperiode = null
        override val value = emptyList<Dagpengeperiode>()
        override val oppslagstidspunkt = null
    }

    companion object {
        fun fromList(
            dagpengeperioder: List<Dagpengeperiode>,
            oppslagsperiode: Periode,
            oppslagstidspunkt: LocalDateTime,
        ): Dagpengeperioder {
            return dagpengeperioder.toNonEmptyListOrNull()?.let { Treff(it, oppslagsperiode, oppslagstidspunkt) }
                ?: IngenTreff(oppslagsperiode, oppslagstidspunkt)
        }
    }
}
