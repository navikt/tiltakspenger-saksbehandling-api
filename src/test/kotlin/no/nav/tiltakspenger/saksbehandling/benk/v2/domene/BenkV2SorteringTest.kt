package no.nav.tiltakspenger.saksbehandling.benk.v2.domene

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Sorteringen kommer fra en url brukeren kan redigere, så parsingen skal aldri kaste.
 * Testen pinner nettopp fallbackene, siden det er de som holder benken oppe når url-en er tullete.
 */
class BenkV2SorteringTest {

    private fun sorter(streng: String?) = streng.tilSortering(
        kolonner = BenkSøknaderKolonne.entries,
        default = BenkSøknaderKolonne.KRAVTIDSPUNKT,
    )

    @Test
    fun `parser kolonne og retning`() {
        sorter("sist_endret,DESC") shouldBe
            BenkV2Sortering(BenkSøknaderKolonne.SIST_ENDRET, BenkV2SorteringRetning.DESC)
    }

    @Test
    fun `parsingen er case-insensitiv`() {
        sorter("SIST_ENDRET,desc") shouldBe
            BenkV2Sortering(BenkSøknaderKolonne.SIST_ENDRET, BenkV2SorteringRetning.DESC)
    }

    @Test
    fun `kolonne med æøå parses`() {
        sorter("søknadstype,ASC") shouldBe
            BenkV2Sortering(BenkSøknaderKolonne.SØKNADSTYPE, BenkV2SorteringRetning.ASC)
    }

    @Test
    fun `ukjent kolonne faller tilbake på default`() {
        sorter("drop_table,DESC") shouldBe
            BenkV2Sortering(BenkSøknaderKolonne.KRAVTIDSPUNKT, BenkV2SorteringRetning.DESC)
    }

    @Test
    fun `ukjent eller manglende retning faller tilbake på stigende`() {
        sorter("fnr,tull") shouldBe BenkV2Sortering(BenkSøknaderKolonne.FNR, BenkV2SorteringRetning.ASC)
        sorter("fnr") shouldBe BenkV2Sortering(BenkSøknaderKolonne.FNR, BenkV2SorteringRetning.ASC)
    }

    @Test
    fun `null og tom streng gir default`() {
        val forventet = BenkV2Sortering(BenkSøknaderKolonne.KRAVTIDSPUNKT, BenkV2SorteringRetning.ASC)
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
