package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class AvbrytKlagebehandlingService(
    private val sakService: SakService,
    private val clock: java.time.Clock,
    private val klageRepo: KlagebehandlingRepo,
) {
    suspend fun avbrytKlagebehandling(
        kommando: AvbrytKlagebehandlingKommando,
    ): Either<KanIkkeAvbryteKlagebehandling, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.avbrytKlagebehandling(
            kommando = kommando,
            clock = clock,
        ).onRight {
            klageRepo.lagreKlagebehandling(it.second)
        }
    }
}
