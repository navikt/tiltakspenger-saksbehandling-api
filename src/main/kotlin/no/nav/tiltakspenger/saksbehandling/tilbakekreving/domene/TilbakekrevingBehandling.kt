package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 *  [tilbakeBehandlingId] BehandlingId i ekstern saksbehandlingsløsning for tilbakekreving
 *  [status] Status på behandlingen fra tilbakeløsningen
 *  [statusIntern] Vår interne status, som tar hensyn til om behandlingen er tildelt saksbehandler eller beslutter
 *  [url] URL til behandlingen i ekstern saksbehandlingsløsning for tilbakekreving
 *  [saksbehandler] Saksbehandler som har tatt behandlingen i vårt system. Håndheves ikke mot tilbakeløsningen, er kun ment for å hjelpe saksbehandlere med å fordele oppgaver
 *  [beslutter] Beslutter som har tatt behandlingen i vårt system. Håndheves ikke mot tilbakeløsningen, er kun ment for å hjelpe saksbehandlere med å fordele oppgaver
 * */
data class TilbakekrevingBehandling(
    val id: TilbakekrevingId,
    val sakId: SakId,
    val utbetalingIder: NonEmptyList<UtbetalingId>,
    val tilbakeBehandlingId: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val status: TilbakekrevingBehandlingsstatus,
    val url: String,
    val kravgrunnlagTotalPeriode: Periode,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val varselSendt: LocalDate?,
    val saksbehandler: String?,
    val beslutter: String?,
) {

    val statusIntern: TilbakekrevingBehandlingsstatusIntern by lazy {
        when (status) {
            TilbakekrevingBehandlingsstatus.OPPRETTET -> TilbakekrevingBehandlingsstatusIntern.OPPRETTET

            TilbakekrevingBehandlingsstatus.TIL_BEHANDLING ->
                if (saksbehandler != null) {
                    TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING
                } else {
                    TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING
                }

            TilbakekrevingBehandlingsstatus.TIL_GODKJENNING ->
                if (beslutter != null) {
                    TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING
                } else {
                    TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING
                }

            TilbakekrevingBehandlingsstatus.AVSLUTTET -> TilbakekrevingBehandlingsstatusIntern.AVSLUTTET
        }
    }

    fun gyldigeKommandoer(saksbehandler: Saksbehandler): List<SaksbehandlerBehandlingKommando> {
        val erSaksbehandler = saksbehandler.erSaksbehandler()
        val erBeslutter = saksbehandler.erBeslutter()
        val navIdent = saksbehandler.navIdent

        val tildeltSaksbehandler = this.saksbehandler
        val tildeltBeslutter = this.beslutter

        return buildList {
            when (statusIntern) {
                TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING -> {
                    if (erSaksbehandler) {
                        add(SaksbehandlerBehandlingKommando.TildelSaksbehandler)
                    }
                }

                TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING -> {
                    if (tildeltSaksbehandler == navIdent) {
                        add(SaksbehandlerBehandlingKommando.LeggTilbakeSaksbehandler)
                    } else if (erSaksbehandler) {
                        add(SaksbehandlerBehandlingKommando.OvertaSaksbehandler)
                    }
                }

                TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING -> {
                    if (erBeslutter && navIdent != tildeltSaksbehandler) {
                        add(SaksbehandlerBehandlingKommando.TildelBeslutter)
                    }
                }

                TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING -> {
                    if (tildeltBeslutter == navIdent) {
                        add(SaksbehandlerBehandlingKommando.LeggTilbakeBeslutter)
                    } else if (erBeslutter && navIdent != tildeltSaksbehandler) {
                        add(SaksbehandlerBehandlingKommando.OvertaBeslutter)
                    }
                }

                TilbakekrevingBehandlingsstatusIntern.OPPRETTET,
                TilbakekrevingBehandlingsstatusIntern.AVSLUTTET,
                -> {
                    /* Ingen gyldige kommandoer */
                }
            }
        }
    }

    init {
        if (saksbehandler != null && beslutter != null) {
            require(saksbehandler != beslutter) {
                "Saksbehandler og beslutter kan ikke være samme person. tilbakekrevingId: $id, sakId: $sakId"
            }
        }
    }

    companion object {

        private const val RETTSGEBYR_2026: Long = 1345

        const val MINSTEBELØP_FOR_TILBAKEKREVING: Long = RETTSGEBYR_2026 * 4
    }
}
