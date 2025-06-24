package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.august
import no.nav.tiltakspenger.libs.periodisering.desember
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.juni
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test

class BrevRevurderingInnvilgetDTOKtTest {

    @Test
    fun `brev uten barnetillegg innenfor 1 års-periode`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererRevurderingInnvilgetBrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                saksbehandlersVurdering = FritekstTilVedtaksbrev("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                vurderingsperiode = Periode(1.juni(2025), 31.august(2025)),
                barnetillegg = null,
                forhåndsvisning = true,
            )

            //language=json
            actual shouldEqualJson """
               {
                 "personalia":{"ident":"${fnr.verdi}","fornavn":"Ola","etternavn":"Nordmann"},
                 "saksnummer":"$saksnummer",
                 "saksbehandlerNavn":"Saksbehandler Navn",
                 "beslutterNavn":"Saksbehandler Navn",
                 "kontor":"Nav Tiltakspenger",
                 "fraDato":"1. juni 2025",
                 "tilDato":"31. august 2025",
                 "harBarnetillegg":false,
                 "introTekstMedBarnetillegg":null,
                 "satser": [
                   {
                     "år": 2025,
                     "ordinær": 298,
                     "barnetillegg": 55
                   }
                 ],
                 "saksbehandlerVurdering":"Dette er en vurdering",
                 "forhåndsvisning":true
               }
            """.trimIndent()
        }
    }

    @Test
    fun `brev uten barnetillegg som går over flere årsperioder`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererRevurderingInnvilgetBrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                saksbehandlersVurdering = FritekstTilVedtaksbrev("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                vurderingsperiode = Periode(1.desember(2024), 31.januar(2025)),
                barnetillegg = null,
                forhåndsvisning = true,
            )

            //language=json
            actual shouldEqualJson """
                {
                  "personalia":{"ident":"${fnr.verdi}","fornavn":"Ola","etternavn":"Nordmann"},
                  "saksnummer":"$saksnummer",
                  "saksbehandlerNavn":"Saksbehandler Navn",
                  "beslutterNavn":"Saksbehandler Navn",
                  "kontor":"Nav Tiltakspenger",
                  "fraDato":"1. desember 2024",
                  "tilDato":"31. januar 2025",
                  "harBarnetillegg":false,
                  "introTekstMedBarnetillegg":null,
                    "satser": [
                      {
                        "år": 2024,
                        "ordinær": 285,
                        "barnetillegg": 53
                      },
                      {
                        "år": 2025,
                        "ordinær": 298,
                        "barnetillegg": 55
                      }
                    ],
                  "saksbehandlerVurdering":"Dette er en vurdering",
                  "forhåndsvisning":true
               }
            """.trimIndent()
        }
    }

    @Test
    fun `brev med barnetillegg innenfor 1 års-periode`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererRevurderingInnvilgetBrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                saksbehandlersVurdering = FritekstTilVedtaksbrev("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                vurderingsperiode = Periode(1.juni(2025), 31.august(2025)),
                barnetillegg = Periodisering(PeriodeMedVerdi(AntallBarn(2), Periode(1.juni(2025), 31.august(2025)))),
                forhåndsvisning = true,
            )

            //language=json
            actual shouldEqualJson """
               {
                 "personalia":{"ident":"${fnr.verdi}","fornavn":"Ola","etternavn":"Nordmann"},
                 "saksnummer":"$saksnummer",
                 "saksbehandlerNavn":"Saksbehandler Navn",
                 "beslutterNavn":"Saksbehandler Navn",
                 "kontor":"Nav Tiltakspenger",
                 "fraDato":"1. juni 2025",
                 "tilDato":"31. august 2025",
                 "harBarnetillegg":true,
                 "introTekstMedBarnetillegg":"Du får tiltakspenger og barnetillegg for to barn fra og med 1. juni 2025 til og med 31. august 2025 fordi deltakelsen på arbeidsmarkedstiltaket er blitt forlenget.",
                 "satser": [
                   {
                     "år": 2025,
                     "ordinær": 298,
                     "barnetillegg": 55
                   }
                 ],
                 "saksbehandlerVurdering":"Dette er en vurdering",
                 "forhåndsvisning":true
               }
            """.trimIndent()
        }
    }

    @Test
    fun `brev med barnetillegg over flere årsperioder`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererRevurderingInnvilgetBrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                saksbehandlersVurdering = FritekstTilVedtaksbrev("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                vurderingsperiode = Periode(1.desember(2024), 31.januar(2025)),
                barnetillegg = Periodisering(
                    PeriodeMedVerdi(AntallBarn(2), Periode(1.desember(2024), 31.desember(2024))),
                    PeriodeMedVerdi(AntallBarn(3), Periode(1.januar(2025), 31.januar(2025))),
                ),
                forhåndsvisning = true,
            )

            //language=json
            actual shouldEqualJson """
                {
                  "personalia": {
                    "ident": "${fnr.verdi}",
                    "fornavn": "Ola",
                    "etternavn": "Nordmann"
                  },
                  "saksnummer": "${saksnummer.verdi}",
                  "saksbehandlerNavn": "Saksbehandler Navn",
                  "beslutterNavn": "Saksbehandler Navn",
                  "kontor": "Nav Tiltakspenger",
                  "fraDato": "1. desember 2024",
                  "tilDato": "31. januar 2025",
                  "harBarnetillegg": true,
                  "introTekstMedBarnetillegg": "Du får tiltakspenger fra og med 1. desember 2024 til og med 31. januar 2025 fordi deltakelsen på arbeidsmarkedstiltaket er blitt forlenget.\nDu får barnetillegg for to barn fra og med 1. desember 2024 til og med 31. desember 2024 og tre barn fra og med 1. januar 2025 til og med 31. januar 2025.",
                  "satser": [
                    {
                      "år": 2024,
                      "ordinær": 285,
                      "barnetillegg": 53
                    },
                    {
                      "år": 2025,
                      "ordinær": 298,
                      "barnetillegg": 55
                    }
                  ],
                  "saksbehandlerVurdering": "Dette er en vurdering",
                  "forhåndsvisning": true
                }
            """.trimIndent()
        }
    }
}
