package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat

fun String.shouldBeKlagebehandlingDTO(
    ignorerTidspunkt: Boolean = true,
    sakId: SakId,
    saksnummer: Saksnummer = Saksnummer("202501011001"),
    klagebehandlingId: KlagebehandlingId,
    fnr: String = "12345678912",
    iverksattTidspunkt: String? = null,
    saksbehandler: String? = "saksbehandlerKlagebehandling",
    journalpostId: String = "12345",
    status: String = "UNDER_BEHANDLING",
    resultat: String? = null,
    vedtakDetKlagesPå: String? = null,
    behandlingDetKlagesPå: String? = null,
    erKlagerPartISaken: Boolean = true,
    klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
    erKlagefristenOverholdt: Boolean = true,
    erUnntakForKlagefrist: String? = null,
    erKlagenSignert: Boolean = true,
    innsendingsdato: String = "2026-02-16",
    innsendingskilde: String = "DIGITAL",
    brevtekst: List<Any> = emptyList(),
    avbrutt: String? = null,
    kanIverksetteVedtak: Boolean? = false,
    kanIverksetteOpprettholdelse: Boolean = false,
    årsak: String? = null,
    begrunnelse: String? = null,
    rammebehandlingId: List<String>? = null,
    åpenRammebehandlingId: String? = null,
    ventestatus: List<String> = emptyList(),
    hjemler: List<String> = emptyList(),
    iverksattOpprettholdelseTidspunkt: Boolean = false,
    journalføringstidspunktInnstillingsbrev: Boolean = false,
    journalpostIdInnstillingsbrev: String? = null,
    dokumentInfoIder: List<String> = emptyList(),
    distribusjonstidspunktInnstillingsbrev: Boolean = false,
    oversendtKlageinstansenTidspunkt: Boolean = false,
    klageinstanshendelser: List<String> = emptyList(),
    ferdigstiltTidspunkt: Boolean = false,
    begrunnelseFerdigstilling: String? = null,
) {
    val expected =
        //language=json
        """
       {
         "id": "$klagebehandlingId",
         "sakId": "$sakId",
         "saksnummer": "$saksnummer",
         "fnr": "$fnr",
         "opprettet": "TIMESTAMP",
         "sistEndret": "TIMESTAMP",
         "iverksattTidspunkt": ${iverksattTidspunkt.toJsonValue()},
         "saksbehandler": ${saksbehandler.toJsonValue()},
         "klagensJournalpostId": "$journalpostId",
         "klagensJournalpostOpprettet": "TIMESTAMP",
         "status": "$status",
         "tilknyttedeRammebehandlingIder": ${if (rammebehandlingId.isNullOrEmpty()) "[]" else rammebehandlingId.map { "\"$it\"" }},
         "åpenRammebehandlingId": ${åpenRammebehandlingId?.toJsonValue()},
         "resultat": ${
            when (resultat) {
                "OPPRETTHOLDT" -> """
                    {
                      "brevtekst": ${if (brevtekst.isEmpty()) "[]" else "[${brevtekst.joinToString()}]"},
                      "hjemler": [ ${hjemler.joinToString { "\"$it\"" }} ],
                      "iverksattOpprettholdelseTidspunkt": ${if (iverksattOpprettholdelseTidspunkt) "\"TIMESTAMP\"" else "null"},
                      "journalføringstidspunktInnstillingsbrev":  ${if (journalføringstidspunktInnstillingsbrev) "\"TIMESTAMP\"" else "null"},
                      "distribusjonstidspunktInnstillingsbrev": ${if (distribusjonstidspunktInnstillingsbrev) "\"TIMESTAMP\"" else "null"},
                      "oversendtKlageinstansenTidspunkt": ${if (oversendtKlageinstansenTidspunkt) "\"TIMESTAMP\"" else "null"},
                      "klageinstanshendelser": [ ${klageinstanshendelser.joinToString()} ],
                      "ferdigstiltTidspunkt": ${if (ferdigstiltTidspunkt) "\"TIMESTAMP\"" else "null"},
                      "journalpostIdInnstillingsbrev": ${journalpostIdInnstillingsbrev?.let { "\"$it\"" }},
                      "dokumentInfoIder": ${dokumentInfoIder.map { "\"$it\"" }},
                      "type": "OPPRETTHOLDT",
                      "begrunnelseFerdigstilling": ${begrunnelseFerdigstilling.toJsonValue()}
                    }
                """.trimIndent()

                "AVVIST" -> """
                    {
                      "type": "AVVIST",
                      "brevtekst": ${if (brevtekst.isEmpty()) "[]" else "[${brevtekst.joinToString()}]"},
                      "begrunnelseFerdigstilling": ${begrunnelseFerdigstilling.toJsonValue()}
                    }
                """.trimIndent()

                "OMGJØR" -> """
                    {
                      "type": "OMGJØR",
                      "årsak": ${årsak.toJsonValue()},
                      "begrunnelse": ${begrunnelse.toJsonValue()},
                      "begrunnelseFerdigstilling": ${begrunnelseFerdigstilling.toJsonValue()},
                      "ferdigstiltTidspunkt": ${if (ferdigstiltTidspunkt) "\"TIMESTAMP\"" else "null"}
                    }
                """.trimIndent()

                null -> "null"

                else -> throw IllegalArgumentException("Ugyldig resultat: $resultat")
            }
        },
         "avbrutt": $avbrutt,
         "kanIverksetteVedtak": $kanIverksetteVedtak,
         "kanIverksetteOpprettholdelse": $kanIverksetteOpprettholdelse,
         "ventestatus": $ventestatus,
         "formkrav": {
           "vedtakDetKlagesPå": ${vedtakDetKlagesPå.toJsonValue()},
           "behandlingDetKlagesPå": ${behandlingDetKlagesPå.toJsonValue()},
           "erKlagerPartISaken": $erKlagerPartISaken,
           "klagesDetPåKonkreteElementerIVedtaket": $klagesDetPåKonkreteElementerIVedtaket,
           "erKlagefristenOverholdt": $erKlagefristenOverholdt,
           "erUnntakForKlagefrist": ${erUnntakForKlagefrist.toJsonValue()},
           "erKlagenSignert": $erKlagenSignert,
           "innsendingsdato": "$innsendingsdato",
           "innsendingskilde": "$innsendingskilde"
         }
       }
        """.trimIndent()
    if (ignorerTidspunkt) {
        this.shouldEqualJsonIgnoringTimestamps(expected)
    } else {
        this.shouldEqualJson(expected)
    }
}

