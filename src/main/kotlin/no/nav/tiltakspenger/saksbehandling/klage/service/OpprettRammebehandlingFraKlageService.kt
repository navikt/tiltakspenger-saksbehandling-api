package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.KanIkkeOppretteRammebehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettRammebehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OpprettRammebehandlingFraKlageService(
    private val sakService: SakService,
    private val clock: Clock,
    private val behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    private val startRevurderingService: StartRevurderingService,
) {
    suspend fun opprett(
        kommando: OpprettRammebehandlingFraKlageKommando,
    ): Either<KanIkkeOppretteRammebehandlingFraKlage, Triple<Sak, Klagebehandling, Rammebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.opprettRammebehandlingFraKlage(
            kommando = kommando,
            clock = clock,
            // Har ansvar for å lagre rammebehandling + sideeffekter som statistikk og metrikker.
            opprettSøknadsbehandling = behandleSøknadPåNyttService::startSøknadsbehandlingPåNytt,
            // Har ansvar for å lagre revurdering + sideeffekter som statistikk og metrikker.
            opprettRevurdering = startRevurderingService::startRevurdering,
        ).right()
    }
}
