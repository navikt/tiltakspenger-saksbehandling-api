package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.KanIkkeOppretteRammebehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.OpprettRammebehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage.opprettRammebehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService

class OpprettRammebehandlingFraKlageService(
    private val sakService: SakService,
    private val behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    private val startRevurderingService: StartRevurderingService,
) {
    suspend fun opprett(
        kommando: OpprettRammebehandlingFraKlageKommando,
    ): Either<KanIkkeOppretteRammebehandlingFraKlage, Pair<Sak, Rammebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.opprettRammebehandlingFraKlage(
            kommando = kommando,
            // Har ansvar for å lagre rammebehandling og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettSøknadsbehandling = behandleSøknadPåNyttService::startSøknadsbehandlingPåNytt,
            // Har ansvar for å lagre revurdering og endringer på klagen + sideeffekter som statistikk og metrikker.
            opprettRevurdering = startRevurderingService::startRevurdering,
        )
    }
}
