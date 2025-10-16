package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterSak
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
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
}
