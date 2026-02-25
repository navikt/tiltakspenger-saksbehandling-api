package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class IverksattOpprettholdelseJobberTest {

    @Test
    fun `jobbene kjører som forventet`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (_, klagebehandling, _) = opprettSakOgOpprettholdKlagebehandling(tac = tac, utførJobber = false)!!
            verifiserResultat(tac, klagebehandling.id)
            tac.klagebehandlingContext.klagebehandlingRepo.hentInnstillingsbrevSomSkalJournalføres().size shouldBe 1
            tac.klagebehandlingContext.journalførKlagebrevJobb.journalførInnstillingsbrev()
            verifiserResultat(
                tac = tac,
                id = klagebehandling.id,
                forventetBrevdato = LocalDate.parse("2025-01-01"),
                forventetJournalpostId = JournalpostId("2"),
                forventetJournalføringstidspunkt = true,
            )
            // Påser at det ikke feiler og kjøre den samme jobben gang nr. 2:
            tac.klagebehandlingContext.klagebehandlingRepo.hentInnstillingsbrevSomSkalJournalføres().size shouldBe 0
            tac.klagebehandlingContext.journalførKlagebrevJobb.journalførInnstillingsbrev()

            tac.klagebehandlingContext.klagebehandlingRepo.hentInnstillingsbrevSomSkalDistribueres().size shouldBe 1
            tac.klagebehandlingContext.distribuerKlagebrevJobb.distribuerInnstillingsbrev()
            verifiserResultat(
                tac = tac,
                id = klagebehandling.id,
                forventetBrevdato = LocalDate.parse("2025-01-01"),
                forventetJournalpostId = JournalpostId("2"),
                forventetJournalføringstidspunkt = true,
                forventetDistribusjonId = DistribusjonId("2"),
                forventetDistribusjonstidspunkt = true,
            )
            // Påser at det ikke feiler og kjøre den samme jobben gang nr. 2:
            tac.klagebehandlingContext.klagebehandlingRepo.hentInnstillingsbrevSomSkalDistribueres().size shouldBe 0
            tac.klagebehandlingContext.distribuerKlagebrevJobb.distribuerInnstillingsbrev()

            tac.klagebehandlingContext.klagebehandlingRepo.hentSakerSomSkalOversendesKlageinstansen().size shouldBe 1
            tac.klagebehandlingContext.oversendKlageTilKlageinstansJobb.oversendKlagerTilKlageinstans()
            verifiserResultat(
                tac = tac,
                id = klagebehandling.id,
                forventetBrevdato = LocalDate.parse("2025-01-01"),
                forventetJournalpostId = JournalpostId("2"),
                forventetJournalføringstidspunkt = true,
                forventetDistribusjonId = DistribusjonId("2"),
                forventetDistribusjonstidspunkt = true,
                forventetOversendtKlageinstansenTidspunkt = true,
            )
            // Påser at det ikke feiler og kjøre den samme jobben gang nr. 2:
            tac.klagebehandlingContext.klagebehandlingRepo.hentSakerSomSkalOversendesKlageinstansen().size shouldBe 0
            tac.klagebehandlingContext.oversendKlageTilKlageinstansJobb.oversendKlagerTilKlageinstans()
        }
    }

    private fun verifiserResultat(
        tac: TestApplicationContext,
        id: KlagebehandlingId,
        forventetIverksattTidspunkt: Boolean = true,
        forventetBrevdato: LocalDate? = null,
        forventetJournalpostId: JournalpostId? = null,
        forventetJournalføringstidspunkt: Boolean = false,
        forventetDistribusjonId: DistribusjonId? = null,
        forventetDistribusjonstidspunkt: Boolean = false,
        forventetOversendtKlageinstansenTidspunkt: Boolean = false,
    ) {
        (tac.sessionFactory as PostgresSessionFactory).withSession { session ->
            KlagebehandlingPostgresRepo.hentOrNull(id, session)!!.resultat.also {
                it as Klagebehandlingsresultat.Opprettholdt
                (it.iverksattOpprettholdelseTidspunkt != null) shouldBe forventetIverksattTidspunkt
                it.brevdato shouldBe forventetBrevdato
                it.journalpostIdInnstillingsbrev shouldBe forventetJournalpostId
                (it.journalføringstidspunktInnstillingsbrev != null) shouldBe forventetJournalføringstidspunkt
                it.distribusjonIdInnstillingsbrev shouldBe forventetDistribusjonId
                (it.distribusjonstidspunktInnstillingsbrev != null) shouldBe forventetDistribusjonstidspunkt
                (it.oversendtKlageinstansenTidspunkt != null) shouldBe forventetOversendtKlageinstansenTidspunkt
            }
        }
    }
}
