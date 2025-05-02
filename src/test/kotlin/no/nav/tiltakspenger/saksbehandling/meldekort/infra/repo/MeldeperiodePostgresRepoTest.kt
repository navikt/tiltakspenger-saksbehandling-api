package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterIverksattFørstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import org.junit.jupiter.api.Test

class MeldeperiodePostgresRepoTest {
    @Test
    fun `kan lagre og hente`() {
        withMigratedDb { testDataHelper ->
            val (sak, rammevedtak) = testDataHelper.persisterIverksattFørstegangsbehandling(
                deltakelseFom = 2.januar(2024),
                deltakelseTom = 31.mars(2024),
            )

            val meldeperiodeRepo = testDataHelper.meldeperiodeRepo
            val meldeperiodekjeder = meldeperiodeRepo.hentForSakId(sak.id)

            meldeperiodekjeder shouldBe sak.meldeperiodeKjeder
            meldeperiodekjeder.size shouldBe 7

            val forsteRelaterteVedtak = meldeperiodekjeder.meldeperioder.first().rammevedtak
            forsteRelaterteVedtak?.size shouldBe 2
            forsteRelaterteVedtak?.firstOrNull()?.periode shouldBe Periode(1.januar(2024), 1.januar(2024))
            forsteRelaterteVedtak?.firstOrNull()?.verdi shouldBe null
            forsteRelaterteVedtak?.lastOrNull()?.periode shouldBe Periode(2.januar(2024), meldeperiodekjeder.first().periode.tilOgMed)
            forsteRelaterteVedtak?.lastOrNull()?.verdi shouldBe rammevedtak.id

            val andreRelaterteVedtak = meldeperiodekjeder.meldeperioder[1].rammevedtak
            andreRelaterteVedtak?.size shouldBe 1
            andreRelaterteVedtak?.firstOrNull()?.verdi shouldBe rammevedtak.id
            andreRelaterteVedtak?.firstOrNull()?.periode shouldBe meldeperiodekjeder.meldeperioder[1].periode

            val sisteRelaterteVedtak = meldeperiodekjeder.meldeperioder.last().rammevedtak
            sisteRelaterteVedtak?.size shouldBe 2
            sisteRelaterteVedtak?.firstOrNull()?.verdi shouldBe rammevedtak.id
            sisteRelaterteVedtak?.firstOrNull()?.periode shouldBe Periode(meldeperiodekjeder.meldeperioder.last().periode.fraOgMed, rammevedtak.tilOgMed)
            sisteRelaterteVedtak?.lastOrNull()?.verdi shouldBe null
            sisteRelaterteVedtak?.lastOrNull()?.periode shouldBe Periode(rammevedtak.tilOgMed.plusDays(1), meldeperiodekjeder.meldeperioder.last().periode.tilOgMed)
        }
    }
}
