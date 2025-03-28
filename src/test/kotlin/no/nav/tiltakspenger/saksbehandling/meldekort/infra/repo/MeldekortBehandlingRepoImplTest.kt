package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingBegrunnelse
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
                begrunnelse = MeldekortbehandlingBegrunnelse("begrunnelse"),
            ).also { meldekortRepo.lagre(it) }

            testDataHelper.sessionFactory.withSession {
                MeldekortBehandlingPostgresRepo.hentForMeldekortId(meldekort.id, it)!! shouldBe meldekort
                MeldekortBehandlingPostgresRepo.hentForSakId(sak.id, it)!! shouldBe MeldekortBehandlinger(
                    listOf(meldekort),
                )
            }

            val oppdatertSak = sakRepo.hentForSakId(sak.id)!!

            val nesteMeldekort = oppdatertSak.opprettMeldekortBehandling(
                oppdatertSak.meldeperiodeKjeder[1].kjedeId,
                ObjectMother.navkontor(),
                ObjectMother.saksbehandler(),
                fixedClock,
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
                fixedClock,
            )

            val meldekortRepo = testDataHelper.meldekortRepo

            meldekortRepo.lagre(meldekortBehandling)

            val oppdatertMeldekortBehandling = meldekortBehandling.sendTilBeslutter(
                beregning = ObjectMother.utfyltMeldekortperiode(
                    sakId = sak.id,
                    startDato = meldekortBehandling.periode.fraOgMed,
                    meldekortId = meldekortBehandling.id,
                    maksDagerMedTiltakspengerForPeriode = meldekortBehandling.beregning.maksDagerMedTiltakspengerForPeriode,
                ),
                saksbehandler = ObjectMother.saksbehandler(),
                clock = fixedClock,
                begrunnelse = null,
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
