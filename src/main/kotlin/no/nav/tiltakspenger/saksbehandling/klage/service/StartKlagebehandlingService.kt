package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.ValiderJournalpostResponse
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.StartKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class StartKlagebehandlingService(
    private val sakService: SakService,
    private val clock: java.time.Clock,
    private val validerJournalpostService: ValiderJournalpostService,
) {
    suspend fun startKlagebehandling(
        kommando: StartKlagebehandlingKommando,
    ): Either<Unit, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val journalpost: ValiderJournalpostResponse = validerJournalpostService.hentOgValiderJournalpost(
            fnr = sak.fnr,
            journalpostId = kommando.journalpostId,
        )
        require(journalpost.datoOpprettet != null) {
            "Journalpost ${kommando.journalpostId} mangler datoOpprettet. sakId=${kommando.sakId}"
        }
        val klagebehandling = Klagebehandling.create(
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            opprettet = nå(clock),
            journalpostOpprettet = journalpost.datoOpprettet,
            kommando = kommando,
        )
    }
}
