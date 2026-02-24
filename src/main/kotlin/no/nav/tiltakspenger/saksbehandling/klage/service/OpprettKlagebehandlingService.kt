package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister.STARTET_BEHANDLING_KLAGE
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.ValiderJournalpostResponse
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.KanIkkeOppretteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.opprett
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService

class OpprettKlagebehandlingService(
    private val sakService: SakService,
    private val clock: java.time.Clock,
    private val validerJournalpostService: ValiderJournalpostService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
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
        val statistikk = statistikkSakService.genererSaksstatistikkForOpprettetKlagebehandling(klagebehandling)

        sessionFactory.withTransactionContext { tx ->
            klagebehandlingRepo.lagreKlagebehandling(klagebehandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)

            // TODO: Å gjøre om withTransactionContext til suspend function er målet, men krever noen dagers arbeid
            @Suppress("RunBlockingInSuspendFunction")
            runBlocking {
                tx.onSuccess {
                    STARTET_BEHANDLING_KLAGE.inc()
                }
            }
        }

        return Pair(oppdatertSak, klagebehandling).right()
    }
}
