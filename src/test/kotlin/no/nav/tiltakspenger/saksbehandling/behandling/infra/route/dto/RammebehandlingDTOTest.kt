package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.gyldigFnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyRammevedtakInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattRevurderingStans
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RammebehandlingDTOTest {
    private val behandlingId = BehandlingId.fromString("beh_01K8R3V8S9X8KGR8HDXXDXN9P3")
    private val sakId = SakId.fromString("sak_01K8QWMR1KZZB728K0F4RQG184")
    private val saksnummer = Saksnummer("202510291001")
    private val vedtakId = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
    private val søknadId = SøknadId.fromString("soknad_01K8QWMR32C2X5Y5T4N945BF9V")
    private val søknadTiltakId = "06872f2f-5ca4-453a-8d41-8e91e1f777a3"
    private val eksternTiltaksdeltakelseId = "f02e50df-d2ee-47f6-9afa-db66bd842bfd"
    private val eksternTiltaksgjennomføringsId = "68f04dee-11a9-4d69-84fd-1096a4264492"
    private val internTiltaksdeltakelseId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG")
    private val soknadstiltakInternTiltaksdeltakelseId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01KEXQCG2FZV0629GX7QM4W1DV")
    private val fnr = gyldigFnr()

    private val vedtaksperiode = Periode(
        fraOgMed = 1.januar(2025),
        tilOgMed = 31.mars(2025),
    )
    private val beregninger =
        MeldeperiodeBeregningerVedtatt.fraVedtaksliste(Vedtaksliste.empty())

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling med innvilgelse`() {
        val clock = fixedClock

        val behandling = nyVedtattSøknadsbehandling(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
            saksopplysningsperiode = vedtaksperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                    id = eksternTiltaksdeltakelseId,
                    tiltaksdeltakerId = internTiltaksdeltakelseId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = clock,
            ),
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
        {
          "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
          "status": "VEDTATT",
          "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
          "saksnummer": "202510291001",
          "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
          "klagebehandlingId": null,
          "saksbehandler": "Z12345",
          "beslutter": "B12345",
          "saksopplysninger": {
            "fødselsdato": "2001-01-01",
            "tiltaksdeltagelse": [
              {
                "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                "typeKode": "GRUPPE_AMO",
                "deltagelseFraOgMed": "2025-01-01",
                "deltagelseTilOgMed": "2025-03-31",
                "deltakelseStatus": "Deltar",
                "deltakelseProsent": 100.0,
                "antallDagerPerUke": 5.0,
                "kilde": "Komet",
                "gjennomforingsprosent": null,
                "internDeltakelseId": "tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"
              }
            ],
            "periode": {
              "fraOgMed": "2025-01-01",
              "tilOgMed": "2025-03-31"
            },
            "ytelser": [],
            "tiltakspengevedtakFraArena": [],
            "oppslagstidspunkt": "2025-01-01T01:02:03.456789"
          },
          "attesteringer": [
            {
              "endretAv": "B12345",
              "status": "GODKJENT",
              "begrunnelse": null,
              "endretTidspunkt": "2025-01-01T01:02:03.456789"
            }
          ],
          "vedtaksperiode": {
            "fraOgMed": "2025-01-01",
            "tilOgMed": "2025-03-31"
          },
          "fritekstTilVedtaksbrev": "nyBehandlingUnderBeslutning()",
          "begrunnelseVilkårsvurdering": "nyBehandlingUnderBeslutning()",
          "avbrutt": null,
          "sistEndret": "2025-01-01T01:02:03.456789",
          "iverksattTidspunkt": "2025-01-01T01:02:03.456789",
          "ventestatus": null,
          "utbetaling": null,
          "innvilgelsesperioder": [
            {
              "periode": {
                "fraOgMed": "2025-01-01",
                "tilOgMed": "2025-03-31"
              },
              "antallDagerPerMeldeperiode": 10,
              "internDeltakelseId": "tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"
            }
          ],
          "barnetillegg": {
            "perioder": [
              {
                "antallBarn": 0,
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                }
              }
            ],
            "begrunnelse": null
          },
          "resultat": "INNVILGELSE",
          "søknad": {
            "id": "soknad_01K8QWMR32C2X5Y5T4N945BF9V",
            "journalpostId": "journalpostId",
            "tiltak": {
              "id": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
              "fraOgMed": "2025-01-01",
              "tilOgMed": "2025-03-31",
              "typeKode": "GRUPPEAMO",
              "typeNavn": "Gruppe AMO"
            },
            "tiltaksdeltakelseperiodeDetErSøktOm": {
              "fraOgMed": "2025-01-01",
              "tilOgMed": "2025-03-31"
            },
            "manueltSattTiltak": null,
            "søknadstype": "DIGITAL",
            "barnetillegg": [],
            "opprettet": "2022-01-01T12:00:00",
            "tidsstempelHosOss": "2022-01-01T12:00:00",
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
            },
            "behandlingsarsak": null
          },
          "automatiskSaksbehandlet": false,
          "manueltBehandlesGrunner": [],
          "kanInnvilges": true,
          "type": "SØKNADSBEHANDLING"
        }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling med avslag`() {
        val clock = fixedClock

        val behandling = nyVedtattSøknadsbehandling(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingsresultatType.AVSLAG,
            avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
            saksopplysningsperiode = vedtaksperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                    id = søknadTiltakId,
                    tiltaksdeltakerId = soknadstiltakInternTiltaksdeltakelseId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = clock,
            ),
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
              "status": "VEDTATT",
              "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer": "202510291001",
              "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "klagebehandlingId": null,
              "saksbehandler": "Z12345",
              "beslutter": "B12345",
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                    "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2025-01-01",
                    "deltagelseTilOgMed": "2025-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "gjennomforingsprosent": null,
                    "internDeltakelseId": "tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"
                  }
                ],
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": [],
                "oppslagstidspunkt": "2025-01-01T01:02:03.456789"
              },
              "attesteringer": [
                {
                  "endretAv": "B12345",
                  "status": "GODKJENT",
                  "begrunnelse": null,
                  "endretTidspunkt": "2025-01-01T01:02:03.456789"
                }
              ],
              "vedtaksperiode": {
                "fraOgMed": "2025-01-01",
                "tilOgMed": "2025-03-31"
              },
              "fritekstTilVedtaksbrev": "nyBehandlingUnderBeslutning()",
              "begrunnelseVilkårsvurdering": "nyBehandlingUnderBeslutning()",
              "avbrutt": null,
              "sistEndret": "2025-01-01T01:02:03.456789",
              "iverksattTidspunkt": "2025-01-01T01:02:03.456789",
              "ventestatus": null,
              "utbetaling": null,
              "avslagsgrunner": [
                "DeltarIkkePåArbeidsmarkedstiltak"
              ],
              "resultat": "AVSLAG",
              "søknad": {
                "id": "soknad_01K8QWMR32C2X5Y5T4N945BF9V",
                "journalpostId": "journalpostId",
                "tiltak": {
                  "id": "06872f2f-5ca4-453a-8d41-8e91e1f777a3",
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31",
                  "typeKode": "GRUPPEAMO",
                  "typeNavn": "Gruppe AMO"
                },
                "manueltSattTiltak": null,
                "tiltaksdeltakelseperiodeDetErSøktOm": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "barnetillegg": [],
                "søknadstype": "DIGITAL",
                "opprettet": "2022-01-01T12:00:00",
                "tidsstempelHosOss": "2022-01-01T12:00:00",
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
                },
                "behandlingsarsak": null
              },
              "automatiskSaksbehandlet": false,
              "manueltBehandlesGrunner": [],
              "type": "SØKNADSBEHANDLING",
              "kanInnvilges": false
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra søknadsbehandling uten valgt resultat`() {
        val clock = fixedClock

        val behandling = nyOpprettetSøknadsbehandling(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                    id = eksternTiltaksdeltakelseId,
                    tiltaksdeltakerId = internTiltaksdeltakelseId,
                ),
            ),
            hentSaksopplysninger = { _, _, _, _, _ ->
                saksopplysninger(
                    fom = vedtaksperiode.fraOgMed,
                    tom = vedtaksperiode.tilOgMed,
                    tiltaksdeltakelse = listOf(
                        tiltaksdeltakelse(
                            fom = vedtaksperiode.fraOgMed,
                            tom = vedtaksperiode.tilOgMed,
                            eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                            internDeltakelseId = internTiltaksdeltakelseId,
                        ),
                    ),
                    clock = clock,
                )
            },
        )

        val behandlingJson = serialize(
            behandling.tilSøknadsbehandlingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
              "status": "UNDER_BEHANDLING",
              "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer": "202510291001",
              "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "klagebehandlingId": null,
              "saksbehandler": "Z12345",
              "beslutter": null,
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                    "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2025-01-01",
                    "deltagelseTilOgMed": "2025-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "gjennomforingsprosent": null,
                    "internDeltakelseId": "tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"
                  }
                ],
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": [],
                "oppslagstidspunkt": "2025-01-01T01:02:03.456789"
              },
              "attesteringer": [],
              "vedtaksperiode": null,
              "fritekstTilVedtaksbrev": null,
              "begrunnelseVilkårsvurdering": null,
              "avbrutt": null,
              "sistEndret": "2025-01-01T01:02:03.456789",
              "iverksattTidspunkt": null,
              "ventestatus": null,
              "utbetaling": null,
              "resultat": "IKKE_VALGT",
              "søknad": {
                "id": "soknad_01K8QWMR32C2X5Y5T4N945BF9V",
                "journalpostId": "journalpostId",
                "tiltak": {
                  "id": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31",
                  "typeKode": "GRUPPEAMO",
                  "typeNavn": "Gruppe AMO"
                },
                "manueltSattTiltak": null,
                "tiltaksdeltakelseperiodeDetErSøktOm": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "barnetillegg": [],
                "søknadstype": "DIGITAL",
                "opprettet": "2022-01-01T12:00:00",
                "tidsstempelHosOss": "2022-01-01T12:00:00",
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
                },
                "behandlingsarsak": null
              },
              "automatiskSaksbehandlet": false,
              "manueltBehandlesGrunner": [],
              "type": "SØKNADSBEHANDLING",
              "kanInnvilges": true
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med innvilgelse`() {
        val clock = fixedClock

        val behandling = nyVedtattRevurderingInnvilgelse(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksopplysningsperiode = vedtaksperiode,
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = clock,
            ),
        )

        val behandlingJson = serialize(
            behandling.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
        {
          "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
          "status": "VEDTATT",
          "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
          "saksnummer": "202510291001",
          "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
          "klagebehandlingId": null,
          "saksbehandler": "Z12345",
          "beslutter": "B12345",
          "saksopplysninger": {
            "fødselsdato": "2001-01-01",
            "tiltaksdeltagelse": [
              {
                "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                "typeKode": "GRUPPE_AMO",
                "deltagelseFraOgMed": "2025-01-01",
                "deltagelseTilOgMed": "2025-03-31",
                "deltakelseStatus": "Deltar",
                "deltakelseProsent": 100.0,
                "antallDagerPerUke": 5.0,
                "kilde": "Komet",
                "gjennomforingsprosent": null,
                "internDeltakelseId": "tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"
              }
            ],
            "periode": {
              "fraOgMed": "2025-01-01",
              "tilOgMed": "2025-03-31"
            },
            "ytelser": [],
            "tiltakspengevedtakFraArena": [],
            "oppslagstidspunkt": "2025-01-01T01:02:03.456789"
          },
          "attesteringer": [
            {
              "endretAv": "B12345",
              "status": "GODKJENT",
              "begrunnelse": null,
              "endretTidspunkt": "2025-01-01T01:02:03.456789"
            }
          ],
          "vedtaksperiode": {
            "fraOgMed": "2025-01-01",
            "tilOgMed": "2025-03-31"
          },
          "fritekstTilVedtaksbrev": "nyRevurderingKlarTilBeslutning()",
          "begrunnelseVilkårsvurdering": "nyRevurderingKlarTilBeslutning()",
          "avbrutt": null,
          "sistEndret": "2025-01-01T01:02:03.456789",
          "iverksattTidspunkt": "2025-01-01T01:02:03.456789",
          "ventestatus": null,
          "utbetaling": null,
          "innvilgelsesperioder": [
            {
              "periode": {
                "fraOgMed": "2025-01-01",
                "tilOgMed": "2025-03-31"
              },
              "antallDagerPerMeldeperiode": 10,
              "internDeltakelseId": "tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"
            }
          ],
          "barnetillegg": {
            "perioder": [
              {
                "antallBarn": 0,
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                }
              }
            ],
            "begrunnelse": null
          },
          "resultat": "REVURDERING_INNVILGELSE",
          "type": "REVURDERING"
        }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med stans`() {
        val clock = fixedClock

        val behandling = nyVedtattRevurderingStans(
            clock = clock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            vedtaksperiode = vedtaksperiode,
            stansFraOgMed = vedtaksperiode.fraOgMed.plusDays(7),
            førsteDagSomGirRett = vedtaksperiode.fraOgMed,
            sisteDagSomGirRett = vedtaksperiode.tilOgMed,
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = clock,
            ),
        )

        val behandlingJson = serialize(
            behandling.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = vedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P3",
              "status": "VEDTATT",
              "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer": "202510291001",
              "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "klagebehandlingId": null,
              "saksbehandler": "Z12345",
              "beslutter": "B12345",
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                    "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2025-01-01",
                    "deltagelseTilOgMed": "2025-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "gjennomforingsprosent": null,
                    "internDeltakelseId": "tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"
                  }
                ],
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": [],
                "oppslagstidspunkt": "2025-01-01T01:02:03.456789"
              },
              "attesteringer": [
                {
                  "endretAv": "B12345",
                  "status": "GODKJENT",
                  "begrunnelse": null,
                  "endretTidspunkt": "2025-01-01T01:02:03.456789"
                }
              ],
              "vedtaksperiode": {
                "fraOgMed": "2025-01-08",
                "tilOgMed": "2025-03-31"
              },
              "fritekstTilVedtaksbrev": "nyRevurderingKlarTilBeslutning()",
              "begrunnelseVilkårsvurdering": "nyRevurderingKlarTilBeslutning()",
              "avbrutt": null,
              "sistEndret": "2025-01-01T01:02:03.456789",
              "iverksattTidspunkt": "2025-01-01T01:02:03.456789",
              "ventestatus": null,
              "utbetaling": null,
              "valgtHjemmelHarIkkeRettighet": [
                "DeltarIkkePåArbeidsmarkedstiltak"
              ],
              "harValgtStansFraFørsteDagSomGirRett": false,
              "resultat": "STANS",
              "type": "REVURDERING"
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }

    @Test
    fun `Rammebehandling DTO fra revurdering med omgjøring uten valgt resultat`() {
        val nyClock = fixedClock

        val behandling = nyVedtattSøknadsbehandling(
            clock = nyClock,
            id = behandlingId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
            saksopplysningsperiode = vedtaksperiode,
            søknad = nyInnvilgbarSøknad(
                id = søknadId,
                søknadstiltak = søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                    id = søknadTiltakId,
                    tiltaksdeltakerId = soknadstiltakInternTiltaksdeltakelseId,
                ),
            ),
            saksopplysninger = saksopplysninger(
                fom = vedtaksperiode.fraOgMed,
                tom = vedtaksperiode.tilOgMed,
                tiltaksdeltakelse = listOf(
                    tiltaksdeltakelse(
                        fom = vedtaksperiode.fraOgMed,
                        tom = vedtaksperiode.tilOgMed,
                        eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                        eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                        internDeltakelseId = internTiltaksdeltakelseId,
                    ),
                ),
                clock = nyClock,
            ),
        )

        val omgjøringId = BehandlingId.fromString("beh_01K8R3V8S9X8KGR8HDXXDXN9P4")
        val omgjøringVedtakId = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S7")

        val omgjøring = nyOpprettetRevurderingOmgjøring(
            clock = nyClock,
            id = omgjøringId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            søknadsbehandlingInnvilgelsesperiode = vedtaksperiode.plusFraOgMed(1),
            vedtattInnvilgetSøknadsbehandling = nyRammevedtakInnvilgelse(
                id = vedtakId,
                sakId = sakId,
                innvilgelsesperioder = nonEmptyListOf(
                    innvilgelsesperiodeKommando(innvilgelsesperiode = ObjectMother.vedtaksperiode()),
                ),
                fnr = fnr,
                behandling = behandling,
            ),
            hentSaksopplysninger = {
                saksopplysninger(
                    fom = vedtaksperiode.fraOgMed,
                    tom = vedtaksperiode.tilOgMed,
                    tiltaksdeltakelse = listOf(
                        tiltaksdeltakelse(
                            fom = vedtaksperiode.fraOgMed,
                            tom = vedtaksperiode.tilOgMed,
                            eksternTiltaksdeltakelseId = eksternTiltaksdeltakelseId,
                            eksternTiltaksgjennomføringsId = eksternTiltaksgjennomføringsId,
                            internDeltakelseId = internTiltaksdeltakelseId,
                        ),
                    ),
                    clock = nyClock,
                )
            },
        )

        val behandlingJson = serialize(
            omgjøring.tilRevurderingDTO(
                utbetalingsstatus = null,
                beregninger = beregninger,
                rammevedtakId = omgjøringVedtakId,
            ),
        )

        @Language("JSON")
        val expectedJson = """
            {
              "id": "beh_01K8R3V8S9X8KGR8HDXXDXN9P4",
              "status": "UNDER_BEHANDLING",
              "sakId": "sak_01K8QWMR1KZZB728K0F4RQG184",
              "saksnummer": "202510291001",
              "rammevedtakId": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S7",
              "saksbehandler": "Z12345",
              "beslutter": null,
              "saksopplysninger": {
                "fødselsdato": "2001-01-01",
                "tiltaksdeltagelse": [
                  {
                    "eksternDeltagelseId": "f02e50df-d2ee-47f6-9afa-db66bd842bfd",
                    "gjennomføringId": "68f04dee-11a9-4d69-84fd-1096a4264492",
                    "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                    "typeKode": "GRUPPE_AMO",
                    "deltagelseFraOgMed": "2025-01-01",
                    "deltagelseTilOgMed": "2025-03-31",
                    "deltakelseStatus": "Deltar",
                    "deltakelseProsent": 100.0,
                    "antallDagerPerUke": 5.0,
                    "kilde": "Komet",
                    "gjennomforingsprosent": null,
                    "internDeltakelseId": "tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"
                  }
                ],
                "periode": {
                  "fraOgMed": "2025-01-01",
                  "tilOgMed": "2025-03-31"
                },
                "ytelser": [],
                "tiltakspengevedtakFraArena": [],
                "oppslagstidspunkt": "2025-01-01T01:02:03.456789"
              },
              "attesteringer": [],
              "vedtaksperiode": null,
              "fritekstTilVedtaksbrev": null,
              "begrunnelseVilkårsvurdering": null,
              "avbrutt": null,
              "sistEndret": "2025-01-01T01:02:03.456789",
              "iverksattTidspunkt": null,
              "ventestatus": null,
              "utbetaling": null,
              "klagebehandlingId": null,
              "omgjørVedtak": "vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6",
              "resultat": "OMGJØRING_IKKE_VALGT",
              "type": "REVURDERING"
            }
        """.trimIndent()

        behandlingJson.shouldEqualJson(expectedJson)
    }
}
