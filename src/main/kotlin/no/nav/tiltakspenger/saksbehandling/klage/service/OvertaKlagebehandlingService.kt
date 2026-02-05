package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.KanIkkeOvertaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.OvertaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.overtaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OvertaKlagebehandlingService(
    private val sakService: SakService,
    private val overtaRammebehandlingService: OvertaRammebehandlingService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
) {
    suspend fun overta(
        kommando: OvertaKlagebehandlingKommando,
    ): Either<KanIkkeOvertaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.overtaKlagebehandling(
            kommando = kommando,
            clock = clock,
            overtaRammebehandling = overtaRammebehandlingService::overta,
            lagreKlagebehandling = klagebehandlingRepo::lagreKlagebehandling,
        )
    }
}
