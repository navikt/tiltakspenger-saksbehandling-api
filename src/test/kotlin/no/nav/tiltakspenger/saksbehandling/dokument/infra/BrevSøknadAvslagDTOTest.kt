package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrThrow
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.time.LocalDate

class BrevSøknadAvslagDTOTest {

    @Test
    fun `genererer og serialiserer brevdata for pdf`() {
        runTest {
            val fnr = Fnr.random()
            val actual = genererAvslagSøknadsbrev(
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("genererer og serialiserer brevdata for pdf test"),
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
                fnr = fnr,
                saksbehandlerNavIdent = "SaksbehandlerNavIdent",
                beslutterNavIdent = null,
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = true,
                harSøktBarnetillegg = true,
                avslagsperiode = ObjectMother.vedtaksperiode(),
                datoForUtsending = LocalDate.now(fixedClock),
            )

            //language=json
            val expected = """
                {
                  "personalia":{
                    "ident":"${fnr.verdi}",
                    "fornavn":"Fornavn",
                    "etternavn":"Etternavn"
                  },
                  "saksnummer":"202501012000",
                  "saksbehandlerNavn":"Saksbehandlernavn",
                  "beslutterNavn":null,
                  "tilleggstekst":"genererer og serialiserer brevdata for pdf test",
                  "avslagsgrunner":["ALDER"],
                  "valgtHjemmelTekst":[
                    "du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få tiltakspenger og barnetillegg.\n\nDette kommer frem av tiltakspengeforskriften § 3."
                  ],
                  "harSøktMedBarn":true,
                  "hjemlerTekst":null,
                  "forhandsvisning":true,
                  "avslagFraOgMed":"1. januar 2023",
                  "avslagTilOgMed":"31. mars 2023",
                  "datoForUtsending": "1. januar 2025"
                  
                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }

    @Test
    fun `genererer og serialiserer brevdata for pdf fra et vedtak`() {
        val (_, avslagsvedtak) = ObjectMother.nySakMedAvslagsvedtak()
        runTest {
            avslagsvedtak.genererAvslagSøknadsbrev(
                hentBrukersNavn = { _: Fnr -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _: String -> "Saksbehandlernavn" },
                datoForUtsending = LocalDate.now(fixedClock),
            ).shouldEqualJson(
                //language=json
                """
                {
                  "personalia":{
                    "ident":"${avslagsvedtak.fnr.verdi}",
                    "fornavn":"Fornavn",
                    "etternavn":"Etternavn"
                  },
                  "saksnummer":"${avslagsvedtak.saksnummer}",
                  "saksbehandlerNavn":"Saksbehandlernavn",
                  "beslutterNavn":"Saksbehandlernavn",
                  "tilleggstekst":"nySakMedAvslagsvedtak",
                  "avslagsgrunner":["ALDER"],
                  "valgtHjemmelTekst":[
                    "du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få tiltakspenger.\n\nDette kommer frem av tiltakspengeforskriften § 3."
                  ],
                  "harSøktMedBarn":false,
                  "hjemlerTekst":null,
                  "forhandsvisning":false,
                  "avslagFraOgMed":"1. januar 2023",
                  "avslagTilOgMed":"31. mars 2023",
                  "datoForUtsending": "1. januar 2025"
                  
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `én avslagsgrunn gir full brevtekst med hjemler, med og uten barnetillegg`() {
        runTest {
            Avslagsgrunnlag.entries.forEach { grunn ->
                listOf(false, true).forEach { barnetillegg ->
                    withClue("avslagsgrunn=$grunn, barnetillegg=$barnetillegg") {
                        val brev = genererBrevJson(nonEmptySetOf(grunn), barnetillegg)
                        brev.valgtHjemmelTekst() shouldBe listOf(grunn.forventetTekst(barnetillegg))
                        brev.get("hjemlerTekst").isNull shouldBe true
                    }
                }
            }
        }
    }

    @Test
    fun `flere avslagsgrunner gir en punkttekst per grunn og samlede hjemler, med og uten barnetillegg`() {
        runTest {
            val alleGrunner = Avslagsgrunnlag.entries.toNonEmptySetOrThrow()
            listOf(false, true).forEach { barnetillegg ->
                withClue("barnetillegg=$barnetillegg") {
                    val brev = genererBrevJson(alleGrunner, barnetillegg)
                    brev.valgtHjemmelTekst() shouldBe alleGrunner.map { it.forventetPunkttekst(barnetillegg) }
                    brev.get("hjemlerTekst").asString() shouldBe alleGrunner.createBrevForskrifter(barnetillegg)
                }
            }
        }
    }

    @Test
    fun `brevdata som testdataene i pdfgenrs speiler`() {
        runTest {
            val brev = genererBrevJson(
                nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak, Avslagsgrunnlag.Livsoppholdytelser),
                harSøktBarnetillegg = true,
            )
            brev.valgtHjemmelTekst() shouldBe listOf(
                "Du deltar ikke på arbeidsmarkedstiltak som gir rett til tiltakspenger.\nFor å få tiltakspenger og barnetillegg må du delta i arbeidsmarkedstiltak som gir rett til tiltakspenger og barnetillegg.",
                "Du mottar en annen pengestøtte til livsopphold.\nDeltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få tiltakspenger og barnetillegg.",
            )
            brev.get("hjemlerTekst").asString() shouldBe
                "Dette kommer frem av arbeidsmarkedsloven § 13 første ledd, og tiltakspengeforskriften §§ 2, 3, 7 første ledd."
        }
    }

    private suspend fun genererBrevJson(
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
        harSøktBarnetillegg: Boolean,
    ): JsonNode = lesTre(
        genererAvslagSøknadsbrev(
            hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
            hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
            tilleggstekst = null,
            avslagsgrunner = avslagsgrunner,
            fnr = Fnr.random(),
            saksbehandlerNavIdent = "SaksbehandlerNavIdent",
            beslutterNavIdent = null,
            saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
            forhåndsvisning = true,
            harSøktBarnetillegg = harSøktBarnetillegg,
            avslagsperiode = ObjectMother.vedtaksperiode(),
            datoForUtsending = LocalDate.now(fixedClock),
        ),
    )

    private fun JsonNode.valgtHjemmelTekst(): List<String> = get("valgtHjemmelTekst").toList().map { it.asString() }

    /**
     * Fasit for brevteksten ved én avslagsgrunn (feltet `valgtHjemmelTekst`), som fullfører malens «Du får ikke … fordi ».
     * Endres en tekst her, må testdataene i tiltakspenger-pdfgenrs (`test/data/vedtakAvslag--en-grunn.json`) oppdateres tilsvarende.
     */
    private fun Avslagsgrunnlag.forventetTekst(medBarnetillegg: Boolean): String {
        val ytelse = if (medBarnetillegg) "tiltakspenger og barnetillegg" else "tiltakspenger"

        return when (this) {
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak ->
                """
                    du ikke deltar på arbeidsmarkedstiltak som gir rett til tiltakspenger.

                    For å få $ytelse må du delta i arbeidsmarkedstiltak som gir rett til $ytelse.

                    Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften ${if (medBarnetillegg) "§§ 2 og 3" else "§ 2"}.
                """.trimIndent()

            Avslagsgrunnlag.Alder ->
                """
                    du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 3.
                """.trimIndent()

            Avslagsgrunnlag.Livsoppholdytelser ->
                """
                    du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få $ytelse.

                    Dette kommer frem av arbeidsmarkedsloven § 13 første ledd og tiltakspengeforskriften § 7 første ledd.
                """.trimIndent()

            Avslagsgrunnlag.Kvalifiseringsprogrammet ->
                """
                    du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
                """.trimIndent()

            Avslagsgrunnlag.Introduksjonsprogrammet ->
                """
                    du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
                """.trimIndent()

            Avslagsgrunnlag.LønnFraTiltaksarrangør ->
                """
                    du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket.

                    Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 8.
                """.trimIndent()

            Avslagsgrunnlag.LønnFraAndre ->
                """
                    du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.

                    Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til $ytelse. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.

                    Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.

                    Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 8 andre ledd.
                """.trimIndent()

            Avslagsgrunnlag.Institusjonsopphold ->
                """
                    du oppholder deg på en institusjon med gratis opphold, mat og drikke.

                    Deltakere som har opphold i institusjon med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket har ikke rett til $ytelse.

                    Det er gjort unntak for opphold i barnevernsinstitusjoner. Dette kommer frem av tiltakspengeforskriften § 9.
                """.trimIndent()

            Avslagsgrunnlag.FremmetForSent ->
                """
                    du har søkt om $ytelse for sent.

                    Tiltakspenger gis for opptil tre måneder før den måneden tiltaksdeltakeren søkte om $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 11.
                """.trimIndent()
        }
    }

    /**
     * Fasit for punkttekstene ved flere avslagsgrunner (feltet `valgtHjemmelTekst`).
     * Endres en tekst her, må testdataene i tiltakspenger-pdfgenrs (`testdata/tpts/vedtakAvslag.json`) oppdateres tilsvarende.
     */
    private fun Avslagsgrunnlag.forventetPunkttekst(medBarnetillegg: Boolean): String {
        val ytelse = if (medBarnetillegg) "tiltakspenger og barnetillegg" else "tiltakspenger"

        return when (this) {
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak ->
                """
                    Du deltar ikke på arbeidsmarkedstiltak som gir rett til tiltakspenger.
                    For å få $ytelse må du delta i arbeidsmarkedstiltak som gir rett til $ytelse.
                """.trimIndent()

            Avslagsgrunnlag.Alder ->
                """
                    Du har ikke fylt 18 år.
                    Du må ha fylt 18 år for å ha rett til å få $ytelse.
                """.trimIndent()

            Avslagsgrunnlag.Livsoppholdytelser ->
                """
                    Du mottar en annen pengestøtte til livsopphold.
                    Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få $ytelse.
                """.trimIndent()

            Avslagsgrunnlag.Kvalifiseringsprogrammet ->
                """
                    Du deltar på kvalifiseringsprogram.
                    Deltakere i kvalifiseringsprogram har ikke rett til $ytelse.
                """.trimIndent()

            Avslagsgrunnlag.Introduksjonsprogrammet ->
                """
                    Du deltar på introduksjonsprogram.
                    Deltakere i introduksjonsprogram har ikke rett til $ytelse.
                """.trimIndent()

            Avslagsgrunnlag.LønnFraTiltaksarrangør ->
                """
                    Du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket.
                    Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket har ikke rett til $ytelse.
                """.trimIndent()

            Avslagsgrunnlag.LønnFraAndre ->
                """
                    Du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.
                    Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til $ytelse. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                    Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
                """.trimIndent()

            Avslagsgrunnlag.Institusjonsopphold ->
                """
                    Du oppholder deg på en institusjon med gratis opphold, mat og drikke.
                    Deltakere som har opphold i institusjon med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket har ikke rett til $ytelse.
                """.trimIndent()

            Avslagsgrunnlag.FremmetForSent ->
                """
                    Du har søkt om $ytelse for sent.
                    Tiltakspenger gis for opptil tre måneder før den måneden tiltaksdeltakeren søkte om $ytelse.
                """.trimIndent()
        }
    }

    @Nested
    inner class CreateBrevForskrifter {
        @Test
        fun `lager forskrifter med bare tiltakspengeforskrifter`() {
            val actual = setOf(Avslagsgrunnlag.Kvalifiseringsprogrammet).createBrevForskrifter(true)
            val expected = """
                Dette kommer frem av tiltakspengeforskriften §§ 3, 7 tredje ledd.
            """.trimIndent()

            actual shouldBe expected
        }

        @Test
        fun `skal ikke gjenta samme paragraf 2 ganger`() {
            val actual = setOf(Avslagsgrunnlag.Kvalifiseringsprogrammet, Avslagsgrunnlag.Introduksjonsprogrammet).createBrevForskrifter(true)
            val expected = """
                Dette kommer frem av tiltakspengeforskriften §§ 3, 7 tredje ledd.
            """.trimIndent()

            actual shouldBe expected
        }

        @Test
        fun `lager forskrifter med tiltakspengerforskrifter og arbeidsmarkedsloven`() {
            val actual = setOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak).createBrevForskrifter(true)
            val expected = """
                Dette kommer frem av arbeidsmarkedsloven § 13, og tiltakspengeforskriften §§ 2, 3.
            """.trimIndent()

            actual shouldBe expected
        }

        @Test
        fun `referer til flere ledd gitt samme paragraf`() {
            val actual = setOf(Avslagsgrunnlag.Kvalifiseringsprogrammet, Avslagsgrunnlag.Livsoppholdytelser).createBrevForskrifter(false)
            val expected = """
                Dette kommer frem av arbeidsmarkedsloven § 13 første ledd, og tiltakspengeforskriften § 7 første og tredje ledd.
            """.trimIndent()

            actual shouldBe expected
        }
    }

    @Test
    fun `mapper Avslagsgrunnlag til AvslagsgrunnerBrevDto`() {
        Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
        Avslagsgrunnlag.Alder.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.ALDER
        Avslagsgrunnlag.Livsoppholdytelser.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.LIVSOPPHOLDYTELSE
        Avslagsgrunnlag.Kvalifiseringsprogrammet.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.KVALIFISERINGSPROGRAMMET
        Avslagsgrunnlag.Introduksjonsprogrammet.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.INTRODUKSJONSPROGRAMMET
        Avslagsgrunnlag.LønnFraTiltaksarrangør.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.LØNN_FRA_TILTAKSARRANGØR
        Avslagsgrunnlag.LønnFraAndre.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.LØNN_FRA_ANDRE
        Avslagsgrunnlag.Institusjonsopphold.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.INSTITUSJONSOPPHOLD
        Avslagsgrunnlag.FremmetForSent.toAvslagsgrunnerBrevDto() shouldBe AvslagsgrunnerBrevDto.FREMMET_FOR_SENT
    }

    @Test
    fun `mapper liste av Avslagsgrunnlag til AvslagsgrunnerBrevDto`() {
        setOf(
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak,
            Avslagsgrunnlag.Alder,
        ).toAvslagsgrunnerBrevDto() shouldBe
            listOf(
                AvslagsgrunnerBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
                AvslagsgrunnerBrevDto.ALDER,
            )
    }
}
