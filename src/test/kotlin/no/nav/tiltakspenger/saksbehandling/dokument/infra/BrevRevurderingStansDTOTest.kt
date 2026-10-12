package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrThrow
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
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
                  "stansgrunner": ["INTRODUKSJONSPROGRAMMET"],
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
                  "stansgrunner": ["INTRODUKSJONSPROGRAMMET"],
                  "barnetillegg": true

                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }

    @Test
    fun `alle valgte hjemler blir stansgrunner i samme rekkefølge, uten hjemmeltekst`() {
        runTest {
            val valgteHjemler = HjemmelForStans.entries.reversed()
            val brev = lesTre(
                genererStansbrev(
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
                    valgteHjemler = valgteHjemler.toNonEmptySetOrThrow(),
                    harStansetBarnetillegg = false,
                ),
            )
            brev.koder("stansgrunner") shouldBe valgteHjemler.map { it.tilGrunnBrevDto().name }
            brev.skalIkkeHaHjemmeltekst()
        }
    }
}
