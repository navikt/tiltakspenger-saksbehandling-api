package no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettRammebehandling

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import org.junit.jupiter.api.Test

class OpprettRammebehandlingFraKlageRouteTest {
    @Test
    fun `kan opprette søknadsbehandling for klagebehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            søknadsbehandling as Søknadsbehandling
            klagebehandling shouldBe Klagebehandling(
                id = klagebehandling.id,
                sakId = sak.id,
                opprettet = klagebehandling.opprettet,
                sistEndret = klagebehandling.sistEndret,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                journalpostId = JournalpostId("12345"),
                journalpostOpprettet = klagebehandling.journalpostOpprettet,
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = Klagebehandlingsresultat.Omgjør(
                    årsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
                    begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
                    rammebehandlingId = søknadsbehandling.id,
                ),
                formkrav = KlageFormkrav(
                    erKlagerPartISaken = true,
                    klagesDetPåKonkreteElementerIVedtaket = true,
                    erKlagefristenOverholdt = true,
                    erKlagenSignert = true,
                    erUnntakForKlagefrist = null,
                    vedtakDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().id,
                ),
                iverksattTidspunkt = null,
                avbrutt = null,
            )
            json.toString().shouldEqualJson(
                """
              {
              "id": "${søknadsbehandling.id}",
              "status": "UNDER_BEHANDLING",
              "sakId": "${sak.id}",
              "saksnummer": "202505011001",
              "rammevedtakId": "null",
              "saksbehandler": "saksbehandlerKlagebehandling",
              "beslutter": null,
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                    "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2023-01-01",
                    "deltagelseTilOgMed": "2023-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "gjennomforingsprosent": null,
                    "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0"
                  }
                ],
                "periode": {
                  "fraOgMed": "2023-01-01",
                  "tilOgMed": "2023-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": [],
                "oppslagstidspunkt": "2025-05-01T01:02:35.456789"
              },
              "attesteringer": [],
              "vedtaksperiode": null,
              "fritekstTilVedtaksbrev": null,
              "begrunnelseVilkårsvurdering": null,
              "avbrutt": null,
              "sistEndret": "2025-05-01T01:02:34.456789",
              "iverksattTidspunkt": null,
              "ventestatus": null,
              "utbetaling": null,
              "resultat": "IKKE_VALGT",
              "søknad": {
                "id": "${søknadsbehandling.søknad.id}",
                "journalpostId": "123456789",
                "tiltak": {
                  "id": "61328250-7d5d-4961-b70e-5cb727a34371",
                  "fraOgMed": "2023-01-01",
                  "tilOgMed": "2023-03-31",
                  "typeKode": "GRUPPEAMO",
                  "typeNavn": "Arbeidsmarkedsoppfølging gruppe"
                },
                "tiltaksdeltakelseperiodeDetErSøktOm": {
                  "fraOgMed": "2023-01-01",
                  "tilOgMed": "2023-03-31"
                },
                "manueltSattTiltak": null,
                "søknadstype": "DIGITAL",
                "barnetillegg": [],
                "opprettet": "2023-01-01T00:00:00",
                "tidsstempelHosOss": "2023-01-01T00:00:00",
                "antallVedlegg": 0,
                "avbrutt": null,
                "kanInnvilges": true,
                "svar": {
                  "harSøktPåTiltak": {
                    "svar": "JA"
                  },
                  "harSøktOmBarnetillegg": {
                    "svar": "NEI"
                  },
                  "kvp": {
                    "svar": "NEI",
                    "periode": null
                  },
                  "intro": {
                    "svar": "NEI",
                    "periode": null
                  },
                  "institusjon": {
                    "svar": "NEI",
                    "periode": null
                  },
                  "etterlønn": {
                    "svar": "NEI"
                  },
                  "gjenlevendepensjon": {
                    "svar": "NEI",
                    "periode": null
                  },
                  "alderspensjon": {
                    "svar": "NEI",
                    "fraOgMed": null
                  },
                  "sykepenger": {
                    "svar": "NEI",
                    "periode": null
                  },
                  "supplerendeStønadAlder": {
                    "svar": "NEI",
                    "periode": null
                  },
                  "supplerendeStønadFlyktning": {
                    "svar": "NEI",
                    "periode": null
                  },
                  "jobbsjansen": {
                    "svar": "NEI",
                    "periode": null
                  },
                  "trygdOgPensjon": {
                    "svar": "NEI",
                    "periode": null
                  }
                }
              },
              "automatiskSaksbehandlet": false,
              "manueltBehandlesGrunner": [],
              "kanInnvilges": true,
              "type": "SØKNADSBEHANDLING"
              }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan opprette revurdering innvilgelse for klagebehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, revurdering, klagebehandling, json) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                type = "REVURDERING_INNVILGELSE",
            )!!
            revurdering as Revurdering
            klagebehandling shouldBe Klagebehandling(
                id = klagebehandling.id,
                sakId = sak.id,
                opprettet = klagebehandling.opprettet,
                sistEndret = klagebehandling.sistEndret,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                journalpostId = JournalpostId("12345"),
                journalpostOpprettet = klagebehandling.journalpostOpprettet,
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = Klagebehandlingsresultat.Omgjør(
                    årsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
                    begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
                    rammebehandlingId = revurdering.id,
                ),
                formkrav = KlageFormkrav(
                    erKlagerPartISaken = true,
                    klagesDetPåKonkreteElementerIVedtaket = true,
                    erKlagefristenOverholdt = true,
                    erKlagenSignert = true,
                    erUnntakForKlagefrist = null,
                    vedtakDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().id,
                ),
                iverksattTidspunkt = null,
                avbrutt = null,
            )
            json.toString().shouldEqualJson(
                """
              {
              "id": "${revurdering.id}",
              "status": "UNDER_BEHANDLING",
              "sakId": "${sak.id}",
              "saksnummer": "202505011001",
              "rammevedtakId": "null",
              "saksbehandler": "saksbehandlerKlagebehandling",
              "beslutter": null,
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                    "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2023-01-01",
                    "deltagelseTilOgMed": "2023-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "gjennomforingsprosent": null,
                    "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0"
                  }
                ],
                "periode": {
                  "fraOgMed": "2023-01-01",
                  "tilOgMed": "2023-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": [],
                "oppslagstidspunkt": "2025-05-01T01:02:34.456789"
              },
              "attesteringer": [],
              "vedtaksperiode": null,
              "fritekstTilVedtaksbrev": null,
              "begrunnelseVilkårsvurdering": null,
              "avbrutt": null,
              "sistEndret": "2025-05-01T01:02:38.456789",
              "iverksattTidspunkt": null,
              "ventestatus": null,
              "utbetaling": null,
              "innvilgelsesperioder": null,
              "barnetillegg": null,
              "resultat": "REVURDERING_INNVILGELSE",
              "type": "REVURDERING"
              }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan opprette omgjøring for klagebehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, revurdering, klagebehandling, json) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
            )!!
            revurdering as Revurdering
            klagebehandling shouldBe Klagebehandling(
                id = klagebehandling.id,
                sakId = sak.id,
                opprettet = klagebehandling.opprettet,
                sistEndret = klagebehandling.sistEndret,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                journalpostId = JournalpostId("12345"),
                journalpostOpprettet = klagebehandling.journalpostOpprettet,
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = Klagebehandlingsresultat.Omgjør(
                    årsak = KlageOmgjøringsårsak.PROSESSUELL_FEIL,
                    begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
                    rammebehandlingId = revurdering.id,
                ),
                formkrav = KlageFormkrav(
                    erKlagerPartISaken = true,
                    klagesDetPåKonkreteElementerIVedtaket = true,
                    erKlagefristenOverholdt = true,
                    erKlagenSignert = true,
                    erUnntakForKlagefrist = null,
                    vedtakDetKlagesPå = sak.vedtaksliste.rammevedtaksliste.first().id,
                ),
                iverksattTidspunkt = null,
                avbrutt = null,
            )
            json.toString().shouldEqualJson(
                """
              {
              "id": "${revurdering.id}",
              "status": "UNDER_BEHANDLING",
              "sakId": "${sak.id}",
              "saksnummer": "202505011001",
              "rammevedtakId": "null",
              "saksbehandler": "saksbehandlerKlagebehandling",
              "beslutter": null,
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                    "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2023-01-01",
                    "deltagelseTilOgMed": "2023-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "gjennomforingsprosent": null,
                    "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0"
                  }
                ],
                "periode": {
                  "fraOgMed": "2023-01-01",
                  "tilOgMed": "2023-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": [],
                "oppslagstidspunkt": "2025-05-01T01:02:34.456789"
              },
              "attesteringer": [],
              "vedtaksperiode": {
                "fraOgMed": "2023-01-01",
                "tilOgMed": "2023-03-31"
              },
              "fritekstTilVedtaksbrev": null,
              "begrunnelseVilkårsvurdering": null,
              "avbrutt": null,
              "sistEndret": "2025-05-01T01:02:38.456789",
              "iverksattTidspunkt": null,
              "ventestatus": null,
              "utbetaling": null,
              "innvilgelsesperioder": [
                {
                  "periode": {
                    "fraOgMed": "2023-01-01",
                    "tilOgMed": "2023-03-31"
                  },
                  "antallDagerPerMeldeperiode": 10,
                  "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0"
                }
              ],
              "barnetillegg": {
                "perioder": [
                  {
                    "antallBarn": 0,
                    "periode": {
                      "fraOgMed": "2023-01-01",
                      "tilOgMed": "2023-03-31"
                    }
                  }
                ],
                "begrunnelse": null
              },
              "omgjørVedtak": "${sak.vedtaksliste.rammevedtaksliste.first().id}",
              "resultat": "OMGJØRING",
              "type": "REVURDERING"
              }
                """.trimIndent(),
            )
        }
    }
}
