package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.KanIkkeOpprettholdeKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OpprettholdKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.opprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OpprettholdKlagebehandlingService(
    private val sakService: SakService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
) {
    suspend fun oppretthold(
        kommando: OpprettholdKlagebehandlingKommando,
    ): Either<KanIkkeOpprettholdeKlagebehandling, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        return sak.opprettholdKlagebehandling(kommando).onRight {
            klagebehandlingRepo.lagreKlagebehandling(it.second)
        }
    }
}
