package no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.nyPersonhendelse
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Tilstanden bygges gjennom prodstiene: vedtak og behandlinger lages via routene, og hendelsene kommer inn via [no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.LeesahConsumer].
 * Jobben kjøres per id slik sveipene gjør i prod.
 * Sveipemetodene kalles ikke: de ville plukket opp parallelle testers hendelser, jf. «Fakes er per test, jobber sveiper over hele skjemaet» i `AGENTS-backend.md`.
 *
 * Klokka står på 1. mai 2025, så innvilgelsesperiodene under er valgt for å ligge før, rundt og etter «nå».
 */
class PersonhendelseJobbTest {

    private val periodeFørNå: Periode = 1.januar(2025) til 31.mars(2025)
    private val periodeRundtNå: Periode = 1.april(2025) til 30.juni(2025)
    private val periodeEtterNå: Periode = 1.juni(2025) til 31.august(2025)

    @Test
    fun `opprettOppgaveForPersonhendelse - ingen vedtak - sletter fra db`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)
            val id = konsumerDødsfallhendelse(tac, fnr, sak.id)

            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(id)

            tac.personhendelseRepository.hent(sak.id) shouldBe emptyList()
            (tac.oppgaveKlient as OppgaveFakeKlient).opprettedeOppgaverUtenDuplikatkontroll shouldBe emptyList()
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelse - vedtak tilbake i tid - sletter fra db`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val sak = iverksettMedPeriode(tac, fnr, periodeFørNå)
            val id = konsumerDødsfallhendelse(tac, fnr, sak)

            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(id)

            tac.personhendelseRepository.hent(sak) shouldBe emptyList()
            (tac.oppgaveKlient as OppgaveFakeKlient).opprettedeOppgaverUtenDuplikatkontroll shouldBe emptyList()
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelse - har vedtak nå - oppretter oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val sak = iverksettMedPeriode(tac, fnr, periodeRundtNå)
            val id = konsumerDødsfallhendelse(tac, fnr, sak)

            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(id)

            tac.personhendelseRepository.hent(sak).single().oppgaveId shouldNotBe null
            (tac.oppgaveKlient as OppgaveFakeKlient).opprettedeOppgaverUtenDuplikatkontroll shouldBe
                listOf(fnr to Oppgavebehov.DOED)
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelse - har vedtak frem i tid - oppretter oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val sak = iverksettMedPeriode(tac, fnr, periodeEtterNå)
            val id = konsumerDødsfallhendelse(tac, fnr, sak)

            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(id)

            tac.personhendelseRepository.hent(sak).single().oppgaveId shouldNotBe null
            (tac.oppgaveKlient as OppgaveFakeKlient).opprettedeOppgaverUtenDuplikatkontroll shouldBe
                listOf(fnr to Oppgavebehov.DOED)
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelse - har vedtak nå, adressebeskyttelse - oppretter ikke oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val sak = iverksettMedPeriode(tac, fnr, periodeRundtNå)
            val id = konsumerAdressebeskyttelseshendelse(tac, fnr, sak)

            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(id)

            tac.personhendelseRepository.hent(sak) shouldBe emptyList()
            (tac.oppgaveKlient as OppgaveFakeKlient).opprettedeOppgaverUtenDuplikatkontroll shouldBe emptyList()
        }
    }

    @Test
    fun `opprettOppgaveForPersonhendelse - har åpen behandling, adressebeskyttelse - oppretter oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _, _) = opprettSøknadsbehandlingUnderBehandling(tac = tac, fnr = fnr)
            val id = konsumerAdressebeskyttelseshendelse(tac, fnr, sak.id)

            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(id)

            tac.personhendelseRepository.hent(sak.id).single().oppgaveId shouldNotBe null
            (tac.oppgaveKlient as OppgaveFakeKlient).opprettedeOppgaverUtenDuplikatkontroll shouldBe
                listOf(fnr to Oppgavebehov.ADRESSEBESKYTTELSE)
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ikke ferdigstilt - oppdaterer sist sjekket`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val sak = iverksettMedPeriode(tac, fnr, periodeRundtNå)
            val id = konsumerDødsfallhendelse(tac, fnr, sak)
            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(id)
            val oppgaveId = tac.personhendelseRepository.hent(sak).single().oppgaveId
            (tac.oppgaveKlient as OppgaveFakeKlient).erFerdigstiltResponse = false

            tac.personhendelseJobb.ryddOppPersonhendelse(id)

            val etterOpprydning = tac.personhendelseRepository.hent(sak).single()
            etterOpprydning.oppgaveId shouldBe oppgaveId
            etterOpprydning.oppgaveSistSjekket shouldNotBe null
        }
    }

    @Test
    fun `opprydning - opprettet oppgave, ferdigstilt - sletter fra db`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val sak = iverksettMedPeriode(tac, fnr, periodeRundtNå)
            val id = konsumerDødsfallhendelse(tac, fnr, sak)
            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(id)
            (tac.oppgaveKlient as OppgaveFakeKlient).erFerdigstiltResponse = true

            tac.personhendelseJobb.ryddOppPersonhendelse(id)

            tac.personhendelseRepository.hent(sak) shouldBe emptyList()
        }
    }

    @Test
    fun `hentIderUtenOppgave - plukker kun opp hendelser uten oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnrUtenOppgave = Fnr.random()
            val (sakUtenOppgave, _) = opprettSakOgSøknad(tac = tac, fnr = fnrUtenOppgave)
            val idUtenOppgave = konsumerDødsfallhendelse(tac, fnrUtenOppgave, sakUtenOppgave.id)
            val fnrMedOppgave = Fnr.random()
            val sakMedOppgave = iverksettMedPeriode(tac, fnrMedOppgave, periodeRundtNå)
            val idMedOppgave = konsumerDødsfallhendelse(tac, fnrMedOppgave, sakMedOppgave)
            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(idMedOppgave)

            val iderUtenOppgave = tac.personhendelseRepository.hentIderUtenOppgave()

            iderUtenOppgave shouldContain idUtenOppgave
            iderUtenOppgave shouldNotContain idMedOppgave
        }
    }

    @Test
    fun `hentIderMedOppgave - plukker kun opp hendelser med oppgave som ikke nylig er sjekket`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnrUtenOppgave = Fnr.random()
            val (sakUtenOppgave, _) = opprettSakOgSøknad(tac = tac, fnr = fnrUtenOppgave)
            val idUtenOppgave = konsumerDødsfallhendelse(tac, fnrUtenOppgave, sakUtenOppgave.id)

            val fnrNyligSjekket = Fnr.random()
            val sakNyligSjekket = iverksettMedPeriode(tac, fnrNyligSjekket, periodeRundtNå)
            val idNyligSjekket = konsumerDødsfallhendelse(tac, fnrNyligSjekket, sakNyligSjekket)
            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(idNyligSjekket)
            (tac.oppgaveKlient as OppgaveFakeKlient).erFerdigstiltResponse = false
            tac.personhendelseJobb.ryddOppPersonhendelse(idNyligSjekket)

            val fnrIkkeSjekket = Fnr.random()
            val sakIkkeSjekket = iverksettMedPeriode(tac, fnrIkkeSjekket, periodeRundtNå)
            val idIkkeSjekket = konsumerDødsfallhendelse(tac, fnrIkkeSjekket, sakIkkeSjekket)
            tac.personhendelseJobb.opprettOppgaveForPersonhendelse(idIkkeSjekket)

            val iderMedOppgave = tac.personhendelseRepository.hentIderMedOppgave()

            iderMedOppgave shouldContain idIkkeSjekket
            iderMedOppgave shouldNotContain idUtenOppgave
            iderMedOppgave shouldNotContain idNyligSjekket
        }
    }

    /**
     * Iverksetter en søknadsbehandling med innvilgelse i [periode] og returnerer sak-id-en.
     * Jobbene etter iverksettelse slås av, siden testene bygger flere saker og jobbene sveiper på tvers av dem.
     */
    private suspend fun ApplicationTestBuilder.iverksettMedPeriode(
        tac: TestApplicationContextMedPostgres,
        fnr: Fnr,
        periode: Periode,
    ): SakId {
        val tiltaksdeltakelse = tac.tiltaksdeltakelse(periode = periode)
        val (sak) = iverksettSøknadsbehandling(
            tac = tac,
            fnr = fnr,
            innvilgelsesperioder = innvilgelsesperioder(periode, tiltaksdeltakelse),
            tiltaksdeltakelse = tiltaksdeltakelse,
            jobber = JobberEtterIverksettelse.ingen,
        )
        return sak.id
    }

    /** Sender en dødsfallhendelse inn via consumeren og returnerer id-en den ble lagret med. */
    private suspend fun konsumerDødsfallhendelse(
        tac: TestApplicationContextMedPostgres,
        fnr: Fnr,
        sakId: SakId,
    ): UUID {
        tac.leesahConsumer.consume(
            "key",
            nyPersonhendelse(
                fnr = fnr,
                doedsfall = Doedsfall(LocalDate.now(tac.clock)),
                clock = tac.clock,
            ),
        )
        return tac.personhendelseRepository.hent(sakId).single().id
    }

    /**
     * Sender en kode 6-hendelse inn via consumeren og returnerer id-en den ble lagret med.
     * Personen registreres som strengt fortrolig i fake-klienten først, ellers avviser servicen hendelsen.
     */
    private suspend fun konsumerAdressebeskyttelseshendelse(
        tac: TestApplicationContextMedPostgres,
        fnr: Fnr,
        sakId: SakId,
    ): UUID {
        (tac.personContext.personKlient as PersonFakeKlient).leggTilPersonopplysning(
            fnr = fnr,
            personopplysninger = ObjectMother.personopplysningKjedeligFyr(fnr = fnr, strengtFortrolig = true),
        )
        tac.leesahConsumer.consume(
            "key",
            nyPersonhendelse(
                fnr = fnr,
                adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                clock = tac.clock,
            ),
        )
        return tac.personhendelseRepository.hent(sakId).single().id
    }
}