private fun String?.toJsonValue(): String = if (this == null) "null" else "\"$this\""

fun String.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO(
    ignorerTidspunkt: Boolean = true,
    resultat: Klagebehandlingsresultat.Opprettholdt,
    sakId: SakId,
    saksnummer: Saksnummer = Saksnummer("202501011001"),
    klagebehandlingId: KlagebehandlingId,
    fnr: String = "12345678912",
    saksbehandler: String? = "saksbehandlerKlagebehandling",
    journalpostId: String = "12345",
    vedtakDetKlagesPå: String? = null,
    behandlingDetKlagesPå: String? = null,
    brevtekst: List<String> = listOf(
        """{"tittel":"Hva klagesaken gjelder","tekst":"Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"}""",
        """{"tittel":"Klagers anførsler","tekst":"<saksbehandler fyller ut>"}""",
        """{"tittel":"Vurdering av klagen","tekst":"<saksbehandler fyller ut>"}""",
    ),
    rammebehandlingId: List<String> = emptyList(),
    åpenRammebehandlingId: String? = null,
    hjemler: List<String> = listOf("ARBEIDSMARKEDSLOVEN_17"),
    klageinstanshendelser: List<String> = listOf(
        """
         {
          "klagehendelseId": "${resultat.klageinstanshendelser.single().klagehendelseId}",
          "klagebehandlingId": "$klagebehandlingId",
          "opprettet": "TIMESTAMP",
          "sistEndret": "TIMESTAMP",
          "eksternKlagehendelseId": "${resultat.klageinstanshendelser.single().eksternKlagehendelseId}",
          "avsluttetTidspunkt": "TIMESTAMP",
          "journalpostreferanser": [],
          "utfall": "STADFESTELSE",
          "hendelsestype": "KLAGEBEHANDLING_AVSLUTTET"
        }
        """.trimIndent(),
    ),
    begrunnelseFerdigstilling: String? = null,
) {
    val expected =
        //language=json
        """
       {
         "id": "$klagebehandlingId",
         "sakId": "$sakId",
         "saksnummer": "$saksnummer",
         "fnr": "$fnr",
         "opprettet": "TIMESTAMP",
         "sistEndret": "TIMESTAMP",
         "iverksattTidspunkt": null,
         "saksbehandler": ${saksbehandler.toJsonValue()},
         "klagensJournalpostId": "$journalpostId",
         "klagensJournalpostOpprettet": "TIMESTAMP",
         "status": "FERDIGSTILT",
         "tilknyttedeRammebehandlingIder": ${if (rammebehandlingId.isEmpty()) "[]" else rammebehandlingId.map { "\"$it\"" }},
         "åpenRammebehandlingId": ${åpenRammebehandlingId?.toJsonValue()},
         "resultat": {
            "brevtekst": [${brevtekst.joinToString()}],
            "hjemler": [ ${hjemler.joinToString { "\"$it\"" }} ],
            "iverksattOpprettholdelseTidspunkt": "TIMESTAMP",
            "journalføringstidspunktInnstillingsbrev":  "TIMESTAMP",
            "distribusjonstidspunktInnstillingsbrev": "TIMESTAMP",
            "oversendtKlageinstansenTidspunkt": "TIMESTAMP",
            "klageinstanshendelser": [ ${klageinstanshendelser.joinToString()} ],
            "ferdigstiltTidspunkt": "TIMESTAMP",
            "journalpostIdInnstillingsbrev": ${resultat.journalpostIdInnstillingsbrev.let { "\"$it\"" }},
            "dokumentInfoIder": ${resultat.dokumentInfoIder.map { "\"$it\"" }},
            "type": "OPPRETTHOLDT",
            "begrunnelseFerdigstilling": ${begrunnelseFerdigstilling.toJsonValue()}
         },
         "avbrutt": null,
         "kanIverksetteVedtak": null,
         "kanIverksetteOpprettholdelse": false,
         "ventestatus": [],
         "formkrav": {
           "vedtakDetKlagesPå": ${vedtakDetKlagesPå.toJsonValue()},
           "behandlingDetKlagesPå": ${behandlingDetKlagesPå.toJsonValue()},
           "erKlagerPartISaken": true,
           "klagesDetPåKonkreteElementerIVedtaket": true,
           "erKlagefristenOverholdt": true,
           "erUnntakForKlagefrist": null,
           "erKlagenSignert": true,
           "innsendingsdato": "2026-02-16",
           "innsendingskilde": "DIGITAL"
         }
       }
        """.trimIndent()

    if (ignorerTidspunkt) {
        this.shouldEqualJsonIgnoringTimestamps(expected)
    } else {
        this.shouldEqualJson(expected)
    }
}

