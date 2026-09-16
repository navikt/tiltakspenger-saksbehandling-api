package no.nav.tiltakspenger.saksbehandling.søknad

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdetTekst
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps

fun String.shouldBeSøknadDTO(
    ignorerTidspunkt: Boolean = true,
    søknadId: SøknadId,
    journalpostId: String = "123456789",
    tiltakId: String,
    tiltakFraOgMed: String = "2023-01-01",
    tiltakTilOgMed: String = "2023-03-31",
    tiltakTypeKode: String = "GRUPPEAMO",
    tiltakTypeNavn: String = "Arbeidsmarkedsoppfølging gruppe",
    manueltSattTiltak: String? = null,
    søknadstype: String = "DIGITAL",
    barnetillegg: List<String> = emptyList(),
    antallVedlegg: Int = 0,
    avbruttAv: String? = "Z12345",
    avbruttBegrunnelse: String? = "begrunnelse for avbryt søknad og/eller rammebehandling",
    /**
     * Historikken over avbrytelser og gjenåpninger.
     * Default speiler [avbruttAv]: en avbrutt søknad har nøyaktig én avbrudds-hendelse.
     */
    hendelserJson: String? = null,
    kanInnvilges: Boolean = true,
    behandlingsarsak: String? = null,
    opprettet: String = "TIMESTAMP",
    tidsstempelHosOss: String = "TIMESTAMP",
    svar: String = """
        {
          "harSøktPåTiltak": { "svar": "JA" },
          "harSøktOmBarnetillegg": { "svar": "NEI" },
          "kvp": { "svar": "NEI", "periode": null },
          "intro": { "svar": "NEI", "periode": null },
          "institusjon": { "svar": "NEI", "periode": null },
          "etterlønn": { "svar": "NEI" },
          "gjenlevendepensjon": { "svar": "NEI", "periode": null },
          "alderspensjon": { "svar": "NEI", "fraOgMed": null },
          "sykepenger": { "svar": "NEI", "periode": null },
          "supplerendeStønadAlder": { "svar": "NEI", "periode": null },
          "supplerendeStønadFlyktning": { "svar": "NEI", "periode": null },
          "jobbsjansen": { "svar": "NEI", "periode": null },
          "trygdOgPensjon": { "svar": "NEI", "periode": null }
        }
    """.trimIndent(),
) {
    val hendelserJsonEllerUtledet = hendelserJson ?: if (avbruttAv != null) {
        """[{
              "type": "AVBRUTT",
              "tidspunkt": "TIMESTAMP",
              "utførtAv": "$avbruttAv",
              "begrunnelse": ${ikkeSladdetTekst(avbruttBegrunnelse)}
            }]"""
    } else {
        "[]"
    }

    //language=json
    val expected = """
        {
          "id": "$søknadId",
          "journalpostId": "$journalpostId",
          "tiltak": {
            "id": "$tiltakId",
            "fraOgMed": "$tiltakFraOgMed",
            "tilOgMed": "$tiltakTilOgMed",
            "typeKode": "$tiltakTypeKode",
            "typeNavn": "$tiltakTypeNavn"
          },
          "tiltaksdeltakelseperiodeDetErSøktOm": {
            "fraOgMed": "$tiltakFraOgMed",
            "tilOgMed": "$tiltakTilOgMed"
          },
          "manueltSattTiltak": ${manueltSattTiltak?.let { "\"$it\"" }},
          "søknadstype": "$søknadstype",
          "barnetillegg": [${barnetillegg.joinToString(",")}],
          "opprettet": "$opprettet",
          "tidsstempelHosOss": "$tidsstempelHosOss",
          "antallVedlegg": $antallVedlegg,
          "avbrutt": $hendelserJsonEllerUtledet,
          "kanInnvilges": $kanInnvilges,
          "svar": $svar,
          "behandlingsarsak": ${behandlingsarsak?.let { "\"$it\"" }}
        }
    """.trimIndent()

    if (ignorerTidspunkt) {
        this.shouldEqualJsonIgnoringTimestamps(expected)
    } else {
        this.shouldEqualJson(expected)
    }
}
