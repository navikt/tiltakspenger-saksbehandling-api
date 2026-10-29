package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.ForvaltningsmeldingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.KanIkkeSendeForvaltningsmelding
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.sendForvaltningsmeldingForKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class SendForvaltningsmeldingKlagebehandlingService(
    private val sakService: SakService,
    private val clock: Clock,
    private val klagebehandlingRepo: KlagebehandlingRepo,
) {
    fun sendForvaltningsmelding(
        kommando: ForvaltningsmeldingKommando,
    ): Either<KanIkkeSendeForvaltningsmelding, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)

        return sak.sendForvaltningsmeldingForKlagebehandling(kommando, clock).onRight {
            klagebehandlingRepo.lagreKlagebehandling(it.second)
        }
    }
}
