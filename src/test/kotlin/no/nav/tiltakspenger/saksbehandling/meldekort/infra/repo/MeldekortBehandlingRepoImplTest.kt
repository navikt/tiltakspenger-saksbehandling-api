package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.left
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingBegrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettManuellMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.genererSimuleringFraBeregning
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import org.junit.jupiter.api.Test

class MeldekortBehandlingRepoImplTest {

    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val meldekortRepo = testDataHelper.meldekortRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak) = testDataHelper.persisterIverksattSøknadsbehandling(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )

            val meldekortBehandling = ObjectMother.meldekortBehandletManuelt(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                meldeperiode = sak.meldeperiodeKjeder.first().first(),
                periode = sak.meldeperiodeKjeder.first().first().periode,
                begrunnelse = MeldekortBehandlingBegrunnelse("begrunnelse"),
            ).let {
                val simuleringMedMetadata = sak.genererSimuleringFraBeregning(it)
                val medSimulering = it.copy(simulering = simuleringMedMetadata.simulering)
                meldekortRepo.lagre(medSimulering, simuleringMedMetadata)
                medSimulering
            }
            testDataHelper.sessionFactory.withSession {
                MeldekortBehandlingPostgresRepo.hentForMeldekortId(
                    meldekortBehandling.id,
                    it,
                )!! shouldBe meldekortBehandling
                MeldekortBehandlingPostgresRepo.hentForSakId(sak.id, it)!! shouldBe MeldekortBehandlinger(
                    listOf(meldekortBehandling),
                )
            }

            val oppdatertSak = sakRepo.hentForSakId(sak.id)!!

            val nesteMeldekort = oppdatertSak.opprettManuellMeldekortBehandling(
                oppdatertSak.meldeperiodeKjeder[1].kjedeId,
                ObjectMother.navkontor(),
                ObjectMother.saksbehandler(),
                fixedClock,
            ).second.also { meldekortRepo.lagre(it, null) }

            val hentForMeldekortId2 =
                testDataHelper.sessionFactory.withSession {
                    MeldekortBehandlingPostgresRepo.hentForMeldekortId(nesteMeldekort.id, it)
                }
            hentForMeldekortId2 shouldBe nesteMeldekort
        }
    }

    @Test
    fun `kan oppdatere`() {
        withMigratedDb { testDataHelper ->
            runTest {
                val (sak) = testDataHelper.persisterIverksattSøknadsbehandling(
                    deltakelseFom = 1.januar(2024),
                    deltakelseTom = 31.mars(2024),
                )
                val meldekortBehandling = sak.opprettManuellMeldekortBehandling(
                    sak.meldeperiodeKjeder.first().kjedeId,
                    ObjectMother.navkontor(),
                    ObjectMother.saksbehandler(),
                    fixedClock,
                ).second

                val meldekortRepo = testDataHelper.meldekortRepo

                meldekortRepo.lagre(meldekortBehandling, null)

                val (oppdatertMeldekortBehandling, simulering) = meldekortBehandling.oppdater(
                    beregn = {
                        ObjectMother.meldekortBeregning(
                            sakId = sak.id,
                            startDato = meldekortBehandling.periode.fraOgMed,
                            meldekortId = meldekortBehandling.id,
                            maksDagerMedTiltakspengerForPeriode = meldekortBehandling.meldeperiode.maksAntallDagerForMeldeperiode,
                        ).beregninger
                    },
                    simuler = { KunneIkkeSimulere.UkjentFeil.left() },
                    kommando = meldekortBehandling.tilOppdaterMeldekortKommando(
                        ObjectMother.saksbehandler(),
                    ),
                ).getOrFail()

                meldekortRepo.oppdater(oppdatertMeldekortBehandling, simulering)

                testDataHelper.sessionFactory.withSession {
                    MeldekortBehandlingPostgresRepo.hentForMeldekortId(
                        meldekortBehandling.id,
                        it,
                    )!! shouldBe oppdatertMeldekortBehandling
                }
            }
        }
    }
}
