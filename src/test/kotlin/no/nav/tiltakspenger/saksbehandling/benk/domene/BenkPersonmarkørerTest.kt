package no.nav.tiltakspenger.saksbehandling.benk.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import no.nav.tiltakspenger.saksbehandling.person.AdressebeskyttelseOgSkjerming
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * For rader uten tilgang utledes markørene av regelen Tilgangsmaskinen avviste på, og ingenting annet.
 * For rader med tilgang kommer de fra oppslaget i PDL og skjermingsregisteret, når det er gjort.
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
    fun `godkjent tilgang uten oppslag gir ingen markører`() {
        BenkPersonmarkører.fra(TilgangsvurderingBulk.Godkjent) shouldBe ingenMarkører
    }

    @Test
    fun `godkjent tilgang får markørene fra oppslaget, også flere samtidig`() {
        BenkPersonmarkører.fra(
            TilgangsvurderingBulk.Godkjent,
            AdressebeskyttelseOgSkjerming(Adressebeskyttelse.STRENGT_FORTROLIG, skjermet = true),
        ) shouldBe BenkPersonmarkører(skjermet = true, kode6 = true, kode7 = false)
        BenkPersonmarkører.fra(
            TilgangsvurderingBulk.Godkjent,
            AdressebeskyttelseOgSkjerming(Adressebeskyttelse.FORTROLIG, skjermet = false),
        ) shouldBe BenkPersonmarkører(skjermet = false, kode6 = false, kode7 = true)
        BenkPersonmarkører.fra(
            TilgangsvurderingBulk.Godkjent,
            AdressebeskyttelseOgSkjerming(Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND, skjermet = false),
        ) shouldBe BenkPersonmarkører(skjermet = false, kode6 = true, kode7 = false)
    }

    @Test
    fun `avvist tilgang bruker avvisningen, ikke oppslaget`() {
        BenkPersonmarkører.fra(
            TilgangsvurderingBulk.Avvist(årsak = TilgangsvurderingAvvistÅrsak.SKJERMET, begrunnelse = "Du har ikke tilgang"),
            AdressebeskyttelseOgSkjerming(Adressebeskyttelse.STRENGT_FORTROLIG, skjermet = true),
        ) shouldBe BenkPersonmarkører(skjermet = true, kode6 = false, kode7 = false)
    }

    private val ingenMarkører = BenkPersonmarkører(skjermet = false, kode6 = false, kode7 = false)

    private fun markører(årsak: TilgangsvurderingAvvistÅrsak): BenkPersonmarkører = BenkPersonmarkører.fra(
        TilgangsvurderingBulk.Avvist(
            årsak = årsak,
            begrunnelse = "Du har ikke tilgang",
        ),
    )
}
