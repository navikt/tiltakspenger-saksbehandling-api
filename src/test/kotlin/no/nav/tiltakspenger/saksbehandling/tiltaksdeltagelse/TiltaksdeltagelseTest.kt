package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse

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

class TiltaksdeltagelseTest {
    @Test
    fun `overlapperMedPeriode - begge datoene mangler - returnerer null`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(null, null)

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe null
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe null
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe null
    }

    @Test
    fun `overlapperMedPeriode - fom mangler, tom er før perioden - returnerer false`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(null, 3.desember(2024))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe false
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe false
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe false
    }

    @Test
    fun `overlapperMedPeriode - fom mangler, tom er i perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(null, 3.mai(2025))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe true
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - fom mangler, tom er etter perioden - returnerer null`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(null, 3.mai(2026))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe null
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe null
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe null
    }

    @Test
    fun `overlapperMedPeriode - tom mangler, fom er før perioden - returnerer null`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.desember(2024), null)

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe null
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe null
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe null
    }

    @Test
    fun `overlapperMedPeriode - tom mangler, fom er i perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.mai(2025), null)

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe true
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - tom mangler, fom er etter perioden - returnerer false`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.mai(2026), null)

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe false
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe false
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe false
    }

    @Test
    fun `overlapperMedPeriode - fom og tom er før perioden - returnerer false`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.februar(2024), 1.juni(2024))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe false
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe false
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe false
    }

    @Test
    fun `overlapperMedPeriode - fom og tom er etter perioden - returnerer false`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.februar(2026), 1.juni(2026))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe false
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe false
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe false
    }

    @Test
    fun `overlapperMedPeriode - fom og tom er innenfor perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.februar(2025), 1.juni(2025))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe true
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - fom er før, tom er innenfor perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.februar(2024), 1.juni(2025))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe true
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - tom er etter, fom er innenfor perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.februar(2025), 1.juni(2026))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe true
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe true
    }

    @Test
    fun `overlapperMedPeriode - fom er før, tom er etter perioden - returnerer true`() {
        val periode = Periode(1.januar(2025), 1.oktober(2025))
        val tiltaksdeltagelse = getTiltaksdeltagelse(3.februar(2024), 1.juni(2026))

        tiltaksdeltagelse.overlapperMedPeriode(periode) shouldBe true
        tiltaksdeltagelse.overlapperMed(getTiltaksdeltagelse(periode)) shouldBe true
        getTiltaksdeltagelse(periode).overlapperMed(tiltaksdeltagelse) shouldBe true
    }

    private fun getTiltaksdeltagelse(periode: Periode): Tiltaksdeltagelse {
        return getTiltaksdeltagelse(periode.fraOgMed, periode.tilOgMed)
    }

    private fun getTiltaksdeltagelse(fom: LocalDate?, tom: LocalDate?): Tiltaksdeltagelse {
        return Tiltaksdeltagelse(
            eksternDeltagelseId = UUID.randomUUID().toString(),
            gjennomføringId = UUID.randomUUID().toString(),
            typeNavn = "Avklaring",
            typeKode = TiltakstypeSomGirRett.AVKLARING,
            rettPåTiltakspenger = true,
            deltagelseFraOgMed = fom,
            deltagelseTilOgMed = tom,
            deltakelseStatus = TiltakDeltakerstatus.Deltar,
            deltakelseProsent = 50.0F,
            antallDagerPerUke = 2.0F,
            kilde = Tiltakskilde.Komet,
        )
    }
}
