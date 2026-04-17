package no.nav.tiltakspenger.saksbehandling.behandling

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

fun String.shouldBeRevurderingDTO(
    ignorerTidspunkt: Boolean = true,
    sakId: SakId,
    saksnummer: Saksnummer = Saksnummer("202501011001"),
    behandlingId: BehandlingId,
    klagebehandlingId: KlagebehandlingId,
    omgjørVedtak: VedtakId? = null,
    rammevedtakId: VedtakId?,
    avbrutt: String? = null,
    attesteringer: List<String> = listOf(
        """{
              "begrunnelse": null,
              "endretAv": "B12345",
              "endretTidspunkt": "2025-01-01T01:03:04.456789",
              "status": "GODKJENT"
            }""",
    ),
    saksbehandler: String = "saksbehandlerKlagebehandling",
    utbetalingskontroll: String? = null,
    barnetilleggBegrunnelse: String? = null,
    antallBarn: Int = 0,
    barnetillegg: Boolean = true,
    iverksattTidspunkt: String? = "2025-01-01T01:03:05.456789",
    vedtaksperiode: String? = """{"fraOgMed": "2023-01-01","tilOgMed": "2023-03-31"}""",
    fritekstTilVedtaksbrev: String? = null,
    resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.OMGJØRING,
    automatiskOpprettetGrunn: String? = null,
    beslutter: String? = "B12345",
    begrunnelseVilkårsvurdering: String? = null,
    tilbakekrevingId: String? = null,
    utbetaling: String? = null,
    ventestatus: String? = null,
    internDeltakelseId: String = "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
    innvilgelsesperiode: Boolean = true,
    antallDagerPerMeldeperiode: Int = 10,
    sistEndret: String = "2025-01-01T01:03:05.456789",
    status: String = "VEDTATT",
    skalSendeVedtaksbrev: Boolean = true,
) {
    val expected = """
        {
          "id": "$behandlingId",
          "sakId": "$sakId",
          "saksnummer": "$saksnummer",
          "klagebehandlingId": "$klagebehandlingId",
          "avbrutt": ${avbrutt?.let { "\"$it\"" } ?: "null"},
          "attesteringer": $attesteringer,
          "saksbehandler": "$saksbehandler",
          "utbetalingskontroll": ${utbetalingskontroll?.let { "\"$it\"" } ?: "null"},
          ${
        if (resultat != RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT) {
            """
                  "barnetillegg": ${
                if (barnetillegg) {
                    """{
                "begrunnelse": ${barnetilleggBegrunnelse?.let { "\"$it\"" }},
                "perioder": [
                    {
                        "antallBarn": $antallBarn,
                        "periode": {
                            "fraOgMed": "2023-01-01",
                            "tilOgMed": "2023-03-31"
                        }
                    }
                ]
            }"""
                } else {
                    null
                }
            },
            """.trimIndent()
        } else {
            ""
        }
    }
          "iverksattTidspunkt": ${iverksattTidspunkt?.let { "\"$it\"" }},
          "vedtaksperiode": $vedtaksperiode,
          "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.let { "\"$it\"" }},
          "resultat": "$resultat",
          "automatiskOpprettetGrunn": ${automatiskOpprettetGrunn?.let { "\"$it\"" }},
          "type": "REVURDERING",
          "beslutter": ${beslutter?.let { "\"$it\"" }},
          "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.let { "\"$it\"" }},
          "tilbakekrevingId": ${tilbakekrevingId?.let { "\"$it\"" }},
          "utbetaling": ${utbetaling?.let { "\"$it\"" }},
          "ventestatus": ${ventestatus?.let { "\"$it\"" }},
          ${
        if (resultat != RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT) {
            """"innvilgelsesperioder": ${
                if (innvilgelsesperiode) {
                    """
                [
            {
                "internDeltakelseId": "$internDeltakelseId",
                "periode": {
                "fraOgMed": "2023-01-01",
                "tilOgMed": "2023-03-31"
                },
                "antallDagerPerMeldeperiode": $antallDagerPerMeldeperiode
            }
            ]
                    """.trimIndent()
                } else {
                    null
                }

            },"""
        } else {
            ""
        }
    }
          ${
        if (resultat == RammebehandlingResultatTypeDTO.OMGJØRING || resultat == RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT) {
            """"omgjørVedtak": "$omgjørVedtak","""
        } else {
            ""
        }
    }
          "saksopplysninger": {
            "oppslagstidspunkt": "2025-01-01T01:02:52.456789",
            "tiltaksdeltagelse": [
              {
                "typeKode": "GRUPPE_AMO",
                "gjennomforingsprosent": null,
                "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                "antallDagerPerUke": 5,
                "deltakelseStatus": "Deltar",
                "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                "deltagelseFraOgMed": "2023-01-01",
                "deltagelseTilOgMed": "2023-03-31",
                "kilde": "Komet",
                "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                "deltakelseProsent": 100
              }
            ],
            "fødselsdato": "2001-01-01",
            "ytelser": [],
            "tiltakspengevedtakFraArena": [],
            "periode": {
              "fraOgMed": "2023-01-01",
              "tilOgMed": "2023-03-31"
            }
          },
          "rammevedtakId": ${rammevedtakId?.let { "\"$it\"" }},
          "opprettet": "TIMESTAMP",
          "sistEndret": "$sistEndret",
          "status": "$status",
          "skalSendeVedtaksbrev": $skalSendeVedtaksbrev
        }
    """

    if (ignorerTidspunkt) {
        this.shouldEqualJsonIgnoringTimestamps(expected)
    } else {
        this.shouldEqualJson(expected)
    }
}

