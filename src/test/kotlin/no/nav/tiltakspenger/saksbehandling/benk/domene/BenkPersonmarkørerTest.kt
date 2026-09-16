package no.nav.tiltakspenger.saksbehandling.benk.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Markørene utledes av regelen Tilgangsmaskinen avviste på, og ingenting annet.
 * Benken gjør ingen oppslag mot PDL eller skjermingsregisteret for å fylle dem.
 */
class BenkPersonmarkørerTest {

    @Test
    fun `strengt fortrolig gir kode 6`() {
        markører(TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG) shouldBe BenkPersonmarkører(
            skjermet = false,
            kode6 = true,
            kode7 = false,
        )
    }

    @Test
    fun `strengt fortrolig utland gir også kode 6`() {
        markører(TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG_UTLAND) shouldBe BenkPersonmarkører(
            skjermet = false,
            kode6 = true,
            kode7 = false,
        )
    }

    @Test
    fun `fortrolig gir kode 7`() {
        markører(TilgangsvurderingAvvistÅrsak.FORTROLIG) shouldBe BenkPersonmarkører(
            skjermet = false,
            kode6 = false,
            kode7 = true,
        )
    }

    @Test
    fun `skjerming gir skjermet`() {
        markører(TilgangsvurderingAvvistÅrsak.SKJERMET) shouldBe BenkPersonmarkører(
            skjermet = true,
            kode6 = false,
            kode7 = false,
        )
    }

    /**
     * De øvrige årsakene sier ingenting om adressebeskyttelse eller skjerming, så raden får ingen markør.
     * UKJENT er med fordi en avvisningskode vi ikke kjenner, heller ikke skal gi en markør.
     */
    @ParameterizedTest
    @EnumSource(
        value = TilgangsvurderingAvvistÅrsak::class,
        names = ["HABILITET", "VERGE", "GEOGRAFISK", "UKJENT_BOSTED", "PERSON_UTLAND", "AVDØD", "UKJENT"],
    )
    fun `de øvrige årsakene gir ingen markører`(årsak: TilgangsvurderingAvvistÅrsak) {
        markører(årsak) shouldBe ingenMarkører
    }

    @Test
    fun `godkjent tilgang gir ingen markører`() {
        BenkPersonmarkører.fra(TilgangsvurderingBulk.Godkjent) shouldBe ingenMarkører
    }

    private val ingenMarkører = BenkPersonmarkører(skjermet = false, kode6 = false, kode7 = false)

    private fun markører(årsak: TilgangsvurderingAvvistÅrsak): BenkPersonmarkører = BenkPersonmarkører.fra(
        TilgangsvurderingBulk.Avvist(
            årsak = årsak,
            begrunnelse = "Du har ikke tilgang",
        ),
    )
}
