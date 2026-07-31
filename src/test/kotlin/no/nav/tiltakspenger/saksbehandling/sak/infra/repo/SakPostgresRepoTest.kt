package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSak
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException

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
            val hentetSak = sakRepo.hentForFnr(opprettetSak.fnr)!!

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

            sakRepo.hentForFnr(sak1.fnr) shouldBe sak1
            sakRepo.hentForSaksnummer(saksnummer = sak1.saksnummer)!! shouldBe sak1
            sakRepo.hentForSakId(sak1.id) shouldBe sak1
        }
    }

    /**
     * `SakPostgresRepo.hentForFnr` returnerer én sak eller null, og den garantien hviler på `sak_fnr_unique`.
     * Testen verifiserer at constrainten faktisk finnes, ved å forsøke innsettingen den skal stoppe.
     * En constraint er ikke sterkere enn migreringen som holder den i live, jf. «Full dekning på postgres-repoene» i `../AGENTS-backend.md`.
     */
    @Test
    fun `en person kan ikke ha to saker`() {
        withMigratedDb { testDataHelper ->
            val sak = testDataHelper.persisterSak()

            val forsøkPåDuplikat = shouldThrow<PSQLException> {
                testDataHelper.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            """
                            insert into sak (id, fnr, saksnummer, sist_endret, opprettet, skal_sendes_til_meldekort_api, skal_sende_meldeperioder_til_datadeling, sendt_til_datadeling)
                            select :id, fnr, :saksnummer, sist_endret, opprettet, false, false, null from sak where id = :kildeId
                            """.trimIndent(),
                            mapOf(
                                "id" to SakId.random().toString(),
                                "saksnummer" to "202101011999",
                                "kildeId" to sak.id.toString(),
                            ),
                        ).asUpdate,
                    )
                }
            }

            forsøkPåDuplikat.message shouldContain "sak_fnr_unique"
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
