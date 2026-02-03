package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.ValiderJournalpostResponse
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereFormkravPåKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.OppdaterKlagebehandlingFormkravKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandlingFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OppdaterKlagebehandlingFormkravService(
    private val sakService: SakService,
    private val clock: java.time.Clock,
    private val validerJournalpostService: ValiderJournalpostService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
) {
    suspend fun oppdaterFormkrav(
        kommando: OppdaterKlagebehandlingFormkravKommando,
    ): Either<KanIkkeOppdatereFormkravPåKlagebehandling, Pair<Sak, Klagebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val journalpost: ValiderJournalpostResponse =
            validerJournalpostService.hentOgValiderJournalpost(
                fnr = sak.fnr,
                journalpostId = kommando.journalpostId,
            )
        if (!journalpost.journalpostFinnes) return KanIkkeOppdatereFormkravPåKlagebehandling.FantIkkeJournalpost.left()
        require(journalpost.datoOpprettet != null) {
            // Dette er ikke en forventet feil - så vi lager ikke en left for den.
            "Journalpost ${kommando.journalpostId} mangler datoOpprettet. sakId=${kommando.sakId}"
        }
        return sak.oppdaterKlagebehandlingFormkrav(kommando, journalpost.datoOpprettet, clock).onRight {
            klagebehandlingRepo.lagreKlagebehandling(it.second)
        }
    }
}
