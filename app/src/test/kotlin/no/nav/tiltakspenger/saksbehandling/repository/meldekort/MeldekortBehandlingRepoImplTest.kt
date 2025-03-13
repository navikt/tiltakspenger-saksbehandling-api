package no.nav.tiltakspenger.saksbehandling.repository.meldekort

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.april
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortBehandlingRepoImplTest {

    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )

            val (_, meldeperioder) = sak.genererMeldeperioder()
            val meldekort = ObjectMother.meldekortBehandlet(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                meldeperiode = meldeperioder.first(),
                periode = meldeperioder.first().periode,
            )

            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldekortRepo = testDataHelper.meldekortRepo
            val sakRepo = testDataHelper.sakRepo

            meldeperiodeRepo.lagre(meldekort.meldeperiode)
            meldekortRepo.lagre(meldekort)

            testDataHelper.sessionFactory.withSession {
                MeldekortBehandlingPostgresRepo.hentForMeldekortId(meldekort.id, it)!! shouldBe meldekort
                MeldekortBehandlingPostgresRepo.hentForSakId(sak.id, it)!! shouldBe MeldekortBehandlinger(
                    listOf(meldekort),
                )
            }

            val oppdatertSak = sakRepo.hentForSakId(sak.id)!!

            val (oppdatertSak2, nesteMeldeperioder) = oppdatertSak.genererMeldeperioder()
            val nesteMeldekort = oppdatertSak2.opprettMeldekortBehandling(
                nesteMeldeperioder.first().kjedeId,
                ObjectMother.navkontor(),
                ObjectMother.saksbehandler(),
            )

            meldeperiodeRepo.lagre(nesteMeldeperioder.first())
            meldekortRepo.lagre(nesteMeldekort)

            val hentForMeldekortId2 =
                testDataHelper.sessionFactory.withSession {
                    MeldekortBehandlingPostgresRepo.hentForMeldekortId(
                        nesteMeldekort.id,
                        it,
                    )
                }
            hentForMeldekortId2 shouldBe nesteMeldekort
        }
    }

    @Test
    fun `kan oppdatere`() {
        withMigratedDb { testDataHelper ->
            val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )
            val (_, meldeperioder) = sak.genererMeldeperioder()
            val meldekortBehandling = sak.opprettMeldekortBehandling(
                meldeperioder.first().kjedeId,
                ObjectMother.navkontor(),
                ObjectMother.saksbehandler(),
            )

            val meldekortRepo = testDataHelper.meldekortRepo
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo

            meldeperiodeRepo.lagre(meldeperioder.first())
            meldekortRepo.lagre(meldekortBehandling)

            val oppdatertMeldekortBehandling = meldekortBehandling.sendTilBeslutter(
                utfyltMeldeperiode = ObjectMother.utfyltMeldekortperiode(
                    sakId = sak.id,
                    startDato = meldekortBehandling.periode.fraOgMed,
                    meldekortId = meldekortBehandling.id,
                    maksDagerMedTiltakspengerForPeriode = meldekortBehandling.beregning.maksDagerMedTiltakspengerForPeriode,
                ),
                saksbehandler = ObjectMother.saksbehandler(),
            ).getOrFail()

            meldekortRepo.oppdater(oppdatertMeldekortBehandling)

            testDataHelper.sessionFactory.withSession {
                MeldekortBehandlingPostgresRepo.hentForMeldekortId(meldekortBehandling.id, it)!! shouldBe oppdatertMeldekortBehandling
            }
        }
    }
}
