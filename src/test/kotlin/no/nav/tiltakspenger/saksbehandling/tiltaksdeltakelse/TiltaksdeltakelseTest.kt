package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class TiltaksdeltakelseTest {
    @Test
    fun `overlapperMedPeriode - begge datoene mangler - returnerer null`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(null, null)

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe null
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe null
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe null
    }

    @Test
    fun `overlapperMedPeriode - fom mangler, tom er før perioden - returnerer false`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(null, 3.desember(2024))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe false
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe false
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe false
    }

    @Test
    fun `overlapperMedPeriode - fom mangler, tom er i perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(null, 3.mai(2025))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe true
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - fom mangler, tom er etter perioden - returnerer null`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(null, 3.mai(2026))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe null
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe null
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe null
    }

    @Test
    fun `overlapperMedPeriode - tom mangler, fom er før perioden - returnerer null`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.desember(2024), null)

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe null
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe null
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe null
    }

    @Test
    fun `overlapperMedPeriode - tom mangler, fom er i perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.mai(2025), null)

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe true
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - tom mangler, fom er etter perioden - returnerer false`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.mai(2026), null)

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe false
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe false
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe false
    }

    @Test
    fun `overlapperMedPeriode - fom og tom er før perioden - returnerer false`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2024), 1.juni(2024))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe false
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe false
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe false
    }

    @Test
    fun `overlapperMedPeriode - fom og tom er etter perioden - returnerer false`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2026), 1.juni(2026))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe false
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe false
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe false
    }

    @Test
    fun `overlapperMedPeriode - fom og tom er innenfor perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2025), 1.juni(2025))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe true
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - fom er før, tom er innenfor perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2024), 1.juni(2025))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe true
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - tom er etter, fom er innenfor perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2025), 1.juni(2026))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe true
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - fom er før, tom er etter perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltakelse = getTiltaksdeltakelse(3.februar(2024), 1.juni(2026))

        tiltaksdeltakelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltakelse.overlapperMed(getTiltaksdeltakelse(periode)) shouldBe true
        getTiltaksdeltakelse(periode).overlapperMed(tiltaksdeltakelse) shouldBe true
    }

    private fun getTiltaksdeltakelse(periode: Periode): Tiltaksdeltakelse {
        return getTiltaksdeltakelse(periode.fraOgMed, periode.tilOgMed)
    }

    private fun getTiltaksdeltakelse(fom: LocalDate?, tom: LocalDate?): Tiltaksdeltakelse {
        return Tiltaksdeltakelse(
            eksternDeltakelseId = UUID.randomUUID().toString(),
            gjennomføringId = UUID.randomUUID().toString(),
            typeNavn = "Avklaring",
            typeKode = TiltakstypeSomGirRett.AVKLARING,
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
