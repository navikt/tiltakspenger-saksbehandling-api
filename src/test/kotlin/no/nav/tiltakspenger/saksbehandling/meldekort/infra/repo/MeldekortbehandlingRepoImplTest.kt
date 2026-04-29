package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.left
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.oppdaterMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.opprettManuellMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.genererSimuleringFraMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import org.junit.jupiter.api.Test

class MeldekortbehandlingRepoImplTest {

    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val meldekortRepo = testDataHelper.meldekortRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak) = testDataHelper.persisterIverksattSøknadsbehandling(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )

            val meldekortbehandling = ObjectMother.meldekortBehandletManuelt(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                meldeperiode = sak.meldeperiodeKjeder.first().first(),
                periode = sak.meldeperiodeKjeder.first().first().periode,
                begrunnelse = Begrunnelse.create("begrunnelse"),
            ).let {
                val simuleringMedMetadata = sak.genererSimuleringFraMeldekortbehandling(it)
                val medSimulering = it.copy(simulering = simuleringMedMetadata.simulering)
                meldekortRepo.lagre(medSimulering, simuleringMedMetadata)
                medSimulering
            }
            testDataHelper.sessionFactory.withSession {
                MeldekortbehandlingPostgresRepo.hentForMeldekortId(
                    meldekortbehandling.id,
                    it,
                )!! shouldBe meldekortbehandling
                MeldekortbehandlingPostgresRepo.hentForSakId(sak.id, it)!! shouldBe Meldekortbehandlinger(
                    listOf(meldekortbehandling),
                )
            }

            val oppdatertSak = sakRepo.hentForSakId(sak.id)!!

            val nesteMeldekort = oppdatertSak.opprettManuellMeldekortbehandling(
                oppdatertSak.meldeperiodeKjeder[1].kjedeId,
                ObjectMother.navkontor(),
                ObjectMother.saksbehandler(),
                fixedClock,
            ).getOrFail().second.also { meldekortRepo.lagre(it, null) }

            val hentForMeldekortId2 =
                testDataHelper.sessionFactory.withSession {
                    MeldekortbehandlingPostgresRepo.hentForMeldekortId(nesteMeldekort.id, it)
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
                val (sakMedMeldekortbehandling, meldekortbehandling) = sak.opprettManuellMeldekortbehandling(
                    sak.meldeperiodeKjeder.first().kjedeId,
                    ObjectMother.navkontor(),
                    ObjectMother.saksbehandler(),
                    fixedClock,
                ).getOrFail()

                val meldekortRepo = testDataHelper.meldekortRepo

                meldekortRepo.lagre(meldekortbehandling, null)

                val (_, oppdatertMeldekortbehandling, simulering) = sakMedMeldekortbehandling.oppdaterMeldekort(
                    simuler = { KunneIkkeSimulere.UkjentFeil.left() },
                    kommando = meldekortbehandling.tilOppdaterMeldekortKommando(
                        ObjectMother.saksbehandler(),
                    ),
                    clock = fixedClock,
                ).getOrFail()

                meldekortRepo.oppdater(oppdatertMeldekortbehandling, simulering)

                testDataHelper.sessionFactory.withSession {
                    MeldekortbehandlingPostgresRepo.hentForMeldekortId(
                        meldekortbehandling.id,
                        it,
                    )!! shouldBe oppdatertMeldekortbehandling
                }
            }
        }
    }
}
