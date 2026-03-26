package no.nav.tiltakspenger.saksbehandling.tilbakekreving.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
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
    val logger = KotlinLogging.logger { }

    fun taBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        return oppdaterBehandling(sakId, tilbakekrevingId) { it.taBehandling(saksbehandler, clock) }
    }

    fun overtaBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        return oppdaterBehandling(sakId, tilbakekrevingId) { it.overta(saksbehandler, clock) }
    }

    fun leggTilbakeBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, TilbakekrevingBehandling> {
        return oppdaterBehandling(sakId, tilbakekrevingId) { it.leggTilbake(saksbehandler, clock) }
    }

    private fun oppdaterBehandling(
        sakId: SakId,
        tilbakekrevingId: TilbakekrevingId,
        oppdatering: (TilbakekrevingBehandling) -> TilbakekrevingBehandling,
    ): Pair<Sak, TilbakekrevingBehandling> {
        val sak = sakService.hentForSakId(sakId)

        val tilbakekrevingBehandling = sak.tilbakekrevinger.singleOrNull { it.id == tilbakekrevingId }
            ?: throw IllegalArgumentException("Fant ikke tilbakekrevingbehandling med id $tilbakekrevingId for sak $sakId")

        val oppdatert = oppdatering(tilbakekrevingBehandling)

        tilbakekrevingBehandlingRepo.taBehandling(oppdatert)

        val oppdatertSak = sak.copy(
            tilbakekrevinger = sak.tilbakekrevinger.map { if (it.id == oppdatert.id) oppdatert else it },
        )

        return oppdatertSak to oppdatert
    }
}
