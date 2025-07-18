package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import org.junit.jupiter.api.Test

class MeldeperiodePostgresRepoTest {
    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak, rammevedtak) = testDataHelper.persisterIverksattSøknadsbehandling(
                deltakelseFom = 2.januar(2024),
                deltakelseTom = 31.mars(2024),
            )

            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldeperiodekjeder = meldeperiodeRepo.hentForSakId(sak.id)

            meldeperiodekjeder shouldBe sak.meldeperiodeKjeder
            meldeperiodekjeder.size shouldBe 7

            val forsteRelaterteVedtak = meldeperiodekjeder.sisteMeldeperiodePerKjede.first().rammevedtak!!
            forsteRelaterteVedtak.size shouldBe 1
            forsteRelaterteVedtak.lastOrNull()?.periode shouldBe Periode(2.januar(2024), meldeperiodekjeder.first().periode.tilOgMed)
            forsteRelaterteVedtak.lastOrNull()?.verdi shouldBe rammevedtak.id

            val andreRelaterteVedtak = meldeperiodekjeder.sisteMeldeperiodePerKjede[1].rammevedtak
            andreRelaterteVedtak?.size shouldBe 1
            andreRelaterteVedtak?.firstOrNull()?.verdi shouldBe rammevedtak.id
            andreRelaterteVedtak?.firstOrNull()?.periode shouldBe meldeperiodekjeder.sisteMeldeperiodePerKjede[1].periode

            val sisteRelaterteVedtak = meldeperiodekjeder.sisteMeldeperiodePerKjede.last().rammevedtak!!
            sisteRelaterteVedtak.size shouldBe 1
            sisteRelaterteVedtak.firstOrNull()?.verdi shouldBe rammevedtak.id
            sisteRelaterteVedtak.firstOrNull()?.periode shouldBe Periode(meldeperiodekjeder.sisteMeldeperiodePerKjede.last().periode.fraOgMed, rammevedtak.tilOgMed)
        }
    }
}
