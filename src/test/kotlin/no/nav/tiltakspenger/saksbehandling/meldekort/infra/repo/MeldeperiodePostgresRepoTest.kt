package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import org.junit.jupiter.api.Test

class MeldeperiodePostgresRepoTest {
    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )

            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            meldeperiodeRepo.hentForSakId(sak.id) shouldBe sak.meldeperiodeKjeder
        }
    }
}