fun String.shouldBeKlagevedtakJson(
    ignorerTidspunkt: Boolean = true,
    klagebehandlingId: KlagebehandlingId,
    sakId: SakId,
    saksnummer: Saksnummer = Saksnummer("202601011001"),
    vedtakDetKlagesPå: VedtakId?,
    behandlingDetKlagesPå: Ulid?,
) {
    val expected = """{
                  "id": "$klagebehandlingId",
                  "sakId": "$sakId",
                  "saksnummer": "$saksnummer",
                  "fnr": "12345678911",
                  "opprettet": "TIMESTAMP",
                  "sistEndret": "TIMESTAMP",
                  "iverksattTidspunkt": "TIMESTAMP",
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "klagensJournalpostId": "12345",
                  "klagensJournalpostOpprettet": "TIMESTAMP",
                  "status": "VEDTATT",
                  "resultat": {
                    "brevtekst": [
                      {"tittel":"Avvisning av klage","tekst":"Din klage er dessverre avvist."}
                    ],
                    "begrunnelseFerdigstilling": null,
                    "type": "AVVIST"
                  },
                  "avbrutt": null,
                  "kanIverksetteVedtak": false,
                  "kanIverksetteOpprettholdelse": false,
                  "ventestatus": [],
                  "formkrav": {
                    "vedtakDetKlagesPå": "$vedtakDetKlagesPå",
                    "behandlingDetKlagesPå": "$behandlingDetKlagesPå",
                    "erKlagerPartISaken": true,
                    "klagesDetPåKonkreteElementerIVedtaket": false,
                    "erKlagefristenOverholdt": true,
                    "erUnntakForKlagefrist": null,
                    "erKlagenSignert": true,
                    "innsendingsdato": "2026-02-16",
                    "innsendingskilde": "DIGITAL"
                  },
                  "tilknyttedeRammebehandlingIder": [],
                  "åpenRammebehandlingId": null
                }
    """.trimIndent()

    if (ignorerTidspunkt) {
        this.shouldEqualJsonIgnoringTimestamps(expected)
    } else {
        this.shouldEqualJson(expected)
    }
}
