package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.ValiderJournalpostResponse
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.KanIkkeOppretteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OpprettKlagebehandlingService(
    private val sakService: SakService,
    private val clock: java.time.Clock,
    private val validerJournalpostService: ValiderJournalpostService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
) {
    suspend fun opprettKlagebehandling(
        kommando: OpprettKlagebehandlingKommando,
    ): Either<KanIkkeOppretteKlagebehandling, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val journalpost: ValiderJournalpostResponse = validerJournalpostService.hentOgValiderJournalpost(
            fnr = sak.fnr,
            journalpostId = kommando.journalpostId,
        )
        if (!journalpost.journalpostFinnes) return KanIkkeOppretteKlagebehandling.FantIkkeJournalpost.left()
        require(journalpost.datoOpprettet != null) {
            // Dette er ikke en forventet feil - så vi lager ikke en left for den.
            "Journalpost ${kommando.journalpostId} mangler datoOpprettet. sakId=${kommando.sakId}"
        }
        val klagebehandling = Klagebehandling.opprett(
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            opprettet = nå(clock),
            journalpostOpprettet = journalpost.datoOpprettet,
            kommando = kommando,
        )
        val oppdatertSak = sak.leggTilKlagebehandling(klagebehandling)
        klagebehandlingRepo.lagreKlagebehandling(klagebehandling)
        return Pair(oppdatertSak, klagebehandling).right()
    }
}