fun String.shouldBeSøknadsbehandlingDTO(
    ignorerTidspunkt: Boolean = true,
    sakId: SakId,
    saksnummer: Saksnummer = Saksnummer("202501011001"),
    behandlingId: BehandlingId,
    klagebehandlingId: KlagebehandlingId,
    rammevedtakId: VedtakId? = null,
    søknadId: SøknadId,
    vedtaksperiode: String? = """{"fraOgMed": "2023-01-01","tilOgMed": "2023-03-31"}""",
    iverksattTidspunkt: String? = "2025-01-01T01:03:06.456789",
    attesteringer: List<String> = emptyList(),
    saksbehandler: String = "saksbehandlerKlagebehandling",
    utbetalingskontroll: String? = null,
    barnetilleggBegrunnelse: String? = null,
    antallBarn: Int = 0,
    barnetillegg: Boolean = true,
    fritekstTilVedtaksbrev: String? = null,
    resultat: RammebehandlingResultatTypeDTO = RammebehandlingResultatTypeDTO.INNVILGELSE,
    beslutter: String? = "B12345",
    begrunnelseVilkårsvurdering: String? = null,
    tilbakekrevingId: String? = null,
    utbetaling: String? = null,
    ventestatus: String? = null,
    internDeltakelseId: String = "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
    innvilgelsesperiode: Boolean = true,
    antallDagerPerMeldeperiode: Int = 10,
    sistEndret: String = "2025-01-01T01:03:06.456789",
    status: String = "VEDTATT",
    skalSendeVedtaksbrev: Boolean = true,
    automatiskSaksbehandlet: Boolean = false,
    kanInnvilges: Boolean = true,
    manueltBehandlesGrunner: List<String> = emptyList(),
) {
    val expected = """
        {
          "attesteringer": [
            ${attesteringer.joinToString(",") { """{"begrunnelse": null, "endretAv": "B12345", "endretTidspunkt": "2025-01-01T01:03:04.456789", "status": "GODKJENT"}""" }}
          ],
          "saksnummer": "$saksnummer",
          "utbetalingskontroll": ${utbetalingskontroll?.let { "\"$it\"" }},
          "iverksattTidspunkt": ${iverksattTidspunkt?.let { "\"$it\"" }},
          "vedtaksperiode": $vedtaksperiode,
          "type": "SØKNADSBEHANDLING",
          "utbetaling": ${utbetaling?.let { "\"$it\"" }},
          "manueltBehandlesGrunner": $manueltBehandlesGrunner,
          "saksopplysninger": {
            "oppslagstidspunkt": "2025-01-01T01:02:52.456789",
            "tiltaksdeltagelse": [
              {
                "typeKode": "GRUPPE_AMO",
                "gjennomforingsprosent": null,
                "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                "antallDagerPerUke": 5,
                "deltakelseStatus": "Deltar",
                "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                "deltagelseFraOgMed": "2023-01-01",
                "deltagelseTilOgMed": "2023-03-31",
                "kilde": "Komet",
                "internDeltakelseId": "$internDeltakelseId",
                "deltakelseProsent": 100
              }
            ],
            "fødselsdato": "2001-01-01",
            "ytelser": [],
            "tiltakspengevedtakFraArena": [],
            "periode": {
                "fraOgMed": "2023-01-01",
                "tilOgMed": "2023-03-31"
            }
          },
          "sakId": "$sakId",
          "id": "$behandlingId",
          "avbrutt": null,
          "saksbehandler": "$saksbehandler",
          ${
        if (barnetillegg) {
            """
            "barnetillegg": {
                "begrunnelse": ${barnetilleggBegrunnelse?.let { "\"$it\"" }},
                "perioder": [
                    {
                        "antallBarn": $antallBarn,
                        "periode": {
                            "fraOgMed": "2023-01-01",
                            "tilOgMed": "2023-03-31"
                        }
                    }
                ]
            },
            """.trimIndent()
        } else {
            ""
        }
    }
          "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.let { "\"$it\"" }},
          "resultat": "$resultat",
          "beslutter": ${beslutter?.let { "\"$it\"" }},
          "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.let { "\"$it\"" }},
          "klagebehandlingId": "$klagebehandlingId",
          "tilbakekrevingId": ${tilbakekrevingId?.let { "\"$it\"" }},
          "kanInnvilges": $kanInnvilges,
          "ventestatus": ${ventestatus?.let { "\"$it\"" }},
          ${
        if (innvilgelsesperiode) {
            """"innvilgelsesperioder": [
            {
                "internDeltakelseId": "$internDeltakelseId",
                "periode": {
                "fraOgMed": "2023-01-01",
                "tilOgMed": "2023-03-31"
                },
                "antallDagerPerMeldeperiode": $antallDagerPerMeldeperiode
            }
            ],"""
        } else {
            ""
        }
    }
        "rammevedtakId": ${rammevedtakId?.let { "\"$it\"" }},
        "opprettet": "TIMESTAMP",
        "sistEndret": "$sistEndret",
        "automatiskSaksbehandlet": $automatiskSaksbehandlet,
        "søknad": {
            "avbrutt": null,
            "svar": {
            "harSøktPåTiltak": { "svar": "JA" },
            "kvp": { "svar": "NEI", "periode": null },
            "gjenlevendepensjon": { "svar": "NEI", "periode": null },
            "harSøktOmBarnetillegg": { "svar": "NEI" },
            "sykepenger": { "svar": "NEI", "periode": null },
            "etterlønn": { "svar": "NEI" },
            "institusjon": { "svar": "NEI", "periode": null },
            "trygdOgPensjon": { "svar": "NEI", "periode": null },
            "intro": { "svar": "NEI", "periode": null },
            "supplerendeStønadAlder": { "svar": "NEI", "periode": null },
            "jobbsjansen": { "svar": "NEI", "periode": null },
            "alderspensjon": { "svar": "NEI", "fraOgMed": null },
            "supplerendeStønadFlyktning": { "svar": "NEI", "periode": null }
        },
            "tiltaksdeltakelseperiodeDetErSøktOm": {
            "fraOgMed": "2023-01-01",
            "tilOgMed": "2023-03-31"
        },
            "barnetillegg": [],
            "opprettet": "2023-01-01T00:00:00",
            "antallVedlegg": 0,
            "tiltak": {
            "fraOgMed": "2023-01-01",
            "typeKode": "GRUPPEAMO",
            "tilOgMed": "2023-03-31",
            "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
            "id": "61328250-7d5d-4961-b70e-5cb727a34371"
        },
            "manueltSattTiltak": null,
            "søknadstype": "DIGITAL",
            "behandlingsarsak": null,
            "kanInnvilges": true,
            "tidsstempelHosOss": "2023-01-01T00:00:00",
            "id": "$søknadId",
            "journalpostId": "123456789"
        },
        "status": "$status",
        "skalSendeVedtaksbrev": $skalSendeVedtaksbrev
    }
    """
    if (ignorerTidspunkt) {
        this.shouldEqualJsonIgnoringTimestamps(expected)
    } else {
        this.shouldEqualJson(expected)
    }
}
