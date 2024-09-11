package no.nav.tiltakspenger.vedtak.repository.behandling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.vedtak.db.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.vedtak.db.withMigratedDb
import org.junit.jupiter.api.Test
import java.util.Random

internal class BehandlingRepoTest {
    companion object {
        val random = Random()
    }

    @Test
    fun `lagre og hente en behandling`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak, _) = testDataHelper.persisterOpprettetFørstegangsbehandling()
            sakRepo.hentForSakId(sak.id) shouldBe sak
            behandlingRepo.hent(sak.førstegangsbehandling.id) shouldBe sak.førstegangsbehandling
        }
    }

    @Test
    fun `hentAlleForIdent skal kun hente behandlinger for en ident og ikke de andre`() {
        withMigratedDb { testDataHelper ->
            val behandlingRepo = testDataHelper.behandlingRepo

            val (sak1, _) = testDataHelper.persisterOpprettetFørstegangsbehandling()
            val (sak2, _) = testDataHelper.persisterOpprettetFørstegangsbehandling()

            behandlingRepo.hentAlleForIdent(sak1.fnr) shouldBe listOf(sak1.førstegangsbehandling)
            behandlingRepo.hentAlleForIdent(sak2.fnr) shouldBe listOf(sak2.førstegangsbehandling)
        }
    }
}
