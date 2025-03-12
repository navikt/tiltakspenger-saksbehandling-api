package no.nav.tiltakspenger.vedtak.repository.meldekort

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import org.junit.jupiter.api.Test

class MeldeperiodePostgresRepoTest {
    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )
            val (_, meldeperioder) = sak.meldeperiodeKjeder.genererMeldeperioder(sak.vedtaksliste)
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            meldeperiodeRepo.lagre(meldeperioder.first())

            meldeperiodeRepo.hentForSakId(meldeperioder.first().sakId) shouldBe MeldeperiodeKjeder(
                listOf(MeldeperiodeKjede(nonEmptyListOf(meldeperioder.first()))),
            )
        }
    }
}
