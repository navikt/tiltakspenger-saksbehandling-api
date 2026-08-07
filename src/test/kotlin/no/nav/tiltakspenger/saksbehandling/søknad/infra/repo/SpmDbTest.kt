package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Mappingen rører ikke postgres — den oversetter bare mellom spørsmålstypen og strengen i `<navn>_type`-kolonnen.
 * Å nå «ugyldig type»-grenen gjennom en prodsti er dessuten umulig: den finnes for korrupt eller historisk data i basen.
 *
 * Testen pinner **de faktiske strengene**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om en variant ble omdøpt i begge `when`-ene samtidig — og da er søknadene som allerede ligger i basen ulesbare uten at noe slår ut.
 */
class SpmDbTest {

    private val fraOgMed = LocalDate.of(2025, 1, 1)
    private val tilOgMed = LocalDate.of(2025, 1, 31)

    @Test
    fun `periodespørsmål lagres med sitt avtalte navn`() {
        lagrePeriodeSpmType(Søknad.PeriodeSpm.Ja(fraOgMed, tilOgMed)) shouldBe "JA"
        lagrePeriodeSpmType(Søknad.PeriodeSpm.Nei) shouldBe "NEI"
        lagrePeriodeSpmType(Søknad.PeriodeSpm.IkkeBesvart) shouldBe "IKKE_BESVART"
    }

    @Test
    fun `periodespørsmål leses tilbake fra lagret verdi`() {
        tilPeriodeSpm("JA", fraOgMed, tilOgMed) shouldBe Søknad.PeriodeSpm.Ja(fraOgMed, tilOgMed)
        tilPeriodeSpm("NEI", null, null) shouldBe Søknad.PeriodeSpm.Nei
        tilPeriodeSpm("IKKE_BESVART", null, null) shouldBe Søknad.PeriodeSpm.IkkeBesvart
    }

    @Test
    fun `ukjent type på periodespørsmål gir feil`() {
        shouldThrowWithMessage<IllegalArgumentException>("Ugyldig type KANSKJE") {
            tilPeriodeSpm("KANSKJE", null, null)
        }
    }

    @Test
    fun `fra og med-spørsmål lagres med sitt avtalte navn`() {
        lagreFraOgMedDatoSpmType(Søknad.FraOgMedDatoSpm.Ja(fraOgMed)) shouldBe "JA"
        lagreFraOgMedDatoSpmType(Søknad.FraOgMedDatoSpm.Nei) shouldBe "NEI"
        lagreFraOgMedDatoSpmType(Søknad.FraOgMedDatoSpm.IkkeBesvart) shouldBe "IKKE_BESVART"
    }

    @Test
    fun `fra og med-spørsmål leses tilbake fra lagret verdi`() {
        tilFraOgMedDatoSpm("JA", fraOgMed) shouldBe Søknad.FraOgMedDatoSpm.Ja(fraOgMed)
        tilFraOgMedDatoSpm("NEI", null) shouldBe Søknad.FraOgMedDatoSpm.Nei
        tilFraOgMedDatoSpm("IKKE_BESVART", null) shouldBe Søknad.FraOgMedDatoSpm.IkkeBesvart
    }

    @Test
    fun `ukjent type på fra og med-spørsmål gir feil`() {
        shouldThrowWithMessage<IllegalArgumentException>("Ugyldig type null") {
            tilFraOgMedDatoSpm(null, null)
        }
    }

    @Test
    fun `ja-nei-spørsmål lagres med sitt avtalte navn`() {
        lagreJaNeiSpmType(Søknad.JaNeiSpm.Ja) shouldBe "JA"
        lagreJaNeiSpmType(Søknad.JaNeiSpm.Nei) shouldBe "NEI"
        lagreJaNeiSpmType(Søknad.JaNeiSpm.IkkeBesvart) shouldBe "IKKE_BESVART"
    }

    @Test
    fun `ja-nei-spørsmål leses tilbake fra lagret verdi`() {
        tilJaNeiSpm("JA") shouldBe Søknad.JaNeiSpm.Ja
        tilJaNeiSpm("NEI") shouldBe Søknad.JaNeiSpm.Nei
        tilJaNeiSpm("IKKE_BESVART") shouldBe Søknad.JaNeiSpm.IkkeBesvart
    }

    @Test
    fun `ukjent type på ja-nei-spørsmål gir feil`() {
        shouldThrowWithMessage<IllegalArgumentException>("Ugyldig type TULL") {
            tilJaNeiSpm("TULL")
        }
    }

    @Test
    fun `periodespørsmål blir til tre kolonneverdier`() {
        mapOf("kvp" to Søknad.PeriodeSpm.Ja(fraOgMed, tilOgMed)).toPeriodeSpmParams() shouldBe
            mapOf(
                "kvp_type" to "JA",
                "kvp_ja" to true,
                "kvp_periode" to "(2025-01-01,2025-01-31)",
            )
        mapOf("kvp" to Søknad.PeriodeSpm.Nei).toPeriodeSpmParams() shouldBe
            mapOf(
                "kvp_type" to "NEI",
                "kvp_ja" to false,
                "kvp_periode" to null,
            )
    }

    @Test
    fun `fra og med-spørsmål blir til tre kolonneverdier`() {
        mapOf("alderspensjon" to Søknad.FraOgMedDatoSpm.Ja(fraOgMed)).toFraOgMedDatoSpmParams() shouldBe
            mapOf(
                "alderspensjon_type" to "JA",
                "alderspensjon_ja" to true,
                "alderspensjon_fom" to fraOgMed,
            )
        mapOf("alderspensjon" to Søknad.FraOgMedDatoSpm.IkkeBesvart).toFraOgMedDatoSpmParams() shouldBe
            mapOf(
                "alderspensjon_type" to "IKKE_BESVART",
                "alderspensjon_ja" to false,
                "alderspensjon_fom" to null,
            )
    }

    @Test
    fun `ja-nei-spørsmål blir til én kolonneverdi`() {
        mapOf("etterlønn" to Søknad.JaNeiSpm.Ja).toJaNeiSpmParams() shouldBe mapOf("etterlønn_type" to "JA")
    }
}
