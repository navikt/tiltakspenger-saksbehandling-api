package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.juni
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class MeldekortBrukerPostgresRepoTest {
    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )
            val meldeperiode = sak.meldeperiodeKjeder.first().first()
            val meldekortBrukerRepo = testDataHelper.meldekortBrukerRepo

            val nyttBrukersMeldekort = ObjectMother.lagreBrukersMeldekortKommando(
                meldeperiodeId = meldeperiode.id,
                mottatt = meldeperiode.opprettet.plus(1, ChronoUnit.MILLIS),
                sakId = meldeperiode.sakId,
                periode = meldeperiode.periode,
            ).tilBrukersMeldekort(meldeperiode, false)

            meldekortBrukerRepo.lagre(nyttBrukersMeldekort)

            meldekortBrukerRepo.hentForSakId(meldeperiode.sakId) shouldBe listOf(
                BrukersMeldekort(
                    id = nyttBrukersMeldekort.id,
                    mottatt = nyttBrukersMeldekort.mottatt,
                    meldeperiode = meldeperiode,
                    sakId = nyttBrukersMeldekort.sakId,
                    dager = nyttBrukersMeldekort.dager,
                    journalpostId = nyttBrukersMeldekort.journalpostId,
                    oppgaveId = nyttBrukersMeldekort.oppgaveId,
                    behandlesAutomatisk = false,
                    behandletAutomatiskStatus = null,
                ),
            )
        }
    }

    @Test
    fun `Skal hente kun det neste (eldste) meldekortet for automatisk behandling per sak`() {
        withMigratedDb { testDataHelper ->
            val meldekortBrukerRepo = testDataHelper.meldekortBrukerRepo

            val (sak1) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )

            val (sak2) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.april(2024),
                deltakelseTom = 30.juni(2024),
            )

            val sak1meldeperiode1 = sak1.meldeperiodeKjeder.first().first()

            val sak1brukersMeldekort1 = ObjectMother.lagreBrukersMeldekortKommando(
                meldeperiodeId = sak1meldeperiode1.id,
                mottatt = sak1meldeperiode1.opprettet.plus(1, ChronoUnit.MILLIS),
                sakId = sak1.id,
                periode = sak1meldeperiode1.periode,
            ).tilBrukersMeldekort(sak1meldeperiode1, true)

            val sak1meldeperiode2 = sak1.meldeperiodeKjeder[1].first()

            val sak1brukersMeldekort2 = ObjectMother.lagreBrukersMeldekortKommando(
                meldeperiodeId = sak1meldeperiode2.id,
                mottatt = sak1meldeperiode2.opprettet.plus(1, ChronoUnit.MILLIS),
                sakId = sak1.id,
                periode = sak1meldeperiode2.periode,
            ).tilBrukersMeldekort(sak1meldeperiode2, true)

            meldekortBrukerRepo.lagre(sak1brukersMeldekort1)
            meldekortBrukerRepo.lagre(sak1brukersMeldekort2)

            val sak2meldeperiode1 = sak2.meldeperiodeKjeder.first().first()

            val sak2brukersMeldekort1 = ObjectMother.lagreBrukersMeldekortKommando(
                meldeperiodeId = sak2meldeperiode1.id,
                mottatt = sak2meldeperiode1.opprettet.plus(1, ChronoUnit.MILLIS),
                sakId = sak2.id,
                periode = sak2meldeperiode1.periode,
            ).tilBrukersMeldekort(sak2meldeperiode1, true)

            meldekortBrukerRepo.lagre(sak2brukersMeldekort1)

            meldekortBrukerRepo.hentMeldekortSomSkalBehandlesAutomatisk() shouldBe listOf(sak1brukersMeldekort1, sak2brukersMeldekort1)
        }
    }
}
