package no.nav.tiltakspenger.saksbehandling.repository.meldekort

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.db.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.db.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettNesteMeldeperiode
import org.junit.jupiter.api.Test

class MeldeperiodePostgresRepoTest {
    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak, _) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 1.januar(2024),
                deltakelseTom = 31.mars(2024),
            )

            val førsteMeldeperiode = sak.meldeperiodeKjeder.hentSisteMeldeperiode()
            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val nesteMeldeperiode = sak.opprettNesteMeldeperiode()!!
            meldeperiodeRepo.lagre(nesteMeldeperiode)

            meldeperiodeRepo.hentForSakId(førsteMeldeperiode.sakId) shouldBe MeldeperiodeKjeder(
                listOf(
                    MeldeperiodeKjede(nonEmptyListOf(førsteMeldeperiode)),
                    MeldeperiodeKjede(nonEmptyListOf(nesteMeldeperiode)),
                ),
            )
        }
    }
}
