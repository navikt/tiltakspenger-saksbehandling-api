package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevOmgjøringOpphørDTOTest {
    @Test
    fun `genererer og serialiserer brevdata for pdf`() {
        runTest {
            val fnr = Fnr.random()
            val vedtaksperiode = 1.januar(2026) til 31.januar(2026)

            val actual = genererOpphørBrev(
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("genererer og serialiserer brevdata for pdf test"),
                fnr = fnr,
                saksbehandlerNavIdent = "SaksbehandlerNavIdent",
                beslutterNavIdent = null,
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = true,
                vedtaksdato = 1.april(2025),
                vedtaksperiode = vedtaksperiode,
                valgteHjemler = nonEmptySetOf(HjemmelForOpphør.Introduksjonsprogrammet),
                harOpphørtBarnetillegg = false,
            )

            //language=json
            val expected = """
                {
                 "personalia": {
                    "ident": "${fnr.verdi}",
                    "fornavn": "Fornavn",
                    "etternavn": "Etternavn"
                  },
                  "saksnummer": "202501012000",
                  "saksbehandlerNavn": "Saksbehandlernavn",
                  "beslutterNavn": null,
                  "datoForUtsending": "1. april 2025",
                  "tilleggstekst": "genererer og serialiserer brevdata for pdf test",
                  "forhandsvisning": true,
                  "vedtaksperiode": {
                    "fraOgMed": "1. januar 2026",
                    "tilOgMed": "31. januar 2026"
                  },
                  "valgtHjemmelTekst": [
                    "Du er deltaker i introduksjonsprogram i denne perioden. Deltakere i introduksjonsprogram har ikke rett til tiltakspenger.\n\nDette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
                  ],
                  "barnetillegg": false

                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }

    @Test
    fun `genererer og serialiserer brevdata for pdf med barnetillegg`() {
        runTest {
            val fnr = Fnr.random()
            val vedtaksperiode = 1.januar(2026) til 31.januar(2026)

            val actual = genererOpphørBrev(
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("genererer og serialiserer brevdata for pdf test"),
                fnr = fnr,
                saksbehandlerNavIdent = "SaksbehandlerNavIdent",
                beslutterNavIdent = null,
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = true,
                vedtaksdato = 1.april(2025),
                vedtaksperiode = vedtaksperiode,
                valgteHjemler = nonEmptySetOf(HjemmelForOpphør.Introduksjonsprogrammet),
                harOpphørtBarnetillegg = true,
            )

            //language=json
            val expected = """
                {
                 "personalia": {
                    "ident": "${fnr.verdi}",
                    "fornavn": "Fornavn",
                    "etternavn": "Etternavn"
                  },
                  "saksnummer": "202501012000",
                  "saksbehandlerNavn": "Saksbehandlernavn",
                  "beslutterNavn": null,
                  "datoForUtsending": "1. april 2025",
                  "tilleggstekst": "genererer og serialiserer brevdata for pdf test",
                  "forhandsvisning": true,
                  "vedtaksperiode": {
                    "fraOgMed": "1. januar 2026",
                    "tilOgMed": "31. januar 2026"
                  },
                  "valgtHjemmelTekst": [
                        "Du er deltaker i introduksjonsprogram i denne perioden. Deltakere i introduksjonsprogram har ikke rett til tiltakspenger og barnetillegg.\n\nDette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
                  ],
                  "barnetillegg": true

                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }

    @Test
    fun `alle hjemler for opphør med forhåndsdefinert tekst har forventet brevtekst, med og uten barnetillegg`() {
        runTest {
            val hjemlerMedTekst = HjemmelForOpphør.entries - Omgjøringsresultat.OmgjøringOpphør.hjemlerSomMåHaFritekst

            hjemlerMedTekst.forEach { hjemmel ->
                listOf(false, true).forEach { barnetillegg ->
                    withClue("hjemmel=$hjemmel, barnetillegg=$barnetillegg") {
                        genererValgtHjemmelTekst(
                            valgteHjemler = nonEmptySetOf(hjemmel),
                            harOpphørtBarnetillegg = barnetillegg,
                            tilleggstekst = null,
                        ) shouldBe listOf(hjemmel.forventetTekst(barnetillegg))
                    }
                }
            }
        }
    }

    @Test
    fun `hjemler som krever fritekst gir ingen hjemmelstekst i brevet`() {
        runTest {
            Omgjøringsresultat.OmgjøringOpphør.hjemlerSomMåHaFritekst.forEach { hjemmel ->
                withClue("hjemmel=$hjemmel") {
                    genererValgtHjemmelTekst(
                        valgteHjemler = nonEmptySetOf(hjemmel),
                        harOpphørtBarnetillegg = false,
                        tilleggstekst = FritekstTilVedtaksbrev.create("Begrunnelsen skrives av saksbehandler."),
                    ).shouldBeNull()
                }
            }
        }
    }

    @Test
    fun `hjemler som krever fritekst filtreres bort fra hjemmelstekstene i kombinasjon med andre hjemler`() {
        runTest {
            genererValgtHjemmelTekst(
                valgteHjemler = nonEmptySetOf(
                    HjemmelForOpphør.Alder,
                    HjemmelForOpphør.Introduksjonsprogrammet,
                ),
                harOpphørtBarnetillegg = false,
                tilleggstekst = null,
            ) shouldBe listOf(HjemmelForOpphør.Introduksjonsprogrammet.forventetTekst(medBarnetillegg = false))
        }
    }

    @Test
    fun `kaster om ingen av hjemlene har forhåndsdefinert tekst og fritekst mangler`() {
        runTest {
            shouldThrow<IllegalArgumentException> {
                genererValgtHjemmelTekst(
                    valgteHjemler = nonEmptySetOf(HjemmelForOpphør.Alder, HjemmelForOpphør.FremmetForSent),
                    harOpphørtBarnetillegg = false,
                    tilleggstekst = null,
                )
            }
        }
    }

    private suspend fun genererValgtHjemmelTekst(
        valgteHjemler: NonEmptySet<HjemmelForOpphør>,
        harOpphørtBarnetillegg: Boolean,
        tilleggstekst: FritekstTilVedtaksbrev?,
    ): List<String>? {
        val brevJson = genererOpphørBrev(
            hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
            hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
            tilleggstekst = tilleggstekst,
            fnr = Fnr.random(),
            saksbehandlerNavIdent = "SaksbehandlerNavIdent",
            beslutterNavIdent = null,
            saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
            forhåndsvisning = true,
            vedtaksdato = 1.april(2025),
            vedtaksperiode = 1.januar(2026) til 31.januar(2026),
            valgteHjemler = valgteHjemler,
            harOpphørtBarnetillegg = harOpphørtBarnetillegg,
        )
        val node = lesTre(brevJson).get("valgtHjemmelTekst")
        return if (node.isNull) null else node.toList().map { it.asText() }
    }

    /**
     * Fasit for brevtekstene som flyter videre til pdfgen/pdfgenrs (feltet `valgtHjemmelTekst`).
     * Endres en tekst her, må testdataene i tiltakspenger-pdfgen/tiltakspenger-pdfgenrs (`data/tpts/opphørVedtak.json`/`vedtakOpphør.json`) oppdateres tilsvarende.
     */
    private fun HjemmelForOpphør.forventetTekst(medBarnetillegg: Boolean): String {
        val ytelse = if (medBarnetillegg) "tiltakspenger og barnetillegg" else "tiltakspenger"

        return when (this) {
            HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak ->
                """
                    Vilkåret om deltakelse i arbeidsmarkedstiltak er ikke oppfylt i denne perioden.

                    Du må være deltaker i et arbeidsmarkedstiltak for å ha rett til å få $ytelse.

                    Dette kommer frem av arbeidsmarkedsloven § 13, tiltakspengeforskriften § 2.
                """.trimIndent()

            HjemmelForOpphør.Livsoppholdytelser ->
                """
                    Du har rett til annen pengestøtte til livsopphold i denne perioden.

                    Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få $ytelse.

                    Dette kommer frem av arbeidsmarkedsloven § 13 første ledd, forskrift om tiltakspenger § 7 første ledd.
                """.trimIndent()

            HjemmelForOpphør.Kvalifiseringsprogrammet ->
                """
                    Du er deltaker i kvalifiseringsprogram i denne perioden. Deltakere i kvalifiseringsprogram har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
                """.trimIndent()

            HjemmelForOpphør.Introduksjonsprogrammet ->
                """
                    Du er deltaker i introduksjonsprogram i denne perioden. Deltakere i introduksjonsprogram har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
                """.trimIndent()

            HjemmelForOpphør.LønnFraTiltaksarrangør ->
                """
                    Du mottar lønn fra tiltaksarrangøren for tiden i arbeidsmarkedstiltaket for denne perioden.

                    Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 8.
                """.trimIndent()

            HjemmelForOpphør.LønnFraAndre ->
                """
                    Du mottar i denne perioden lønn for arbeid som er en del av tiltaksdeltakelsen. Du har derfor dekning av utgifter til livsopphold.

                    Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til $ytelse. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.

                    Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.

                    Dette kommer frem av arbeidsmarkedsloven § 13, tiltakspengeforskriften § 8 andre ledd.
                """.trimIndent()

            HjemmelForOpphør.Institusjonsopphold ->
                """
                    Du oppholder deg på en institusjon med gratis opphold, mat og drikke i denne perioden.

                    Deltakere som har opphold i institusjon med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket har ikke rett til $ytelse.

                    Det er gjort unntak for opphold i barnevernsinstitusjoner. Dette kommer frem av tiltakspengeforskriften § 9.
                """.trimIndent()

            HjemmelForOpphør.IkkeLovligOpphold ->
                """
                    I denne perioden har du ikke lovlig opphold i Norge.

                    Du må ha lovlig opphold i Norge, for å ha rett til $ytelse.

                    Dette kommer frem av arbeidsmarkedsloven § 2.
                """.trimIndent()

            HjemmelForOpphør.Alder,
            HjemmelForOpphør.FremmetForSent,
            -> error("Hjemmel $this har ingen forhåndsdefinert brevtekst — saksbehandler må bruke fritekst")
        }
    }
}
