package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.KanIkkeOppretteBehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.OpprettbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage.opprettBehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OpprettRammebehandlingFraKlageService(
    private val sakService: SakService,
    private val behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    private val startRevurderingService: StartRevurderingService,
    private val opprettMeldekortbehandlingService: OpprettMeldekortbehandlingService,
) {
    suspend fun opprett(
        kommando: OpprettbehandlingFraKlageKommando,
    ): Either<KanIkkeOppretteBehandlingFraKlage, Pair<Sak, AttesterbarBehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.opprettBehandlingFraKlage(
            kommando = kommando,
            // Har ansvar for å lagre rammebehandling og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettSøknadsbehandling = behandleSøknadPåNyttService::startSøknadsbehandlingPåNytt,
            // Har ansvar for å lagre revurdering og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettRevurdering = startRevurderingService::startRevurdering,
            opprettMeldekortbehandling = { kommando, sak ->
//                val meldekortvedtak = sak.meldekortvedtaksliste.single { it.id == kommando.vedtakId }
//
//                opprettMeldekortbehandlingService.opprettBehandling(
//                    kjedeId = meldekortvedtak.kjedeId,
//                    sakId = sak.id,
//                    saksbehandler = kommando.saksbehandler,
//                )
                TODO()
            },
        )
    }
}
