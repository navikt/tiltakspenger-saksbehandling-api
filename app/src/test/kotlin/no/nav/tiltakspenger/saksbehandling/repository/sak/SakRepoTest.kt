package no.nav.tiltakspenger.saksbehandling.repository.sak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.persisterNySak
import no.nav.tiltakspenger.saksbehandling.db.persisterOpprettetFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.persisterSak
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.desember
import no.nav.tiltakspenger.saksbehandling.felles.februar
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saker
import org.junit.jupiter.api.Nested
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

    @Nested
    inner class HentSakerSomMåGenerereMeldeperioderFra {
        @Test
        fun `sak har meldeperioder og skal ikke generere flere`() {
            withMigratedDb { testDataHelper ->
                val sakRepo = testDataHelper.sakRepo
                val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                    deltakelseFom = 1.februar(2025),
                    deltakelseTom = 28.februar(2025),
                )
                sakRepo.oppdaterFørsteOgSisteDagSomGirRett(sak.id, sak.førsteDagSomGirRett, sak.sisteDagSomGirRett)
                sak.meldeperiodeKjeder.meldeperioder.size shouldBe 3
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(1.februar(2025)) shouldBe emptyList()
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
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(1.januar(2000)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(1.januar(2025)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(1.januar(2050)) shouldBe emptyList()
                // Later som det finnes et vedtak med innvilgelsesperiode hele januar 2025
                sakRepo.oppdaterFørsteOgSisteDagSomGirRett(sak.id, 1.januar(2025), 31.januar(2025))
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(ikkeGenererEtter = 31.desember(2024)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(ikkeGenererEtter = 1.januar(2025)) shouldBe listOf(sak.id)
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(ikkeGenererEtter = 1.januar(2050)) shouldBe listOf(sak.id)

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

                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(19.januar(2025)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(20.januar(2025)) shouldBe listOf(sak.id)
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(20.januar(2050)) shouldBe listOf(sak.id)
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
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(19.januar(2000)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(19.januar(2025)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(19.januar(2050)) shouldBe emptyList()

                sakRepo.oppdaterFørsteOgSisteDagSomGirRett(sak.id, 1.januar(2025), 2.februar(2025))
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(2.februar(2000)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(2.februar(2025)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(2.februar(2050)) shouldBe emptyList()

                sakRepo.oppdaterFørsteOgSisteDagSomGirRett(sak.id, 1.januar(2025), 3.februar(2025))
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(2.februar(2000)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(2.februar(2025)) shouldBe emptyList()
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(3.februar(2025)) shouldBe listOf(sak.id)
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra(3.februar(2050)) shouldBe listOf(sak.id)
            }
        }
    }
}
