package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrThrow
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
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
import tools.jackson.databind.JsonNode
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
                  "opphørsgrunner": ["INTRODUKSJONSPROGRAMMET"],
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
                  "opphørsgrunner": ["INTRODUKSJONSPROGRAMMET"],
                  "barnetillegg": true

                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }

    @Test
    fun `hjemler som krever fritekst gir brev når tilleggstekst finnes`() {
        runTest {
            val valgteHjemler = Omgjøringsresultat.OmgjøringOpphør.hjemlerSomMåHaFritekst.toList()
            val brev = genererBrevJson(
                valgteHjemler = valgteHjemler.toNonEmptySetOrThrow(),
                tilleggstekst = FritekstTilVedtaksbrev.create("Begrunnelsen skrives av saksbehandler."),
            )
            brev.koder("opphørsgrunner") shouldBe valgteHjemler.map { it.tilGrunnBrevDto().name }
            brev.skalIkkeHaHjemmeltekst()
        }
    }

    @Test
    fun `kaster om alle hjemlene krever fritekst og tilleggstekst mangler`() {
        runTest {
            shouldThrow<IllegalArgumentException> {
                genererBrevJson(
                    valgteHjemler = nonEmptySetOf(HjemmelForOpphør.Alder, HjemmelForOpphør.FremmetForSent),
                    tilleggstekst = null,
                )
            }
        }
    }

    @Test
    fun `én hjemmel med standardtekst holder uten tilleggstekst, og alle valgte hjemler blir opphørsgrunner i samme rekkefølge`() {
        runTest {
            val brev = genererBrevJson(
                valgteHjemler = nonEmptySetOf(
                    HjemmelForOpphør.FremmetForSent,
                    HjemmelForOpphør.Introduksjonsprogrammet,
                    HjemmelForOpphør.Alder,
                ),
                tilleggstekst = null,
            )
            brev.koder("opphørsgrunner") shouldBe listOf("FREMMET_FOR_SENT", "INTRODUKSJONSPROGRAMMET", "ALDER")
            brev.skalIkkeHaHjemmeltekst()
        }
    }

    private suspend fun genererBrevJson(
        valgteHjemler: NonEmptySet<HjemmelForOpphør>,
        tilleggstekst: FritekstTilVedtaksbrev?,
    ): JsonNode = lesTre(
        genererOpphørBrev(
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
            harOpphørtBarnetillegg = false,
        ),
    )
}
