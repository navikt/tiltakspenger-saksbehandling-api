package no.nav.tiltakspenger.saksbehandling.benk.domene

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Sorteringen kommer fra en url brukeren kan redigere, så parsingen skal aldri kaste.
 * Testen pinner nettopp fallbackene, siden det er de som holder benken oppe når url-en er tullete.
 */
class BenkSorteringTest {

    private fun sorter(streng: String?) = streng.tilSortering(
        kolonner = BenkSøknaderKolonne.entries,
        default = BenkSøknaderKolonne.KRAVTIDSPUNKT,
    )

    @Test
    fun `parser kolonne og retning`() {
        sorter("sist_endret,DESC") shouldBe
            BenkSortering(BenkSøknaderKolonne.SIST_ENDRET, BenkSorteringRetning.DESC)
    }

    @Test
    fun `parsingen er case-insensitiv`() {
        sorter("SIST_ENDRET,desc") shouldBe
            BenkSortering(BenkSøknaderKolonne.SIST_ENDRET, BenkSorteringRetning.DESC)
    }

    @Test
    fun `kolonne med æøå parses`() {
        sorter("søknadstype,ASC") shouldBe
            BenkSortering(BenkSøknaderKolonne.SØKNADSTYPE, BenkSorteringRetning.ASC)
    }

    @Test
    fun `ukjent kolonne faller tilbake på default`() {
        sorter("drop_table,DESC") shouldBe
            BenkSortering(BenkSøknaderKolonne.KRAVTIDSPUNKT, BenkSorteringRetning.DESC)
    }

    @Test
    fun `ukjent eller manglende retning faller tilbake på stigende`() {
        sorter("fnr,tull") shouldBe BenkSortering(BenkSøknaderKolonne.FNR, BenkSorteringRetning.ASC)
        sorter("fnr") shouldBe BenkSortering(BenkSøknaderKolonne.FNR, BenkSorteringRetning.ASC)
    }

    @Test
    fun `null og tom streng gir default`() {
        val forventet = BenkSortering(BenkSøknaderKolonne.KRAVTIDSPUNKT, BenkSorteringRetning.ASC)
        sorter(null) shouldBe forventet
        sorter("") shouldBe forventet
    }

    @Test
    fun `en fanes kolonne gjelder ikke for en annen fane`() {
        // «beløp» finnes på meldekortfanen, men ikke på søknadsfanen, og skal derfor ikke slippe gjennom der.
        sorter("beløp,ASC").kolonne shouldBe BenkSøknaderKolonne.KRAVTIDSPUNKT
        "beløp,ASC".tilSortering(
            kolonner = BenkMeldekortKolonne.entries,
            default = BenkMeldekortKolonne.PERIODE,
        ).kolonne shouldBe BenkMeldekortKolonne.BELØP
    }
}
