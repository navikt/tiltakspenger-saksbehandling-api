package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort.Companion.MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.infra.http.FellesFakeSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test

/**
 * Jobben leser meldekort på tvers av saker, så testene kjøres isolert med styrte klientresponser.
 * Sak, vedtak og meldekort bygges gjennom routene.
 */
class AutomatiskMeldekortEksternOppgaveTest {
    @Test
    @IsolatedDatabaseTest
    fun `manuelt meldekort for beskyttet bruker får Gosys-oppgave uten lagring i ekstern_oppgave`() {
        listOf(
            listOf(true, false, false, false),
            listOf(false, true, false, false),
            listOf(false, false, true, false),
            listOf(false, false, false, true),
        ).forEach { (fortrolig, strengtFortrolig, strengtFortroligUtland, skjermet) ->
            withTestApplicationContextAndPostgres(
                clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
                runIsolated = true,
            ) { tac ->
                val (sak) = iverksettSøknadsbehandling(tac = tac, fnr = ObjectMother.gyldigFnr(), jobber = JobberEtterIverksettelse.ingen)
                val meldekort = mottaMeldekortMedForMyeFravær(tac, sak)
                (tac.personContext.personKlient as PersonFakeKlient).leggTilPersonopplysning(
                    sak.fnr,
                    ObjectMother.personopplysningKjedeligFyr(
                        fnr = sak.fnr,
                        fortrolig = fortrolig,
                        strengtFortrolig = strengtFortrolig,
                        strengtFortroligUtland = strengtFortroligUtland,
                    ),
                )
                (tac.personContext.fellesSkjermingsklient as FellesFakeSkjermingsklient).leggTil(sak.fnr, skjermet)

                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

                (tac.oppgaveKlient as OppgaveFakeKlient).opprettedeOppgaveIder.size shouldBe 1
                tac.eksternOppgaveRepo.hentForSakId(sak.id) shouldBe emptyList()
                val lagret = tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(meldekort.id)!!
                lagret.behandlesAutomatisk shouldBe false
                lagret.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR
                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `oppgavefeil beholder meldekortet til ny kjøring uten sentral referanse`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac, fnr = ObjectMother.gyldigFnr(), jobber = JobberEtterIverksettelse.ingen)
            val meldekort = mottaMeldekortMedForMyeFravær(tac, sak)
            (tac.personContext.fellesSkjermingsklient as FellesFakeSkjermingsklient).leggTil(sak.fnr, true)
            val klient = tac.oppgaveKlient as OppgaveFakeKlient
            klient.opprettOppgaveResponse = ObjectMother.httpKlientUventetStatus().left()

            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            tac.eksternOppgaveRepo.hentForSakId(sak.id) shouldBe emptyList()
            tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(meldekort.id) shouldBe meldekort
            tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

            klient.opprettOppgaveResponse = null
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            klient.opprettedeOppgaveIder.size shouldBe 1
            tac.eksternOppgaveRepo.hentForSakId(sak.id) shouldBe emptyList()
            tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(meldekort.id)!!.behandlesAutomatisk shouldBe false
        }
    }

    private suspend fun ApplicationTestBuilder.mottaMeldekortMedForMyeFravær(
        tac: TestApplicationContextMedPostgres,
        sak: Sak,
        journalpostId: String = "1234",
    ): BrukersMeldekort {
        val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
        val utfylt = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort)
        val fraværsdager = utfylt.keys
            .filter { utfylt.getValue(it) != BrukerutfyltMeldekortDTO.Status.IKKE_BESVART }
            .sorted()
            .windowed(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER + 1)
            .first { vindu -> vindu.zipWithNext().all { (a, b) -> b == a.plusDays(1) } }
            .toSet()
        val (_, meldekort) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            journalpostId = journalpostId,
            dager = utfylt.mapValues { (dato, status) ->
                if (dato in fraværsdager) BrukerutfyltMeldekortDTO.Status.FRAVÆR_GODKJENT_AV_NAV else status
            },
        )
        return meldekort!!
    }
}
