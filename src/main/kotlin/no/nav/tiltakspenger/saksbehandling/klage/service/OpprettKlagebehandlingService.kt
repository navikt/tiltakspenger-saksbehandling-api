package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.ValiderJournalpostResponse
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OpprettKlagebehandlingService(
    private val sakService: SakService,
    private val clock: java.time.Clock,
    private val validerJournalpostService: ValiderJournalpostService,
    private val klageRepo: KlagebehandlingFakeRepo,
) {
    suspend fun opprettKlagebehandling(
        kommando: OpprettKlagebehandlingKommando,
    ): Either<Unit, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val journalpost: ValiderJournalpostResponse = validerJournalpostService.hentOgValiderJournalpost(
            fnr = sak.fnr,
            journalpostId = kommando.journalpostId,
        )
        require(journalpost.datoOpprettet != null) {
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
        klageRepo.lagreKlagebehandling(klagebehandling)
        return Pair(oppdatertSak, klagebehandling).right()
    }
}
