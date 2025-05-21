package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

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

internal class SakRepoTest {
    @Test
    fun `lagre og hente en sak uten soknad eller behandling`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo

            val opprettetSak = testDataHelper.persisterSak()
            val hentetSak = sakRepo.hentForFnr(opprettetSak.fnr).first()

            hentetSak.behandlinger.behandlinger shouldBe emptyList()
            hentetSak.vedtaksliste.value shouldBe emptyList()
            hentetSak.meldekortBehandlinger.verdi shouldBe emptyList()
            hentetSak.meldeperiodeKjeder.meldeperioder shouldBe emptyList()
            hentetSak.brukersMeldekort shouldBe emptyList()
            hentetSak.utbetalinger.verdi shouldBe emptyList()
            hentetSak.soknader shouldBe emptyList()
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
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo

            val sak1 = testDataHelper.persisterIverksattSøknadsbehandling().first
            val sak2 = testDataHelper.persisterIverksattSøknadsbehandling().first
            testDataHelper.persisterOpprettetSøknadsbehandling().first
            testDataHelper.persisterUnderBeslutningSøknadsbehandling().first

            sakRepo.hentForSendingTilMeldekortApi() shouldBe listOf(sak1, sak2)
        }
    }
}
