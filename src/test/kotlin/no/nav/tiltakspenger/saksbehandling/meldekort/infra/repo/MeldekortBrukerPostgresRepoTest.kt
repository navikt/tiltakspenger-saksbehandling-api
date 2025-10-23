package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class MeldekortBrukerPostgresRepoTest {
    private fun lagBrukersMeldekort(
        meldeperiode: Meldeperiode,
        behandlesAutomatisk: Either<BrukersMeldekortBehandletAutomatiskStatus, Unit>,
    ): BrukersMeldekort {
        return ObjectMother.lagreBrukersMeldekortKommando(
            meldeperiodeId = meldeperiode.id,
            mottatt = meldeperiode.opprettet.plus(1, ChronoUnit.MILLIS),
            sakId = meldeperiode.sakId,
            periode = meldeperiode.periode,
        ).tilBrukersMeldekort(meldeperiode, behandlesAutomatisk)
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

            val nyttBrukersMeldekort =
                lagBrukersMeldekort(meldeperiode, BrukersMeldekortBehandletAutomatiskStatus.UKJENT_FEIL.left())

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
                    behandletAutomatiskStatus = BrukersMeldekortBehandletAutomatiskStatus.UKJENT_FEIL,
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
            val sak1brukersMeldekort1 = lagBrukersMeldekort(sak1meldeperiode1, Unit.right())
            meldekortBrukerRepo.lagre(sak1brukersMeldekort1)

            val sak1meldeperiode2 = sak1.meldeperiodeKjeder[1].first()
            val sak1brukersMeldekort2 = lagBrukersMeldekort(sak1meldeperiode2, Unit.right())
            meldekortBrukerRepo.lagre(sak1brukersMeldekort2)

            val sak2meldeperiode1 = sak2.meldeperiodeKjeder[0].first()
            val sak2brukersMeldekort1 = lagBrukersMeldekort(sak2meldeperiode1, Unit.right())
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

            val meldekort1 = lagBrukersMeldekort(sak.meldeperiodeKjeder[0].first(), Unit.right())
                .copy(behandletAutomatiskStatus = BrukersMeldekortBehandletAutomatiskStatus.BEHANDLET)
            meldekortBrukerRepo.lagre(meldekort1)

            val meldekort2 = lagBrukersMeldekort(sak.meldeperiodeKjeder[1].first(), Unit.right())
            meldekortBrukerRepo.lagre(meldekort2)

            meldekortBrukerRepo.hentMeldekortSomSkalBehandlesAutomatisk() shouldBe listOf(meldekort2)
        }
    }
}
