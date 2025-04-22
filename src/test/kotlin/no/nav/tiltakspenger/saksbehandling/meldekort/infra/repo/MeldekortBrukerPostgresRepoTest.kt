package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periodisering.januar
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
                    behandletStatus = null,
                ),
            )
        }
    }
}
