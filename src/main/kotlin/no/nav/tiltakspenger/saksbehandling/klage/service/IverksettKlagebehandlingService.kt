package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.KanIkkeIverksetteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksettKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class IverksettKlagebehandlingService(
    private val sakService: SakService,
    private val clock: Clock,
    private val klageRepo: KlagebehandlingRepo,
) {
    suspend fun iverksett(
        kommando: IverksettKlagebehandlingKommando,
    ): Either<KanIkkeIverksetteKlagebehandling, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.iverksettKlagebehandling(kommando, clock).onRight {
            klageRepo.lagreKlagebehandling(it.second)
        }
    }
}
