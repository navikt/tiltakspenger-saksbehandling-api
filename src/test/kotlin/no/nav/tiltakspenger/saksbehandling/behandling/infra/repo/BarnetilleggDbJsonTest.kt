package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.createOrThrow
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
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

    private fun barnetillegg(begrunnelse: Begrunnelse?) = BarnetilleggV1(
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

    @Test
    fun `json uten versjon leses som V1`() {
        """{"value":[{"periode":{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-19"},"verdi":1}],"begrunnelse":null}"""
            .toBarnetillegg() shouldBe barnetillegg(begrunnelse = null)
    }

    @Test
    fun `ukjent versjon kaster`() {
        shouldThrow<IllegalStateException> {
            """{"innvilgelsesperioder":[],"barn":[],"begrunnelse":null,"versjon":"V3"}""".toBarnetillegg()
        }.message shouldBe "Ukjent versjon av barnetillegg: V3"
    }

    @Test
    fun `V2 lagres med versjon og barna, og leses tilbake likt`() {
        val søknadId = SøknadId.fromString("soknad_01KAAAAAAAAAAAAAAAAAAAAAAA")
        val fnr = ObjectMother.gyldigFnr()
        val barnetillegg = BarnetilleggV2(
            barn = listOf(
                BarnMedBarnetillegg(
                    fnr = fnr,
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann",
                    fødselsdato = 1.januar(2020),
                    kilde = BarnMedBarnetillegg.Kilde.Søknad(søknadId),
                    perioder = null,
                ),
                BarnMedBarnetillegg(
                    fnr = null,
                    fornavn = "Kari",
                    mellomnavn = "Mellom",
                    etternavn = "Nordmann",
                    fødselsdato = 1.januar(2021),
                    kilde = BarnMedBarnetillegg.Kilde.Saksbehandler,
                    perioder = nonEmptyListOf(6 til 10.januar(2025), 13 til 17.januar(2025)),
                ),
            ),
            innvilgelsesperioder = nonEmptyListOf(periode),
            begrunnelse = Begrunnelse.createOrThrow("To barn"),
        )

        val json = barnetillegg.toDbJson()

        json shouldBe
            """{"innvilgelsesperioder":[{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-19"}],""" +
            """"barn":[{"fnr":"${fnr.verdi}","fornavn":"Ola","mellomnavn":null,"etternavn":"Nordmann","fødselsdato":"2020-01-01","kilde":"SØKNAD","søknadId":"soknad_01KAAAAAAAAAAAAAAAAAAAAAAA","perioder":null},""" +
            """{"fnr":null,"fornavn":"Kari","mellomnavn":"Mellom","etternavn":"Nordmann","fødselsdato":"2021-01-01","kilde":"SAKSBEHANDLER","søknadId":null,"perioder":[{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-10"},{"fraOgMed":"2025-01-13","tilOgMed":"2025-01-17"}]}],""" +
            """"begrunnelse":"To barn","versjon":"V2"}"""
        json.toBarnetillegg().shouldBeInstanceOf<BarnetilleggV2>() shouldBe barnetillegg
    }

    @Test
    fun `barn lagret med én periode før det kunne velges flere, leses med den perioden`() {
        val json = """{"innvilgelsesperioder":[{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-19"}],""" +
            """"barn":[{"fnr":null,"fornavn":"Kari","mellomnavn":null,"etternavn":"Nordmann","fødselsdato":"2021-01-01","kilde":"SAKSBEHANDLER","søknadId":null,"periode":{"fraOgMed":"2025-01-06","tilOgMed":"2025-01-10"}},""" +
            """{"fnr":null,"fornavn":"Ola","mellomnavn":null,"etternavn":"Nordmann","fødselsdato":"2020-01-01","kilde":"SAKSBEHANDLER","søknadId":null,"periode":null}],""" +
            """"begrunnelse":null,"versjon":"V2"}"""

        json.toBarnetillegg().shouldBeInstanceOf<BarnetilleggV2>().barn.map { it.perioder } shouldBe listOf(nonEmptyListOf(6 til 10.januar(2025)), null)
    }
}
