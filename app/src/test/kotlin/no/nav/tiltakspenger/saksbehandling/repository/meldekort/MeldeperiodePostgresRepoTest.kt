package no.nav.tiltakspenger.saksbehandling.repository.meldekort

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
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
