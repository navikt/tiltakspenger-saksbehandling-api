package no.nav.tiltakspenger.vedtak.repository.meldekort

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.domene.opprettFørsteMeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.opprettFørsteMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.opprettNesteMeldeperiode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import no.nav.tiltakspenger.vedtak.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.vedtak.db.withMigratedDb
import org.junit.jupiter.api.Test

class MeldekortBehandlingRepoImplTest {

    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak, vedtak) = testDataHelper.persisterIverksattFørstegangsbehandling()

            val førsteMeldeperiode = sak.opprettFørsteMeldeperiode()
            val meldekort =
                ObjectMother.utfyltMeldekort(
                    sakId = sak.id,
                    rammevedtakId = vedtak.id,
                    fnr = sak.fnr,
                    saksnummer = sak.saksnummer,
                    antallDagerForMeldeperiode = vedtak.antallDagerPerMeldeperiode,
                    meldeperiode = førsteMeldeperiode,
                )

            val meldekortRepo = testDataHelper.meldekortRepo
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val sakRepo = testDataHelper.sakRepo

            meldekortRepo.lagre(meldekort)
            meldeperiodeRepo.lagre(meldekort.meldeperiode)

            testDataHelper.sessionFactory.withSession {
                MeldekortPostgresRepo.hentForMeldekortId(meldekort.id, it)!! shouldBe meldekort
                MeldekortPostgresRepo.hentForSakId(sak.id, it)!! shouldBe MeldekortBehandlinger(
                    meldekort.tiltakstype,
                    listOf(meldekort),
                )
            }

            val oppdatertSak = sakRepo.hentForSakId(sak.id)!!

            val nesteMeldeperiode = oppdatertSak.opprettNesteMeldeperiode()!!
            val nesteMeldekort =
                meldekort.opprettNesteMeldekortBehandling(
                    Periodisering(
                        AvklartUtfallForPeriode.OPPFYLT,
                        Periode(
                            fraOgMed = meldekort.periode.fraOgMed.plusWeeks(2),
                            tilOgMed = meldekort.periode.tilOgMed.plusWeeks(2),
                        ),
                    ),
                    nesteMeldeperiode,
                ).getOrFail()

            meldeperiodeRepo.lagre(nesteMeldeperiode)
            meldekortRepo.lagre(nesteMeldekort)

            val hentForMeldekortId2 =
                testDataHelper.sessionFactory.withSession {
                    MeldekortPostgresRepo.hentForMeldekortId(
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
            val (sak, vedtak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )
            val meldeperiode = sak.opprettFørsteMeldeperiode()
            val meldekortBehandling = vedtak.opprettFørsteMeldekortBehandling(meldeperiode)

            val meldekortRepo = testDataHelper.meldekortRepo
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo

            meldekortRepo.lagre(meldekortBehandling)
            meldeperiodeRepo.lagre(meldeperiode)

            val oppdatertMeldekort = meldekortBehandling.sendTilBeslutter(
                utfyltMeldeperiode = ObjectMother.utfyltMeldekortperiode(
                    sakId = sak.id,
                    startDato = meldekortBehandling.periode.fraOgMed,
                    meldekortId = meldekortBehandling.id,
                    tiltakstype = meldekortBehandling.tiltakstype,
                    maksDagerMedTiltakspengerForPeriode = meldekortBehandling.beregning.maksDagerMedTiltakspengerForPeriode,
                ),
                saksbehandler = ObjectMother.saksbehandler(),
                navkontor = Navkontor("0222"),
            ).getOrFail()

            meldekortRepo.oppdater(oppdatertMeldekort)

            testDataHelper.sessionFactory.withSession {
                MeldekortPostgresRepo.hentForMeldekortId(meldekortBehandling.id, it)!! shouldBe oppdatertMeldekort
            }
        }
    }
}
