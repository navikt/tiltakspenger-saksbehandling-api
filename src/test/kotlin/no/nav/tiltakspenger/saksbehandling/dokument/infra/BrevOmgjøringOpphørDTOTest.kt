package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.nonEmptySetOf
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
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
                    "Du er deltaker i introduksjonsprogram denne perioden. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger.\n\nDette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
                  ],
                  "barnetillegg": false,
                  "kontor": "Nav Tiltakspenger"
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
                        "Du er deltaker i introduksjonsprogram denne perioden. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger og barnetillegg.\n\nDette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
                  ],
                  "barnetillegg": true,
                  "kontor": "Nav Tiltakspenger"
                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }
}
