package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.createOrThrow
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Begrunnelsen er valgfri begge veier, og en rad kan i tillegg inneholde en blank streng ingen skrivesti lenger produserer.
 * Mappingen rører ikke postgres — den er ren json.
 *
 * Testen pinner **den faktiske json-en**, ikke bare rundturen, jf. mønsteret i [HjemmelForOpphørDbTest].
 */
class BarnetilleggDbJsonTest {

    private val periode = Periode(6.januar(2025), 19.januar(2025))

    private fun barnetillegg(begrunnelse: Begrunnelse?) = Barnetillegg(
        periodisering = listOf(PeriodeMedVerdi(periode = periode, verdi = AntallBarn(1))).tilIkkeTomPeriodisering(),
        begrunnelse = begrunnelse,
    )

    @Test
    fun `begrunnelsen lagres som klartekst i jsonen`() {
        barnetillegg(Begrunnelse.createOrThrow("To barn i perioden")).toDbJson() shouldBe
            """{"value":[{"periode":{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-19"},"verdi":1}],"begrunnelse":"To barn i perioden"}"""
    }

    @Test
    fun `uten begrunnelse lagres null`() {
        barnetillegg(begrunnelse = null).toDbJson() shouldBe
            """{"value":[{"periode":{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-19"},"verdi":1}],"begrunnelse":null}"""
    }

    @Test
    fun `begrunnelsen leses tilbake fra lagret json`() {
        """{"value":[{"periode":{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-19"},"verdi":1}],"begrunnelse":"To barn i perioden"}"""
            .toBarnetillegg().begrunnelse?.verdi shouldBe "To barn i perioden"
    }

    @Test
    fun `lagret null-begrunnelse leses som null`() {
        """{"value":[{"periode":{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-19"},"verdi":1}],"begrunnelse":null}"""
            .toBarnetillegg().begrunnelse shouldBe null
    }

    /** Skrivestien kan ikke produsere en blank begrunnelse (`Begrunnelse.create` gir null), så en blank streng i raden leses som fravær i stedet for å kaste. */
    @Test
    fun `en blank lagret begrunnelse leses som null`() {
        """{"value":[{"periode":{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-19"},"verdi":1}],"begrunnelse":"   "}"""
            .toBarnetillegg().begrunnelse shouldBe null
    }
}
