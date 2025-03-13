package no.nav.tiltakspenger.saksbehandling.repository.meldekort

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
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
