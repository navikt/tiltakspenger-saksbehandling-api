package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.KanIkkeLeggeTilbakeKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.LeggTilbakeKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.leggTilbakeKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class LeggTilbakeKlagebehandlingService(
    private val sakService: SakService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val leggTilbakeRammebehandlingService: LeggTilbakeRammebehandlingService,
    private val clock: Clock,
) {
    suspend fun leggTilbake(
        kommando: LeggTilbakeKlagebehandlingKommando,
    ): Either<KanIkkeLeggeTilbakeKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.leggTilbakeKlagebehandling(
            kommando = kommando,
            clock = clock,
            leggTilbakeRammebehandling = leggTilbakeRammebehandlingService::leggTilbakeBehandling,
            lagreKlagebehandling = klagebehandlingRepo::lagreKlagebehandling,
        )
    }
}
