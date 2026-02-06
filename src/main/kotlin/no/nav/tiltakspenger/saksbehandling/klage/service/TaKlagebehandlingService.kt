package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.KanIkkeTaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.TaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.taKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class TaKlagebehandlingService(
    private val sakService: SakService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val taRammebehandlingService: TaRammebehandlingService,
    private val clock: Clock,
) {
    suspend fun ta(
        kommando: TaKlagebehandlingKommando,
    ): Either<KanIkkeTaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.taKlagebehandling(
            kommando = kommando,
            sistEndret = nå(clock),
            taRammebehandling = taRammebehandlingService::taBehandling,
            lagreKlagebehandling = klagebehandlingRepo::lagreKlagebehandling,
        )
    }
}
