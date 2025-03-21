package no.nav.tiltakspenger.saksbehandling.repository.meldekort

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.persisterNySak
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.februar
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
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
        fun `hentSakerSomMåGenerereMeldeperioderFra - `() {
            withMigratedDb { testDataHelper ->
                val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
                val sakRepo = testDataHelper.sakRepo

                // Lager en test som kun tester databasespørringen hentSakerSomMåGenerereMeldeperioderFra - uavhengig av domeneimplementasjonen
                val sak = testDataHelper.persisterNySak()

                // Tester at det ikke er generert noen meldeperioder enda. Skal ikke spille noen rolle hvilken dag vi sender inn her.
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(1.januar(2000)) shouldBe emptyList()
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(1.januar(2025)) shouldBe emptyList()
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(1.januar(2050)) shouldBe emptyList()
                // Later som det finnes et vedtak som siste dag gir rett 31.januar(2025)
                sakRepo.oppdaterSisteDagSomGirRett(sak.id, 31.januar(2025))
                // TODO jah: Virkelig bug som må fikses. Vi må også persistere førsteDagSomGirRett, for å kunne avgjøre om vi skal generere en meldeperiode dersom det ikke finnes meldeperioder fra før.
                // meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(ikkeGenererEtter = 1.januar(2050)) shouldBe listOf(sak)

                testDataHelper.sessionFactory.withSessionContext {
                    meldeperiodeRepo.lagre(
                        ObjectMother.meldeperiode(
                            sakId = sak.id,
                            saksnummer = sak.saksnummer,
                            periode = Periode(6.januar(2025), 19.januar(2025)),
                        ),
                        it,
                    )
                }

                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(19.januar(2025)) shouldBe emptyList()
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(20.januar(2025)) shouldBe listOf(sak.id)
                testDataHelper.sessionFactory.withSessionContext {
                    meldeperiodeRepo.lagre(
                        ObjectMother.meldeperiode(
                            sakId = sak.id,
                            saksnummer = sak.saksnummer,
                            periode = Periode(20.januar(2025), 2.februar(2025)),
                        ),
                        it,
                    )
                }
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(19.januar(2025)) shouldBe emptyList()

                sakRepo.oppdaterSisteDagSomGirRett(sak.id, 2.februar(2025))
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(2.februar(2025)) shouldBe emptyList()

                sakRepo.oppdaterSisteDagSomGirRett(sak.id, 3.februar(2025))
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(2.februar(2025)) shouldBe emptyList()
                meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(3.februar(2025)) shouldBe listOf(sak.id)
            }
        }
    }
}
