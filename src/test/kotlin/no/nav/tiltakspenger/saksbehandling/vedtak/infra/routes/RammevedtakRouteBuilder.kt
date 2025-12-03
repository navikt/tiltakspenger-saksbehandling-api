package no.nav.tiltakspenger.saksbehandling.vedtak.infra.routes

import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.infra.route.RammevedtakDTOJson

/**
 * Kaster assert-error hvis den ikke er lik [no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.RammevedtakDTO]
 */
fun RammevedtakDTOJson.shouldBeEqualToRammevedtakDTOinnvilgelse(
    id: String,
    behandlingId: String,
    opprettet: String = "2025-01-01T01:02:18.456789",
    saksbehandler: String = "Z12345",
    beslutter: String = "B12345",
    opprinneligVedtaksperiode: Periode = 1.april(2025) til 10.april(2025),
    gjeldendeVedtaksperioder: List<Periode> = listOf(opprinneligVedtaksperiode),
    opprinneligInnvilgetPerioder: List<Periode> = listOf(opprinneligVedtaksperiode),
    gjeldendeInnvilgetPerioder: List<Periode> = gjeldendeVedtaksperioder,

    erGjeldende: Boolean = true,
    antallDagerPerMeldeperiode: Int = 10,
    resultat: String = "INNVILGELSE",
    barnetillegg: String? = """
        {
            "begrunnelse": null,
            "perioder": [
              {
                "antallBarn": 0,
                "periode": {
                  "fraOgMed": "2025-04-01",
                  "tilOgMed": "2025-04-10"
                }
              }
            ]
          }
    """.trimIndent(),
    vedtaksdato: String? = null,
) {
    shouldBeEqualToRammevedtakDTO(
        erGjeldende = erGjeldende,
        saksbehandler = saksbehandler,
        opprettet = opprettet,
        barnetillegg = barnetillegg,
        gjeldendeVedtaksperioder = gjeldendeVedtaksperioder,
        resultat = resultat,
        gjeldendeInnvilgetPerioder = gjeldendeInnvilgetPerioder,
        beslutter = beslutter,
        opprinneligVedtaksperiode = opprinneligVedtaksperiode,
        behandlingId = behandlingId,
        id = id,
        vedtaksdato = vedtaksdato,
        opprinneligInnvilgetPerioder = opprinneligInnvilgetPerioder,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
    )
}

/**
 * Kaster assert-error hvis den ikke er lik [no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.RammevedtakDTO]
 */
fun RammevedtakDTOJson.shouldBeEqualToRammevedtakDTOavslag(
    id: String,
    behandlingId: String,
    opprettet: String = "2025-01-01T01:02:18.456789",
    saksbehandler: String = "Z12345",
    beslutter: String = "B12345",
    opprinneligVedtaksperiode: Periode = 1.april(2025) til 10.april(2025),
    vedtaksdato: String? = null,
) {
    shouldBeEqualToRammevedtakDTO(
        erGjeldende = false,
        saksbehandler = saksbehandler,
        opprettet = opprettet,
        barnetillegg = null,
        gjeldendeVedtaksperioder = emptyList(),
        resultat = "AVSLAG",
        gjeldendeInnvilgetPerioder = emptyList(),
        beslutter = beslutter,
        opprinneligVedtaksperiode = opprinneligVedtaksperiode,
        behandlingId = behandlingId,
        id = id,
        vedtaksdato = vedtaksdato,
        opprinneligInnvilgetPerioder = emptyList(),
        antallDagerPerMeldeperiode = 0,
    )
}

/**
 * Kaster assert-error hvis den ikke er lik [no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.RammevedtakDTO]
 */
fun RammevedtakDTOJson.shouldBeEqualToRammevedtakDTO(
    id: String,
    behandlingId: String,
    opprettet: String,
    saksbehandler: String,
    beslutter: String,
    opprinneligVedtaksperiode: Periode,
    gjeldendeVedtaksperioder: List<Periode>,
    opprinneligInnvilgetPerioder: List<Periode>,
    gjeldendeInnvilgetPerioder: List<Periode>,
    erGjeldende: Boolean,
    resultat: String,
    vedtaksdato: String?,
    barnetillegg: String?,
    antallDagerPerMeldeperiode: Int,
) {
    this.toString().shouldEqualJson {
        fieldComparison = FieldComparison.Strict
        propertyOrder = PropertyOrder.Lenient
        numberFormat = NumberFormat.Strict
        // language=JSON
        """
            {
              "id": "$id",
              "behandlingId": "$behandlingId",
              "opprettet": "$opprettet",
              "erGjeldende": $erGjeldende,
              "saksbehandler": "$saksbehandler",
              "barnetillegg": $barnetillegg,
              "gjeldendeVedtaksperioder": ${gjeldendeVedtaksperioder.map {
            """{
                    "fraOgMed": "${it.fraOgMed}",
                    "tilOgMed": "${it.tilOgMed}"
                }"""
        }},
              "resultat": "$resultat",
              "gjeldendeInnvilgetPerioder": ${gjeldendeInnvilgetPerioder.map {
            """{
                    "fraOgMed": "${it.fraOgMed}",
                    "tilOgMed": "${it.tilOgMed}"
                }"""
        }
        },
              "beslutter": "$beslutter",
              "opprinneligVedtaksperiode": {
                "fraOgMed": "${opprinneligVedtaksperiode.fraOgMed}",
                "tilOgMed": "${opprinneligVedtaksperiode.tilOgMed}"
              },
              "vedtaksdato": ${vedtaksdato?.let { "\"$vedtaksdato\"" } },
              "opprinneligInnvilgetPerioder": ${opprinneligInnvilgetPerioder.map {
            """{
                    "fraOgMed": "${it.fraOgMed}",
                    "tilOgMed": "${it.tilOgMed}"
                }"""
        }
        },
              "antallDagerPerMeldeperiode": $antallDagerPerMeldeperiode
            }
        """.trimIndent()
    }
}
