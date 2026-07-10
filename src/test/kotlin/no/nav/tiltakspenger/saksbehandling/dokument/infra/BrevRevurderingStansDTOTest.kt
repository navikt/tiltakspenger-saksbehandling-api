package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevRevurderingStansDTOTest {
    @Test
    fun `genererer og serialiserer brevdata for pdf`() {
        runTest {
            val fnr = Fnr.random()
            val actual = genererStansbrev(
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("genererer og serialiserer brevdata for pdf test"),
                fnr = fnr,
                saksbehandlerNavIdent = "SaksbehandlerNavIdent",
                beslutterNavIdent = null,
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = true,
                vedtaksdato = 1.april(2025),
                stansperiode = ObjectMother.vedtaksperiode(),
                valgteHjemler = nonEmptySetOf(HjemmelForStans.Introduksjonsprogrammet),
                harStansetBarnetillegg = false,
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
                  "stansFraOgMedDato": "1. januar 2023",
                  "valgtHjemmelTekst": [
                    "du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram har ikke rett til tiltakspenger.\n\nDette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
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
            val actual = genererStansbrev(
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("genererer og serialiserer brevdata for pdf test"),
                fnr = fnr,
                saksbehandlerNavIdent = "SaksbehandlerNavIdent",
                beslutterNavIdent = null,
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = true,
                vedtaksdato = 1.april(2025),
                stansperiode = ObjectMother.vedtaksperiode(),
                valgteHjemler = nonEmptySetOf(HjemmelForStans.Introduksjonsprogrammet),
                harStansetBarnetillegg = true,
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
                  "stansFraOgMedDato": "1. januar 2023",
                  "valgtHjemmelTekst": [
                        "du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram har ikke rett til tiltakspenger og barnetillegg.\n\nDette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
                  ],
                  "barnetillegg": true

                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }

    @Test
    fun `alle hjemler for stans har forventet brevtekst, med og uten barnetillegg`() {
        runTest {
            HjemmelForStans.entries.forEach { hjemmel ->
                listOf(false, true).forEach { barnetillegg ->
                    withClue("hjemmel=$hjemmel, barnetillegg=$barnetillegg") {
                        genererValgtHjemmelTekst(
                            valgteHjemler = nonEmptySetOf(hjemmel),
                            harStansetBarnetillegg = barnetillegg,
                        ) shouldBe listOf(hjemmel.forventetTekst(barnetillegg))
                    }
                }
            }
        }
    }

    @Test
    fun `flere valgte hjemler gir en brevtekst per hjemmel`() {
        runTest {
            genererValgtHjemmelTekst(
                valgteHjemler = nonEmptySetOf(
                    HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak,
                    HjemmelForStans.Alder,
                ),
                harStansetBarnetillegg = false,
            ) shouldBe listOf(
                HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak.forventetTekst(medBarnetillegg = false),
                HjemmelForStans.Alder.forventetTekst(medBarnetillegg = false),
            )
        }
    }

    private suspend fun genererValgtHjemmelTekst(
        valgteHjemler: NonEmptySet<HjemmelForStans>,
        harStansetBarnetillegg: Boolean,
    ): List<String> {
        val brevJson = genererStansbrev(
            hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
            hentSaksbehandlersNavn = { _ -> "Saksbehandlernavn" },
            tilleggstekst = null,
            fnr = Fnr.random(),
            saksbehandlerNavIdent = "SaksbehandlerNavIdent",
            beslutterNavIdent = null,
            saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
            forhåndsvisning = true,
            vedtaksdato = 1.april(2025),
            stansperiode = ObjectMother.vedtaksperiode(),
            valgteHjemler = valgteHjemler,
            harStansetBarnetillegg = harStansetBarnetillegg,
        )
        return lesTre(brevJson).get("valgtHjemmelTekst").toList().map { it.asText() }
    }

    /**
     * Fasit for brevtekstene som flyter videre til pdfgen/pdfgenrs (feltet `valgtHjemmelTekst`).
     * Endres en tekst her, må testdataene i tiltakspenger-pdfgen/tiltakspenger-pdfgenrs (`data/tpts/stansvedtak.json`) oppdateres tilsvarende.
     */
    private fun HjemmelForStans.forventetTekst(medBarnetillegg: Boolean): String {
        val ytelse = if (medBarnetillegg) "tiltakspenger og barnetillegg" else "tiltakspenger"

        return when (this) {
            HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak ->
                """
                    du ikke lenger deltar på arbeidsmarkedstiltak.

                    Du må være deltaker i et arbeidsmarkedstiltak for å ha rett til å få $ytelse.

                    Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 2.
                """.trimIndent()

            HjemmelForStans.Alder ->
                """
                    du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få $ytelse.

                    Det kommer frem av tiltakspengeforskriften § 3.
                """.trimIndent()

            HjemmelForStans.Livsoppholdytelser ->
                """
                    du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få $ytelse.

                    Dette kommer frem av arbeidsmarkedsloven § 13 første ledd og tiltakspengeforskriften § 7 første ledd.
                """.trimIndent()

            HjemmelForStans.Kvalifiseringsprogrammet ->
                """
                    du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
                """.trimIndent()

            HjemmelForStans.Introduksjonsprogrammet ->
                """
                    du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
                """.trimIndent()

            HjemmelForStans.LønnFraTiltaksarrangør ->
                """
                    du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket.

                    Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket har ikke rett til $ytelse.

                    Dette kommer frem av tiltakspengeforskriften § 8.
                """.trimIndent()

            HjemmelForStans.LønnFraAndre ->
                """
                    du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.

                    Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til $ytelse. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.

                    Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.

                    Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 8 andre ledd.
                """.trimIndent()

            HjemmelForStans.Institusjonsopphold ->
                """
                    du oppholder deg på en institusjon med gratis opphold, mat og drikke.

                    Deltakere som har opphold i institusjon med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket har ikke rett til $ytelse.

                    Det er gjort unntak for opphold i barnevernsinstitusjoner. Dette kommer frem av tiltakspengeforskriften § 9.
                """.trimIndent()

            HjemmelForStans.IkkeLovligOpphold ->
                """
                    du i denne perioden ikke har lovlig opphold i Norge.

                    Du må ha lovlig opphold i Norge, for å ha rett til $ytelse.

                    Dette kommer frem av arbeidsmarkedsloven § 2.
                """.trimIndent()
        }
    }
}
