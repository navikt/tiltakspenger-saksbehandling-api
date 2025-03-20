package no.nav.tiltakspenger.saksbehandling.repository.meldekort

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.april
import no.nav.tiltakspenger.saksbehandling.felles.februar
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mai
import no.nav.tiltakspenger.saksbehandling.felles.mars
import org.junit.jupiter.api.Nested
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

    @Nested
    inner class HentSakerSomMåGenerereMeldeperioderFra {
        @Test
        fun `sak har meldeperioder og skal ikke generere flere`() {
            withMigratedDb { testDataHelper ->
                val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
                val sakRepo = testDataHelper.sakRepo
                val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                    deltakelseFom = 1.februar(2025),
                    deltakelseTom = 28.februar(2025),
                )
                sakRepo.oppdaterSisteDagSomGirRett(sak.id, sak.sisteDagSomGirRett)
                sak.meldeperiodeKjeder.meldeperioder.size shouldBe 3
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(1.februar(2025)) shouldBe emptyList()
            }
        }

        @Test
        fun `sak må generere`() {
            withMigratedDb { testDataHelper ->
                val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
                val sakRepo = testDataHelper.sakRepo
                val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                    deltakelseFom = 24.februar(2025),
                    deltakelseTom = 6.april(2025),
                )
                sakRepo.oppdaterSisteDagSomGirRett(sak.id, sak.sisteDagSomGirRett)
                sak.meldeperiodeKjeder.meldeperioder.size shouldBe 1
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(1.mai(2025)) shouldBe listOf(sak)
            }
        }
    }
}
