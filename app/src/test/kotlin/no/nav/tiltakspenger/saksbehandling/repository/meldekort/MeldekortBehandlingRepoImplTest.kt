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
            val meldekortRepo = testDataHelper.meldekortRepo
            val sakRepo = testDataHelper.sakRepo

            val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 2.januar(2023),
                deltakelseTom = 2.april(2023),
            )
            val meldekort = ObjectMother.meldekortBehandlet(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                meldeperiode = sak.meldeperiodeKjeder.first().first(),
                periode = sak.meldeperiodeKjeder.first().first().periode,
            ).also { meldekortRepo.lagre(it) }

            testDataHelper.sessionFactory.withSession {
                MeldekortBehandlingPostgresRepo.hentForMeldekortId(meldekort.id, it)!! shouldBe meldekort
                MeldekortBehandlingPostgresRepo.hentForSakId(sak.id, it)!! shouldBe MeldekortBehandlinger(
                    listOf(meldekort),
                )
            }

            val oppdatertSak = sakRepo.hentForSakId(sak.id)!!

            val nesteMeldekort = oppdatertSak.opprettMeldekortBehandling(
                oppdatertSak.meldeperiodeKjeder.last().kjedeId,
                ObjectMother.navkontor(),
                ObjectMother.saksbehandler(),
            ).also { meldekortRepo.lagre(it) }

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
            val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )
            val meldekortBehandling = sak.opprettMeldekortBehandling(
                sak.meldeperiodeKjeder.first().kjedeId,
                ObjectMother.navkontor(),
                ObjectMother.saksbehandler(),
            )

            val meldekortRepo = testDataHelper.meldekortRepo

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
                MeldekortBehandlingPostgresRepo.hentForMeldekortId(
                    meldekortBehandling.id,
                    it,
                )!! shouldBe oppdatertMeldekortBehandling
            }
        }
    }
}
