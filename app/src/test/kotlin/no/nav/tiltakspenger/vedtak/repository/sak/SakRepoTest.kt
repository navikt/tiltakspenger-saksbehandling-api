package no.nav.tiltakspenger.vedtak.repository.sak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saker
import no.nav.tiltakspenger.saksbehandling.domene.sak.TynnSak
import no.nav.tiltakspenger.vedtak.db.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.vedtak.db.persisterSak
import no.nav.tiltakspenger.vedtak.db.withMigratedDb
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

            val sak1 = testDataHelper.persisterOpprettetFørstegangsbehandling().first
            testDataHelper.persisterOpprettetFørstegangsbehandling().first
            val tynnSak = TynnSak(sak1.id, sak1.fnr, sak1.saksnummer)

            sakRepo.hentForFnr(sak1.fnr) shouldBe Saker(sak1.fnr, listOf(sak1))
            sakRepo.hentForSaksnummer(saksnummer = sak1.saksnummer)!! shouldBe sak1
            sakRepo.hentForSakId(sak1.id) shouldBe sak1
            sakRepo.hentDetaljerForSakId(sak1.id) shouldBe tynnSak
        }
    }

    @Test
    fun `hentForIdent skal hente saker med matchende ident`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo

            val fnr = Fnr.random()

            val sak1 =
                testDataHelper
                    .persisterOpprettetFørstegangsbehandling(
                        fnr = fnr,
                    ).first
            val sak2 =
                testDataHelper
                    .persisterOpprettetFørstegangsbehandling(
                        fnr = fnr,
                    ).first
            testDataHelper.persisterOpprettetFørstegangsbehandling()

            sakRepo.hentForFnr(fnr) shouldBe Saker(fnr, listOf(sak1, sak2))
        }
    }
}
