package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import java.time.LocalDateTime

sealed interface IBehandling {
    val id: BehandlingId
    val status: Behandlingsstatus

    val opprettet: LocalDateTime
    val sistEndret: LocalDateTime
    val iverksattTidspunkt: LocalDateTime?
    val sendtTilDatadeling: LocalDateTime?

    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr

    val saksopplysninger: Saksopplysninger
    val saksopplysningsperiode: Periode

    val saksbehandler: String?
    val beslutter: String?
    val sendtTilBeslutning: LocalDateTime?
    val attesteringer: List<Attestering>
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
    val avbrutt: Avbrutt?
    val utfall: Any?
    val virkningsperiode: Periode?
}

sealed class BehandlingNy : IBehandling {
    val erUnderBehandling: Boolean = status == UNDER_BEHANDLING
    val erAvbrutt: Boolean = status == AVBRUTT
    val erVedtatt: Boolean = status == VEDTATT
    val erAvsluttet: Boolean = erAvbrutt || erVedtatt

    fun inneholderEksternDeltagelseId(eksternDeltagelseId: String): Boolean =
        saksopplysninger.tiltaksdeltagelse.find { it.eksternDeltagelseId == eksternDeltagelseId } != null

    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? =
        saksopplysninger.getTiltaksdeltagelse(eksternDeltagelseId)

    init {
        if (beslutter != null && saksbehandler != null) {
            require(beslutter != saksbehandler) { "Saksbehandler og beslutter kan ikke være samme person" }
        }

        when (status) {
            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Behandlingen kan ikke være tilknyttet en saksbehandler når statusen er KLAR_TIL_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
                require(beslutter == null)
            }

            UNDER_BEHANDLING -> {
                requireNotNull(saksbehandler) {
                    "Behandlingen må være tilknyttet en saksbehandler når status er UNDER_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
                if (attesteringer.isEmpty()) {
                    require(beslutter == null) { "Beslutter kan ikke være tilknyttet behandlingen dersom det ikke er gjort noen attesteringer" }
                } else {
                    require(utfall != null) { "Behandlingsutfall må være satt dersom det er gjort attesteringer på behandlingen" }
                }
            }

            KLAR_TIL_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er KLAR_TIL_BESLUTNING" }
                require(beslutter == null) {
                    "Behandlingen kan ikke være tilknyttet en beslutter når status er KLAR_TIL_BESLUTNING"
                }
                require(iverksattTidspunkt == null)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen KLAR_TIL_BESLUTNING" }
                require(this.utfall != null) { "Behandlingsutfall må være satt for statusen KLAR_TIL_BESLUTNING" }
            }

            UNDER_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er UNDER_BESLUTNING" }
                requireNotNull(beslutter) { "Behandlingen må tilknyttet en beslutter når status er UNDER_BESLUTNING" }
                require(iverksattTidspunkt == null)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen UNDER_BESLUTNING" }
                require(this.utfall != null) { "Behandlingsutfall må være satt for statusen UNDER_BESLUTNING" }
            }

            VEDTATT -> {
                // Det er viktig at vi ikke tar saksbehandler og beslutter av behandlingen når status er VEDTATT.
                requireNotNull(beslutter) { "Behandlingen må ha beslutter når status er VEDTATT" }
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er VEDTATT" }
                requireNotNull(iverksattTidspunkt)
                requireNotNull(sendtTilBeslutning)
                require(virkningsperiode != null) { "Virkningsperiode må være satt for statusen VEDTATT" }
                require(this.utfall != null) { "Behandlingsutfall må være satt for statusen VEDTATT" }
            }

            AVBRUTT -> {
                requireNotNull(avbrutt)
            }
        }
    }
}
