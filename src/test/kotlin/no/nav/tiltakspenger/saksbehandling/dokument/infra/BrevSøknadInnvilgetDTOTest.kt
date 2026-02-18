package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test

class BrevSøknadInnvilgetDTOTest {

    @Test
    fun `brev uten barnetillegg innenfor 1 års-periode`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.juni(2025), 31.august(2025))),
                barnetillegg = null,
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            //language=json
            actual shouldEqualJson """
               {
                 "personalia":{"ident":"${fnr.verdi}","fornavn":"Ola","etternavn":"Nordmann"},
                 "saksnummer":"$saksnummer",
                 "saksbehandlerNavn":"Saksbehandler Navn",
                 "beslutterNavn":"Saksbehandler Navn",
                 "kontor":"Nav Tiltakspenger",
                 "harBarnetillegg":false,
                 "satser": [
                   {
                     "år": 2025,
                     "ordinær": 298,
                     "barnetillegg": 55
                   }
                 ],
                 "tilleggstekst":"Dette er en vurdering",
                 "forhandsvisning":true,
                 "datoForUtsending": "31. januar 2025",
                 "innvilgelsesperioder": { 
                  "antallDagerTekst": "fem dager",
                  "perioder": [
                    {
                      "fraOgMed": "1. juni 2025",
                      "tilOgMed": "31. august 2025" 
                    }
                  ]
                 },
                 "barnetillegg": []
               }
            """.trimIndent()
        }
    }

    @Test
    fun `brev uten barnetillegg som går over flere årsperioder`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.desember(2024), 31.januar(2025))),
                barnetillegg = null,
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            //language=json
            actual shouldEqualJson """
                {
                  "personalia":{"ident":"${fnr.verdi}","fornavn":"Ola","etternavn":"Nordmann"},
                  "saksnummer":"$saksnummer",
                  "saksbehandlerNavn":"Saksbehandler Navn",
                  "beslutterNavn":"Saksbehandler Navn",
                  "kontor":"Nav Tiltakspenger",
                  "harBarnetillegg":false,
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
                  "tilleggstekst":"Dette er en vurdering",
                  "forhandsvisning":true,
                  "datoForUtsending": "31. januar 2025",
                 "innvilgelsesperioder": { 
                  "antallDagerTekst": "fem dager",
                  "perioder": [
                    {
                      "fraOgMed": "1. desember 2024",
                      "tilOgMed": "31. januar 2025" 
                    }
                  ]
                 },
                 "barnetillegg": []
               }
            """.trimIndent()
        }
    }

    @Test
    fun `brev med barnetillegg innenfor 1 års-periode`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.juni(2025), 31.august(2025))),
                barnetillegg = SammenhengendePeriodisering(AntallBarn(2), 1.juni(2025) til 31.august(2025)),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            //language=json
            actual shouldEqualJson """
               {
                 "personalia":{"ident":"${fnr.verdi}","fornavn":"Ola","etternavn":"Nordmann"},
                 "saksnummer":"$saksnummer",
                 "saksbehandlerNavn":"Saksbehandler Navn",
                 "beslutterNavn":"Saksbehandler Navn",
                 "kontor":"Nav Tiltakspenger",
                 "harBarnetillegg":true,
                 "satser": [
                   {
                     "år": 2025,
                     "ordinær": 298,
                     "barnetillegg": 55
                   }
                 ],
                 "tilleggstekst":"Dette er en vurdering",
                 "forhandsvisning": true,
                 "datoForUtsending": "31. januar 2025",
                 "innvilgelsesperioder": { 
                  "antallDagerTekst": "fem dager",
                  "perioder": [
                    {
                      "fraOgMed": "1. juni 2025",
                      "tilOgMed": "31. august 2025" 
                    }
                  ]
                 },
                 "barnetillegg": [
                   {
                      "antallBarnTekst": "to",
                      "periode": {
                       "fraOgMed": "1. juni 2025",
                       "tilOgMed": "31. august 2025" 
                      }
                   }
                 ]
               }
            """.trimIndent()
        }
    }

    @Test
    fun `brev med barnetillegg over flere årsperioder`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.desember(2024), 31.januar(2025))),
                barnetillegg = SammenhengendePeriodisering(
                    PeriodeMedVerdi(AntallBarn(2), Periode(1.desember(2024), 31.desember(2024))),
                    PeriodeMedVerdi(AntallBarn(3), Periode(1.januar(2025), 31.januar(2025))),
                ),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
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
                  "harBarnetillegg": true,
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
                  "tilleggstekst": "Dette er en vurdering",
                  "forhandsvisning": true,
                  "datoForUtsending": "31. januar 2025",
                 "innvilgelsesperioder": { 
                  "antallDagerTekst": "fem dager",
                  "perioder": [
                    {
                      "fraOgMed": "1. desember 2024",
                      "tilOgMed": "31. januar 2025" 
                    }
                  ]
                 },
                 "barnetillegg": [
                    {
                        "antallBarnTekst": "to",
                        "periode": {
                          "fraOgMed": "1. desember 2024",
                          "tilOgMed": "31. desember 2024"
                        }
                    },
                    {
                        "antallBarnTekst": "tre",
                        "periode":  {
                          "fraOgMed": "1. januar 2025",
                          "tilOgMed": "31. januar 2025"
                        }
                    }
                 ]
                }
            """.trimIndent()
        }
    }

    @Test
    fun `brev med barnetillegg over tre perioder`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.november(2024), 31.januar(2025))),
                barnetillegg = SammenhengendePeriodisering(
                    PeriodeMedVerdi(AntallBarn(1), Periode(1.november(2024), 30.november(2024))),
                    PeriodeMedVerdi(AntallBarn(2), Periode(1.desember(2024), 31.desember(2024))),
                    PeriodeMedVerdi(AntallBarn(3), Periode(1.januar(2025), 31.januar(2025))),
                ),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
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
                  "harBarnetillegg": true,
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
                  "tilleggstekst": "Dette er en vurdering",
                  "forhandsvisning": true,
                  "datoForUtsending": "31. januar 2025",
                  "innvilgelsesperioder": {
                    "antallDagerTekst": "fem dager",
                    "perioder": [
                      {
                        "fraOgMed": "1. november 2024",
                        "tilOgMed": "31. januar 2025"
                      }
                    ]
                  },
                  "barnetillegg": [
                    {
                      "antallBarnTekst": "ett",
                      "periode" : {
                        "fraOgMed": "1. november 2024",
                        "tilOgMed": "30. november 2024"
                      }
                    },
                    {
                      "antallBarnTekst": "to",
                      "periode" :  {
                        "fraOgMed": "1. desember 2024",
                        "tilOgMed": "31. desember 2024"
                      }
                    },
                    {
                      "antallBarnTekst": "tre",
                      "periode" :  {
                        "fraOgMed": "1. januar 2025",
                        "tilOgMed": "31. januar 2025"
                      }
                    }
                  ]
                }
            """.trimIndent()
        }
    }

    @Test
    fun `brev med to innvilgelsesperioder etter sammenslåing, og tre barnetilleggsperioder`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                innvilgelsesperioder = innvilgelsesperioder(
                    Periode(1.november(2024), 31.januar(2025)),
                    Periode(1.februar(2025), 28.februar(2025)),
                    Periode(1.april(2025), 30.april(2025)),
                ),
                barnetillegg = Periodisering(
                    PeriodeMedVerdi(AntallBarn(1), Periode(1.november(2024), 31.januar(2025))),
                    PeriodeMedVerdi(AntallBarn(2), Periode(1.februar(2025), 28.februar(2025))),
                    PeriodeMedVerdi(AntallBarn(3), Periode(1.april(2025), 30.april(2025))),
                ),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
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
                  "harBarnetillegg": true,
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
                  "tilleggstekst": "Dette er en vurdering",
                  "forhandsvisning": true,
                  "datoForUtsending": "31. januar 2025",
                  "innvilgelsesperioder": {
                      "antallDagerTekst": "fem dager",
                      "perioder": [
                      {
                          "fraOgMed": "1. november 2024",
                          "tilOgMed": "28. februar 2025"
                      },
                      {
                          "fraOgMed": "1. april 2025",
                          "tilOgMed": "30. april 2025"
                      }
                      ]
                  },
                  "barnetillegg": [
                    {
                      "antallBarnTekst": "ett",
                      "periode" : {
                        "fraOgMed": "1. november 2024",
                        "tilOgMed": "31. januar 2025"
                      }
                    },
                    {
                      "antallBarnTekst": "to",
                      "periode" :  {
                        "fraOgMed": "1. februar 2025",
                        "tilOgMed": "28. februar 2025"
                      }
                    },
                    {
                      "antallBarnTekst": "tre",
                      "periode" :  {
                        "fraOgMed": "1. april 2025",
                        "tilOgMed": "30. april 2025"
                      }
                    }
                  ]
                }
            """.trimIndent()
        }
    }

    @Test
    fun `brev med tre innvilgelsesperioder etter sammenslåing og tre tilsvarende barnetilleggsperioder`() {
        runTest {
            val fnr = Fnr.random()
            val saksnummer = Saksnummer("202501301001")
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = FritekstTilVedtaksbrev.create("Dette er en vurdering"),
                fnr = fnr,
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = saksnummer,
                innvilgelsesperioder = innvilgelsesperioder(
                    Periode(1.november(2024), 31.januar(2025)),
                    Periode(1.februar(2025), 28.februar(2025)),
                    Periode(1.april(2025), 30.april(2025)),
                    Periode(2.mai(2025), 31.mai(2025)),
                ),
                barnetillegg = Periodisering(
                    PeriodeMedVerdi(AntallBarn(1), Periode(1.november(2024), 28.februar(2025))),
                    PeriodeMedVerdi(AntallBarn(2), Periode(1.april(2025), 30.april(2025))),
                    PeriodeMedVerdi(AntallBarn(3), Periode(2.mai(2025), 31.mai(2025))),
                ),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
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
                  "harBarnetillegg": true,
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
                  "tilleggstekst": "Dette er en vurdering",
                  "forhandsvisning": true,
                  "datoForUtsending": "31. januar 2025",
                  "innvilgelsesperioder": {
                    "antallDagerTekst": "fem dager",
                    "perioder": [
                      {
                          "fraOgMed": "1. november 2024",
                          "tilOgMed": "28. februar 2025"
                      },
                      {
                          "fraOgMed": "1. april 2025",
                          "tilOgMed": "30. april 2025"
                      },
                      {
                          "fraOgMed": "2. mai 2025",
                          "tilOgMed": "31. mai 2025"
                      }
                    ]
                  },
                  "barnetillegg": [
                    {
                      "antallBarnTekst": "ett",
                      "periode": {
                        "fraOgMed": "1. november 2024",
                        "tilOgMed": "28. februar 2025"
                      }
                    },
                    {
                      "antallBarnTekst": "to",
                      "periode":  {
                        "fraOgMed": "1. april 2025",
                        "tilOgMed": "30. april 2025"
                      }
                    },
                    {
                      "antallBarnTekst": "tre",
                      "periode":  {
                        "fraOgMed": "2. mai 2025",
                        "tilOgMed": "31. mai 2025"
                      }
                    }
                 ]
              }
            """.trimIndent()
        }
    }
}
