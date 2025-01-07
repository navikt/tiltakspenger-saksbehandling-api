package no.nav.tiltakspenger.vedtak.repository.meldekort

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.meldekort.domene.v2.MeldeperiodeKjede
import no.nav.tiltakspenger.meldekort.domene.v2.MeldeperiodeKjeder
import no.nav.tiltakspenger.meldekort.domene.v2.opprettFørsteMeldeperiode
import no.nav.tiltakspenger.vedtak.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.vedtak.db.withMigratedDb
import org.junit.jupiter.api.Test

class MeldeperiodePostgresRepoTest {
    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak, _) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )
            val meldeperiode = sak.opprettFørsteMeldeperiode()
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            meldeperiodeRepo.lagre(meldeperiode)

            meldeperiodeRepo.hentForSakId(meldeperiode.sakId) shouldBe MeldeperiodeKjeder(
                listOf(MeldeperiodeKjede(nonEmptyListOf(meldeperiode))),
            )
        }
    }
}
