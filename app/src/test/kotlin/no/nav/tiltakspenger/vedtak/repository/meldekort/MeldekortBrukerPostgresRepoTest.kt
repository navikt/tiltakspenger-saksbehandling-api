package no.nav.tiltakspenger.vedtak.repository.meldekort

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.meldekort.domene.opprettFørsteMeldeperiode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.vedtak.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.vedtak.db.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class MeldekortBrukerPostgresRepoTest {
    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak, _) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )
            val meldeperiode = sak.opprettFørsteMeldeperiode()
            testDataHelper.meldeperiodeRepo.lagre(meldeperiode)
            val meldekortBrukerRepo = testDataHelper.meldekortBrukerRepo
            val brukersMeldekort = ObjectMother.brukersMeldekort(
                meldeperiode = meldeperiode,
                mottatt = meldeperiode.opprettet.plus(1, ChronoUnit.MILLIS),
                sakId = meldeperiode.sakId,
            )
            meldekortBrukerRepo.lagre(brukersMeldekort)

            meldekortBrukerRepo.hentForSakId(meldeperiode.sakId) shouldBe listOf(brukersMeldekort)
        }
    }
}
