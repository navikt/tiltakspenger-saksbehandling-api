package no.nav.tiltakspenger.saksbehandling.tilbakekreving.service

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling.leggTilbake
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling.overta
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tildeling.taBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingRepo
import java.time.Clock

class TilbakekrevingBehandlingTildelingService(
    private val sakService: SakService,
    private val tilbakekrevingBehandlingRepo: TilbakekrevingBehandlingRepo,
    private val clock: Clock,
) {

    fun taBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        val (sak, behandling) = hentSakOgBehandling(sakId, tilbakekrevingId)
        val oppdatert = behandling.taBehandling(saksbehandler, clock)

        val harOppdatert = when (oppdatert.statusIntern) {
            TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING ->
                tilbakekrevingBehandlingRepo.taBehandlingSaksbehandler(oppdatert)

            TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING ->
                tilbakekrevingBehandlingRepo.taBehandlingBeslutter(oppdatert)

            else -> throw IllegalStateException("Uventet status ${oppdatert.statusIntern} etter ta behandling for $tilbakekrevingId")
        }
        require(harOppdatert) {
            "Oppdatering av tilbakekrevingbehandling feilet for $tilbakekrevingId. En annen bruker kan ha endret behandlingen."
        }

        return oppdatertSak(sak, oppdatert)
    }

    fun overtaBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        val (sak, behandling) = hentSakOgBehandling(sakId, tilbakekrevingId)
        val oppdatert = behandling.overta(saksbehandler, clock)

        val harOppdatert = when (behandling.statusIntern) {
            TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING ->
                tilbakekrevingBehandlingRepo.overtaSaksbehandler(oppdatert, behandling.saksbehandlerIdent!!)

            TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING ->
                tilbakekrevingBehandlingRepo.overtaBeslutter(oppdatert, behandling.beslutterIdent!!)

            else -> throw IllegalStateException("Uventet status ${behandling.statusIntern} ved overta for $tilbakekrevingId")
        }
        require(harOppdatert) {
            "Oppdatering av tilbakekrevingbehandling feilet for $tilbakekrevingId. En annen bruker kan ha endret behandlingen."
        }

        return oppdatertSak(sak, oppdatert)
    }

    fun leggTilbakeBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        val (sak, behandling) = hentSakOgBehandling(sakId, tilbakekrevingId)
        val oppdatert = behandling.leggTilbake(saksbehandler, clock)

        val harOppdatert = when (behandling.statusIntern) {
            TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING ->
                tilbakekrevingBehandlingRepo.leggTilbakeSaksbehandler(oppdatert, behandling.saksbehandlerIdent!!)

            TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING ->
                tilbakekrevingBehandlingRepo.leggTilbakeBeslutter(oppdatert, behandling.beslutterIdent!!)

            else -> throw IllegalStateException("Uventet status ${behandling.statusIntern} ved legg tilbake for $tilbakekrevingId")
        }
        require(harOppdatert) {
            "Oppdatering av tilbakekrevingbehandling feilet for $tilbakekrevingId. En annen bruker kan ha endret behandlingen."
        }

        return oppdatertSak(sak, oppdatert)
    }

    private fun hentSakOgBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
    ): Pair<Sak, TilbakekrevingBehandling> {
        val sak = sakService.hentForSakId(sakId)
        val behandling = sak.tilbakekrevinger.singleOrNull { it.id == tilbakekrevingId }
            ?: throw IllegalArgumentException("Fant ikke tilbakekrevingbehandling med id $tilbakekrevingId for sak $sakId")
        return sak to behandling
    }

    private fun oppdatertSak(sak: Sak, oppdatert: TilbakekrevingBehandling): Pair<Sak, TilbakekrevingBehandling> {
        val oppdatertSak = sak.copy(
            tilbakekrevinger = sak.tilbakekrevinger.map { if (it.id == oppdatert.id) oppdatert else it },
        )
        return oppdatertSak to oppdatert
    }
}
