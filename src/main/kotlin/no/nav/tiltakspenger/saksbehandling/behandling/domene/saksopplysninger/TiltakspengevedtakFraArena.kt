package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import arrow.core.Nel
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import java.time.LocalDateTime

sealed interface TiltakspengevedtakFraArena : List<ArenaTPVedtak> {
    val oppslagsperiode: Periode?
    val oppslagstidspunkt: LocalDateTime?
    val value: List<ArenaTPVedtak>

    data class Treff(
        override val value: Nel<ArenaTPVedtak>,
        override val oppslagsperiode: Periode,
        override val oppslagstidspunkt: LocalDateTime,
    ) : TiltakspengevedtakFraArena,
        List<ArenaTPVedtak> by value

    data class IngenTreff(
        override val oppslagsperiode: Periode,
        override val oppslagstidspunkt: LocalDateTime,
    ) : TiltakspengevedtakFraArena,
        List<ArenaTPVedtak> by emptyList() {
        override val value = emptyList<ArenaTPVedtak>()
    }

    object IkkeBehandlingsgrunnlag : TiltakspengevedtakFraArena, List<ArenaTPVedtak> by emptyList() {
        override val oppslagsperiode = null
        override val value = emptyList<ArenaTPVedtak>()
        override val oppslagstidspunkt = null
    }

    /** Denne featuren ble ikke implementert før september 2025 og behandlinger før dette har ikke gjort oppslaget. */
    object BehandletFørFeature : TiltakspengevedtakFraArena, List<ArenaTPVedtak> by emptyList() {
        override val oppslagsperiode = null
        override val value = emptyList<ArenaTPVedtak>()
        override val oppslagstidspunkt = null
    }

    companion object {
        fun fromList(
            arenaTpVedtak: List<ArenaTPVedtak>,
            oppslagsperiode: Periode,
            oppslagstidspunkt: LocalDateTime,
        ): TiltakspengevedtakFraArena {
            return arenaTpVedtak.toNonEmptyListOrNull()?.let { Treff(it, oppslagsperiode, oppslagstidspunkt) }
                ?: IngenTreff(
                    oppslagsperiode,
                    oppslagstidspunkt,
                )
        }
    }
}
