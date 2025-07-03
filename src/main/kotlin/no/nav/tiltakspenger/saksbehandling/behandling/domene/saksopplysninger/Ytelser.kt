package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import arrow.core.Nel
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import java.time.LocalDateTime

sealed interface Ytelser : List<Ytelse> {
    /** Perioden vi har bedt om data for. Merk at for Utbetaldata kan man ikke spørre om lengre enn dagens dato. Kun satt dersom vi har gjort et oppslag. */
    val oppslagsperiode: Periode?

    /** Kun satt dersom vi har gjort et oppslag. */
    val oppslagstidspunkt: LocalDateTime?

    /** Lagt på for bakoverkompabilitet */
    val value: List<Ytelse>

    /** Bruker har en eller flere treff innenfor oppslagsperioden. */
    data class Treff(
        override val value: Nel<Ytelse>,
        override val oppslagsperiode: Periode,
        override val oppslagstidspunkt: LocalDateTime,
    ) : Ytelser,
        List<Ytelse> by value

    /** Bruker har ingen treff innenfor oppslagsperioden */
    data class IngenTreff(
        override val oppslagsperiode: Periode,
        override val oppslagstidspunkt: LocalDateTime,
    ) : Ytelser,
        List<Ytelse> by emptyList() {
        override val value = emptyList<Ytelse>()
    }

    /** Vi hadde ikke behandlingsgrunnlag for å slå opp ytelser. Typisk at vi ikke har en tiltaksdeltagelsesperiode enda. */
    object IkkeBehandlingsgrunnlag : Ytelser, List<Ytelse> by emptyList() {
        override val oppslagsperiode = null
        override val value = emptyList<Ytelse>()
        override val oppslagstidspunkt = null
    }

    /** Denne featuren ble ikke implementert før medio juni 2025 og behandlinger før dette har ikke gjort oppslaget. */
    object BehandletFørFeature : Ytelser, List<Ytelse> by emptyList() {
        override val oppslagsperiode = null
        override val value = emptyList<Ytelse>()
        override val oppslagstidspunkt = null
    }

    companion object {
        fun fromList(
            ytelser: List<Ytelse>,
            oppslagsperiode: Periode,
            oppslagstidspunkt: LocalDateTime,
        ): Ytelser {
            return ytelser.toNonEmptyListOrNull()?.let { Treff(it, oppslagsperiode, oppslagstidspunkt) }
                ?: IngenTreff(
                    oppslagsperiode,
                    oppslagstidspunkt,
                )
        }
    }
}
