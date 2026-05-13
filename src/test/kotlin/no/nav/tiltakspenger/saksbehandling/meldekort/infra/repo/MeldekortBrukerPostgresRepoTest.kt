package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class MeldekortBrukerPostgresRepoTest {
    private fun lagBrukersMeldekort(meldeperiode: Meldeperiode): BrukersMeldekort {
        return ObjectMother.lagreBrukersMeldekortKommando(
            meldeperiodeId = meldeperiode.id,
            mottatt = meldeperiode.opprettet.plus(1, ChronoUnit.MILLIS),
            sakId = meldeperiode.sakId,
            periode = meldeperiode.periode,
        ).tilNyttBrukersMeldekort(meldeperiode, fixedClock)
    }

    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak) = testDataHelper.persisterIverksattSøknadsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )
            val meldeperiode = sak.meldeperiodeKjeder.first().first()
            val meldekortBrukerRepo = testDataHelper.meldekortBrukerRepo

            val nyttBrukersMeldekort = lagBrukersMeldekort(meldeperiode)

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
                    behandlesAutomatisk = true,
                    behandletAutomatiskStatus = MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING,
                    behandletAutomatiskForsøkshistorikk = Forsøkshistorikk.opprett(clock = fixedClock),
                ),
            )
        }
    }

    @Test
    fun `Skal hente kun det neste (eldste) meldekortet for automatisk behandling per sak`() {
        withMigratedDb(runIsolated = true) { testDataHelper ->
            val meldekortBrukerRepo = testDataHelper.meldekortBrukerRepo

            val (sak1) = testDataHelper.persisterIverksattSøknadsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )

            val (sak2) = testDataHelper.persisterIverksattSøknadsbehandling(
                deltakelseFom = 1.april(2024),
                deltakelseTom = 30.juni(2024),
            )

            val sak1meldeperiode1 = sak1.meldeperiodeKjeder[0].first()
            val sak1brukersMeldekort1 = lagBrukersMeldekort(sak1meldeperiode1)
            meldekortBrukerRepo.lagre(sak1brukersMeldekort1)

            val sak1meldeperiode2 = sak1.meldeperiodeKjeder[1].first()
            val sak1brukersMeldekort2 = lagBrukersMeldekort(sak1meldeperiode2)
            meldekortBrukerRepo.lagre(sak1brukersMeldekort2)

            val sak2meldeperiode1 = sak2.meldeperiodeKjeder[0].first()
            val sak2brukersMeldekort1 = lagBrukersMeldekort(sak2meldeperiode1)
            meldekortBrukerRepo.lagre(sak2brukersMeldekort1)

            meldekortBrukerRepo.hentMeldekortSomSkalBehandlesAutomatisk() shouldBe listOf(
                sak1brukersMeldekort1,
                sak2brukersMeldekort1,
            )
        }
    }

    @Test
    fun `Skal ikke hente meldekort som allerede er behandlet`() {
        withMigratedDb { testDataHelper ->
            val meldekortBrukerRepo = testDataHelper.meldekortBrukerRepo

            val (sak) = testDataHelper.persisterIverksattSøknadsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )

            val meldekort1 = lagBrukersMeldekort(sak.meldeperiodeKjeder[0].first())
            meldekortBrukerRepo.lagre(meldekort1)
            meldekortBrukerRepo.markerSomAutomatiskBehandlet(
                meldekort1.id,
                Forsøkshistorikk.opprett(clock = testDataHelper.clock),
            )

            val meldekort2 = lagBrukersMeldekort(sak.meldeperiodeKjeder[1].first())
            meldekortBrukerRepo.lagre(meldekort2)

            meldekortBrukerRepo.hentMeldekortSomSkalBehandlesAutomatisk() shouldBe listOf(meldekort2)
        }
    }
}
