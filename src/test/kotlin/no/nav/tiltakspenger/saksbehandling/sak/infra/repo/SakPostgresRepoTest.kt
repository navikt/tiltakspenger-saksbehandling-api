package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSak
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterUnderBeslutningSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.sak.Saker
import org.junit.jupiter.api.Test

class SakPostgresRepoTest {
    @Test
    fun `oppdaterer verdi for å sende inn helg for meldekort`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val opprettetSak = testDataHelper.persisterSak()
            sakRepo.hentForSaksnummer(opprettetSak.saksnummer)?.kanSendeInnHelgForMeldekort shouldBe false
            val oppdatertSak = opprettetSak.oppdaterKanSendeInnHelgForMeldekort(true)
            sakRepo.oppdaterKanSendeInnHelgForMeldekort(oppdatertSak.id, oppdatertSak.kanSendeInnHelgForMeldekort)
            sakRepo.hentForSaksnummer(opprettetSak.saksnummer)?.kanSendeInnHelgForMeldekort shouldBe true
        }
    }

    @Test
    fun `lagre og hente en sak uten soknad eller behandling`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo

            val opprettetSak = testDataHelper.persisterSak()
            val hentetSak = sakRepo.hentForFnr(opprettetSak.fnr).first()

            hentetSak.rammebehandlinger.behandlinger shouldBe emptyList()
            hentetSak.rammevedtaksliste.verdi shouldBe emptyList()
            hentetSak.meldekortbehandlinger shouldBe emptyList()
            hentetSak.meldeperiodeKjeder.sisteMeldeperiodePerKjede shouldBe emptyList()
            hentetSak.brukersMeldekort shouldBe emptyList()
            hentetSak.utbetalinger.verdi shouldBe emptyList()
            hentetSak.søknader shouldBe emptyList()
        }
    }

    @Test
    fun `lagre og hente en sak med en søknad`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo

            val sak1 = testDataHelper.persisterOpprettetSøknadsbehandling().first
            testDataHelper.persisterOpprettetSøknadsbehandling().first

            sakRepo.hentForFnr(sak1.fnr) shouldBe Saker(sak1.fnr, listOf(sak1))
            sakRepo.hentForSaksnummer(saksnummer = sak1.saksnummer)!! shouldBe sak1
            sakRepo.hentForSakId(sak1.id) shouldBe sak1
        }
    }

    @Test
    fun `hentForIdent skal hente saker med matchende ident`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo

            val fnr = Fnr.random()

            val sak1 =
                testDataHelper
                    .persisterOpprettetSøknadsbehandling(
                        fnr = fnr,
                    ).first
            val sak2 =
                testDataHelper
                    .persisterOpprettetSøknadsbehandling(
                        fnr = fnr,
                    ).first
            testDataHelper.persisterOpprettetSøknadsbehandling()

            sakRepo.hentForFnr(fnr) shouldBe Saker(fnr, listOf(sak1, sak2))
        }
    }

    @Test
    fun `Skal flagge saker med iverksatt behandling for sending til meldekort-api`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo

            val sak1 = testDataHelper.persisterIverksattSøknadsbehandling().first
            val sak2 = testDataHelper.persisterIverksattSøknadsbehandling().first
            testDataHelper.persisterOpprettetSøknadsbehandling().first
            testDataHelper.persisterUnderBeslutningSøknadsbehandling().first

            sakRepo.hentForSendingTilMeldekortApi() shouldBe listOf(sak1, sak2)
        }
    }

    @Test
    fun `hentSakIdForPersonidenter returnerer null når ingen ident matcher`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            testDataHelper.persisterSak()

            sakRepo.hentSakIdForPersonidenter(nonEmptyListOf(Fnr.random().verdi)) shouldBe null
        }
    }

    @Test
    fun `hentSakIdForPersonidenter returnerer fnr og sakId når én ident matcher`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val sak = testDataHelper.persisterSak()
            // Persister en annen sak for å sikre at vi filtrerer på fnr
            testDataHelper.persisterSak()

            sakRepo.hentSakIdForPersonidenter(
                nonEmptyListOf(sak.fnr.verdi, Fnr.random().verdi),
            ) shouldBe (sak.fnr to sak.id)
        }
    }

    @Test
    fun `hentSakIdForPersonidenter kaster IllegalStateException når flere saker matcher`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val sak1 = testDataHelper.persisterSak()
            val sak2 = testDataHelper.persisterSak()

            shouldThrow<IllegalStateException> {
                sakRepo.hentSakIdForPersonidenter(nonEmptyListOf(sak1.fnr.verdi, sak2.fnr.verdi))
            }
        }
    }
}
