package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.json.JSONObject
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
                 
                 "tilleggstekst":"Dette er en vurdering",
                 "forhandsvisning":true,
                 "datoForUtsending": "2025-01-31",
                 "innvilgelsesperioder": { 
                  "antallDagerPerMeldeperiode": 10,
                  "tiltak": ["Arbeidsmarkedsoppfølging gruppe"]
                 },
                 "sisteKjenteSatsår": 2026,
                 "perioder": [
                   {
                     "periode": {"fraOgMed": "2025-06-01", "tilOgMed": "2025-08-31"},
                     "beløp": [
                       {
                         "periode": {"fraOgMed": "2025-06-01", "tilOgMed": "2025-08-31"},
                         "antallBarn": 0,
                         "tiltakspenger": 298,
                         "barnetilleggBeløp": 0,
                         "total": 298
                       }
                     ]
                   }
                 ]
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
                  
                  "tilleggstekst":"Dette er en vurdering",
                  "forhandsvisning":true,
                  "datoForUtsending": "2025-01-31",
                 "innvilgelsesperioder": { 
                  "antallDagerPerMeldeperiode": 10,
                  "tiltak": ["Arbeidsmarkedsoppfølging gruppe"]
                 },
                 "sisteKjenteSatsår": 2026,
                 "perioder": [
                   {
                     "periode": {"fraOgMed": "2024-12-01", "tilOgMed": "2025-01-31"},
                     "beløp": [
                       {
                         "periode": {"fraOgMed": "2024-12-01", "tilOgMed": "2024-12-31"},
                         "antallBarn": 0,
                         "tiltakspenger": 285,
                         "barnetilleggBeløp": 0,
                         "total": 285
                       },
                       {
                         "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-01-31"},
                         "antallBarn": 0,
                         "tiltakspenger": 298,
                         "barnetilleggBeløp": 0,
                         "total": 298
                       }
                     ]
                   }
                 ]
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
                 
                 "tilleggstekst":"Dette er en vurdering",
                 "forhandsvisning": true,
                 "datoForUtsending": "2025-01-31",
                 "innvilgelsesperioder": { 
                  "antallDagerPerMeldeperiode": 10,
                  "tiltak": ["Arbeidsmarkedsoppfølging gruppe"]
                 },
                 "sisteKjenteSatsår": 2026,
                 "perioder": [
                   {
                     "periode": {"fraOgMed": "2025-06-01", "tilOgMed": "2025-08-31"},
                     "beløp": [
                       {
                         "periode": {"fraOgMed": "2025-06-01", "tilOgMed": "2025-08-31"},
                         "antallBarn": 2,
                         "tiltakspenger": 298,
                         "barnetilleggBeløp": 110,
                         "total": 408
                       }
                     ]
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
                  "tilleggstekst": "Dette er en vurdering",
                  "forhandsvisning": true,
                  "datoForUtsending": "2025-01-31",
                 "innvilgelsesperioder": { 
                  "antallDagerPerMeldeperiode": 10,
                  "tiltak": ["Arbeidsmarkedsoppfølging gruppe"]
                 },
                 "sisteKjenteSatsår": 2026,
                 "perioder": [
                   {
                     "periode": {"fraOgMed": "2024-12-01", "tilOgMed": "2025-01-31"},
                     "beløp": [
                       {
                         "periode": {"fraOgMed": "2024-12-01", "tilOgMed": "2024-12-31"},
                         "antallBarn": 2,
                         "tiltakspenger": 285,
                         "barnetilleggBeløp": 106,
                         "total": 391
                       },
                       {
                         "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-01-31"},
                         "antallBarn": 3,
                         "tiltakspenger": 298,
                         "barnetilleggBeløp": 165,
                         "total": 463
                       }
                     ]
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
                  "tilleggstekst": "Dette er en vurdering",
                  "forhandsvisning": true,
                  "datoForUtsending": "2025-01-31",
                  "innvilgelsesperioder": {
                    "antallDagerPerMeldeperiode": 10,
                    "tiltak": ["Arbeidsmarkedsoppfølging gruppe"]
                  },
                  "sisteKjenteSatsår": 2026,
                  "perioder": [
                    {
                      "periode": {"fraOgMed": "2024-11-01", "tilOgMed": "2025-01-31"},
                      "beløp": [
                        {
                          "periode": {"fraOgMed": "2024-11-01", "tilOgMed": "2024-11-30"},
                          "antallBarn": 1,
                          "tiltakspenger": 285,
                          "barnetilleggBeløp": 53,
                          "total": 338
                        },
                        {
                          "periode": {"fraOgMed": "2024-12-01", "tilOgMed": "2024-12-31"},
                          "antallBarn": 2,
                          "tiltakspenger": 285,
                          "barnetilleggBeløp": 106,
                          "total": 391
                        },
                        {
                          "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-01-31"},
                          "antallBarn": 3,
                          "tiltakspenger": 298,
                          "barnetilleggBeløp": 165,
                          "total": 463
                        }
                      ]
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
                  "tilleggstekst": "Dette er en vurdering",
                  "forhandsvisning": true,
                  "datoForUtsending": "2025-01-31",
                  "innvilgelsesperioder": {
                      "antallDagerPerMeldeperiode": 10,
                      "tiltak": ["Arbeidsmarkedsoppfølging gruppe"]
                  },
                  "sisteKjenteSatsår": 2026,
                  "perioder": [
                    {
                      "periode": {"fraOgMed": "2024-11-01", "tilOgMed": "2025-02-28"},
                      "beløp": [
                        {
                          "periode": {"fraOgMed": "2024-11-01", "tilOgMed": "2024-12-31"},
                          "antallBarn": 1,
                          "tiltakspenger": 285,
                          "barnetilleggBeløp": 53,
                          "total": 338
                        },
                        {
                          "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-01-31"},
                          "antallBarn": 1,
                          "tiltakspenger": 298,
                          "barnetilleggBeløp": 55,
                          "total": 353
                        },
                        {
                          "periode": {"fraOgMed": "2025-02-01", "tilOgMed": "2025-02-28"},
                          "antallBarn": 2,
                          "tiltakspenger": 298,
                          "barnetilleggBeløp": 110,
                          "total": 408
                        }
                      ]
                    },
                    {
                      "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                      "beløp": [
                        {
                          "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                          "antallBarn": 3,
                          "tiltakspenger": 298,
                          "barnetilleggBeløp": 165,
                          "total": 463
                        }
                      ]
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
                  "tilleggstekst": "Dette er en vurdering",
                  "forhandsvisning": true,
                  "datoForUtsending": "2025-01-31",
                  "innvilgelsesperioder": {
                    "antallDagerPerMeldeperiode": 10,
                    "tiltak": ["Arbeidsmarkedsoppfølging gruppe"]
                  },
                  "sisteKjenteSatsår": 2026,
                  "perioder": [
                    {
                      "periode": {"fraOgMed": "2024-11-01", "tilOgMed": "2025-02-28"},
                      "beløp": [
                        {
                          "periode": {"fraOgMed": "2024-11-01", "tilOgMed": "2024-12-31"},
                          "antallBarn": 1,
                          "tiltakspenger": 285,
                          "barnetilleggBeløp": 53,
                          "total": 338
                        },
                        {
                          "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-02-28"},
                          "antallBarn": 1,
                          "tiltakspenger": 298,
                          "barnetilleggBeløp": 55,
                          "total": 353
                        }
                      ]
                    },
                    {
                      "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                      "beløp": [
                        {
                          "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                          "antallBarn": 2,
                          "tiltakspenger": 298,
                          "barnetilleggBeløp": 110,
                          "total": 408
                        }
                      ]
                    },
                    {
                      "periode": {"fraOgMed": "2025-05-02", "tilOgMed": "2025-05-31"},
                      "beløp": [
                        {
                          "periode": {"fraOgMed": "2025-05-02", "tilOgMed": "2025-05-31"},
                          "antallBarn": 3,
                          "tiltakspenger": 298,
                          "barnetilleggBeløp": 165,
                          "total": 463
                        }
                      ]
                    }
                  ]
              }
            """.trimIndent()
        }
    }

    @Test
    fun `antall dager sendes ikke når det varierer i perioden`() {
        runTest {
            val deltakelse = tiltaksdeltakelse(fom = 1.juni(2025), tom = 31.august(2025))
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(
                    innvilgelsesperiode(Periode(1.juni(2025), 30.juni(2025)), deltakelse, AntallDagerForMeldeperiode(6)),
                    innvilgelsesperiode(Periode(1.juli(2025), 31.august(2025)), deltakelse, AntallDagerForMeldeperiode(10)),
                ),
                barnetillegg = null,
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONObject("innvilgelsesperioder").opt("antallDagerPerMeldeperiode") shouldBe null
        }
    }

    @Test
    fun `perioder - én periode uten barnetillegg gir ett beløp uten barnetillegg`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.juni(2025), 31.august(2025))),
                barnetillegg = null,
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONArray("perioder").toString() shouldEqualJson
                """
                    [
  {
    "beløp": [
      {
        "total": 298,
        "barnetilleggBeløp": 0,
        "antallBarn": 0,
        "periode": {
          "fraOgMed": "2025-06-01",
          "tilOgMed": "2025-08-31"
        },
        "tiltakspenger": 298
      }
    ],
    "periode": {
      "fraOgMed": "2025-06-01",
      "tilOgMed": "2025-08-31"
    }
  }
]
                """
        }
    }

    @Test
    fun `perioder - én periode med barnetillegg gir ett beløp med summert barnetillegg`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.juni(2025), 31.august(2025))),
                barnetillegg = SammenhengendePeriodisering(AntallBarn(2), 1.juni(2025) til 31.august(2025)),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONArray("perioder").toString() shouldEqualJson
                """
                   [
  {
    "beløp": [
      {
        "total": 408,
        "barnetilleggBeløp": 110,
        "antallBarn": 2,
        "periode": {
          "fraOgMed": "2025-06-01",
          "tilOgMed": "2025-08-31"
        },
        "tiltakspenger": 298
      }
    ],
    "periode": {
      "fraOgMed": "2025-06-01",
      "tilOgMed": "2025-08-31"
    }
  }
] 
                """
        }
    }

    @Test
    fun `perioder - satsendring midt i perioden gir to beløp innenfor samme periode`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.november(2024), 28.februar(2025))),
                barnetillegg = SammenhengendePeriodisering(AntallBarn(1), 1.november(2024) til 28.februar(2025)),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONArray("perioder").toString() shouldEqualJson
                """
                [
                  {
                    "periode": {"fraOgMed": "2024-11-01", "tilOgMed": "2025-02-28"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2024-11-01", "tilOgMed": "2024-12-31"},
                        "antallBarn": 1,
                        "tiltakspenger": 285,
                        "barnetilleggBeløp": 53,
                        "total": 338
                      },
                      {
                        "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-02-28"},
                        "antallBarn": 1,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 55,
                        "total": 353
                      }
                    ]
                  }
                ]
                """
        }
    }

    @Test
    fun `perioder - endring i antall barn midt i perioden gir to beløp innenfor samme periode`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.april(2025), 31.mai(2025))),
                barnetillegg = SammenhengendePeriodisering(
                    PeriodeMedVerdi(AntallBarn(1), Periode(1.april(2025), 30.april(2025))),
                    PeriodeMedVerdi(AntallBarn(2), Periode(1.mai(2025), 31.mai(2025))),
                ),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONArray("perioder").toString() shouldEqualJson
                """
                [
                  {
                    "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-05-31"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                        "antallBarn": 1,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 55,
                        "total": 353
                      },
                      {
                        "periode": {"fraOgMed": "2025-05-01", "tilOgMed": "2025-05-31"},
                        "antallBarn": 2,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 110,
                        "total": 408
                      }
                    ]
                  }
                ]
                """
        }
    }

    @Test
    fun `perioder - to innvilgelsesperioder gir to perioder`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(
                    Periode(1.april(2025), 30.april(2025)),
                    Periode(1.juni(2025), 30.juni(2025)),
                ),
                barnetillegg = null,
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONArray("perioder")
                .toString() shouldEqualJson
                """
                [
                  {
                    "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                        "antallBarn": 0,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 0,
                        "total": 298
                      }
                    ]
                  },
                  {
                    "periode": {"fraOgMed": "2025-06-01", "tilOgMed": "2025-06-30"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2025-06-01", "tilOgMed": "2025-06-30"},
                        "antallBarn": 0,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 0,
                        "total": 298
                      }
                    ]
                  }
                ]
                """
        }
    }

    @Test
    fun `perioder - barnetillegg i bare deler av perioden gir beløp uten barnetillegg resten av perioden`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.april(2025), 31.mai(2025))),
                barnetillegg = SammenhengendePeriodisering(AntallBarn(1), 1.mai(2025) til 31.mai(2025)),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONArray("perioder").toString() shouldEqualJson
                """
                [
                  {
                    "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-05-31"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                        "antallBarn": 0,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 0,
                        "total": 298
                      },
                      {
                        "periode": {"fraOgMed": "2025-05-01", "tilOgMed": "2025-05-31"},
                        "antallBarn": 1,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 55,
                        "total": 353
                      }
                    ]
                  }
                ]
                """
        }
    }

    @Test
    fun `perioder - tilstøtende perioder med like beløp slås sammen til én periode med ett beløp`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(
                    Periode(1.april(2025), 30.april(2025)),
                    Periode(1.mai(2025), 31.mai(2025)),
                ),
                barnetillegg = Periodisering(
                    PeriodeMedVerdi(AntallBarn(1), Periode(1.april(2025), 30.april(2025))),
                    PeriodeMedVerdi(AntallBarn(1), Periode(1.mai(2025), 31.mai(2025))),
                ),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONArray("perioder").toString() shouldEqualJson
                """
                [
                  {
                    "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-05-31"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-05-31"},
                        "antallBarn": 1,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 55,
                        "total": 353
                      }
                    ]
                  }
                ]
                """
        }
    }

    @Test
    fun `perioder - opphold mellom perioder med like beløp slås ikke sammen`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(
                    Periode(1.april(2025), 30.april(2025)),
                    Periode(2.mai(2025), 31.mai(2025)),
                ),
                barnetillegg = Periodisering(
                    PeriodeMedVerdi(AntallBarn(1), Periode(1.april(2025), 30.april(2025))),
                    PeriodeMedVerdi(AntallBarn(1), Periode(2.mai(2025), 31.mai(2025))),
                ),
                forhåndsvisning = true,
                vedtaksdato = 31.januar(2025),
            )

            JSONObject(actual).getJSONArray("perioder").toString() shouldEqualJson
                """
                [
                  {
                    "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2025-04-01", "tilOgMed": "2025-04-30"},
                        "antallBarn": 1,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 55,
                        "total": 353
                      }
                    ]
                  },
                  {
                    "periode": {"fraOgMed": "2025-05-02", "tilOgMed": "2025-05-31"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2025-05-02", "tilOgMed": "2025-05-31"},
                        "antallBarn": 1,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 55,
                        "total": 353
                      }
                    ]
                  }
                ]
                """
        }
    }

    @Test
    fun `sisteKjenteSatsår - sendes med når vedtaket varer forbi året siste sats trådte i kraft`() {
        runTest {
            val actual = genererInnvilgetSøknadsbrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                tilleggstekst = null,
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.november(2026), 31.januar(2027))),
                barnetillegg = null,
                forhåndsvisning = true,
                vedtaksdato = 31.oktober(2026),
            )

            JSONObject(actual).getInt("sisteKjenteSatsår") shouldBe 2026
        }
    }
}
