package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.periode.Overlapp
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class TiltaksdeltakelseTest {
    @Test
    fun `overlapperMed - begge datoene mangler - returnerer kanskje`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(null, null)

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Kanskje
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Kanskje
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Kanskje
    }

    @Test
    fun `overlapperMed - fom mangler, tom er før perioden - returnerer nei`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(null, 3.desember(2024))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Nei
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Nei
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Nei
    }

    @Test
    fun `overlapperMed - fom mangler, tom er i perioden - returnerer ja`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(null, 3.mai(2025))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Ja
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Ja
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Ja
    }

    @Test
    fun `overlapperMed - fom mangler, tom er etter perioden - returnerer kanskje`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(null, 3.mai(2026))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Kanskje
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Kanskje
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Kanskje
    }

    @Test
    fun `overlapperMed - tom mangler, fom er før perioden - returnerer kanskje`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.desember(2024), null)

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Kanskje
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Kanskje
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Kanskje
    }

    @Test
    fun `overlapperMed - tom mangler, fom er i perioden - returnerer ja`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.mai(2025), null)

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Ja
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Ja
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Ja
    }

    @Test
    fun `overlapperMed - tom mangler, fom er etter perioden - returnerer nei`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.mai(2026), null)

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Nei
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Nei
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Nei
    }

    @Test
    fun `overlapperMed - fom og tom er før perioden - returnerer nei`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2024), 1.juni(2024))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Nei
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Nei
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Nei
    }

    @Test
    fun `overlapperMed - fom og tom er etter perioden - returnerer nei`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2026), 1.juni(2026))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Nei
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Nei
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Nei
    }

    @Test
    fun `overlapperMed - fom og tom er innenfor perioden - returnerer ja`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2025), 1.juni(2025))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Ja
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Ja
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Ja
    }

    @Test
    fun `overlapperMed - fom er før, tom er innenfor perioden - returnerer ja`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2024), 1.juni(2025))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Ja
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Ja
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Ja
    }

    @Test
    fun `overlapperMed - tom er etter, fom er innenfor perioden - returnerer ja`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2025), 1.juni(2026))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Ja
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Ja
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Ja
    }

    @Test
    fun `overlapperMed - fom er før, tom er etter perioden - returnerer ja`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2024), 1.juni(2026))

        tiltaksdeltakelse.overlapperMed(periode) shouldBe Overlapp.Ja
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe Overlapp.Ja
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe Overlapp.Ja
    }

    private fun getTiltaksdeltakelse(periode: Periode): TiltaksdeltakelseIntern {
        return getTiltaksdeltakelse(periode.fraOgMed, periode.tilOgMed)
    }

    private fun getTiltaksdeltakelse(fom: LocalDate?, tom: LocalDate?): TiltaksdeltakelseIntern {
        return TiltaksdeltakelseIntern(
            eksternDeltakelseId = UUID.randomUUID().toString(),
            gjennomføringId = UUID.randomUUID().toString(),
            typeNavn = "Avklaring",
            typeKode = TiltakstypeSomGirRettDTO.AVKLARING,
            rettPåTiltakspenger = true,
            deltakelseFraOgMed = fom,
            deltakelseTilOgMed = tom,
            deltakelseStatus = TiltakDeltakerstatus.Deltar,
            deltakelseProsent = 50.0F,
            antallDagerPerUke = 2.0F,
            kilde = Tiltakskilde.Komet,
            deltidsprosentGjennomforing = 100.0,
            internDeltakelseId = TiltaksdeltakerId.random(),
        )
    }
}
