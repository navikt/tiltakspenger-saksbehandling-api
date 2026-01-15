package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandlingBrevtekst
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OppdaterKlagebehandlingTekstTilBrevService(
    private val sakService: SakService,
    private val clock: Clock,
    private val klageRepo: KlagebehandlingRepo,
) {
    suspend fun oppdaterTekstTilBrev(
        kommando: KlagebehandlingBrevKommando,
    ): Either<KanIkkeOppdatereKlagebehandling, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)

        return sak.oppdaterKlagebehandlingBrevtekst(kommando, clock).onRight {
            klageRepo.lagreKlagebehandling(it.second)
        }
    }
}
