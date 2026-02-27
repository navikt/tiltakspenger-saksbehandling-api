package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

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
    rammebehandlingId: String? = null,
    ventestatus: String? = null,
    hjemler: List<String> = emptyList(),
    iverksattOpprettholdelseTidspunkt: Boolean = false,
    journalføringstidspunktInnstillingsbrev: Boolean = false,
    distribusjonstidspunktInnstillingsbrev: Boolean = false,
    oversendtKlageinstansenTidspunkt: Boolean = false,
    klageinstanshendelser: List<String> = emptyList(),
    ferdigstiltTidspunkt: Boolean = false,
) {
    val expected =
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
                      "type": "OPPRETTHOLDT"
                    }
                """.trimIndent()

                "AVVIST" -> """
                    {
                      "type": "AVVIST",
                      "brevtekst": ${if (brevtekst.isEmpty()) "[]" else "[${brevtekst.joinToString()}]"}
                    }
                """.trimIndent()

                "OMGJØR" -> """
                    {
                      "type": "OMGJØR",
                      "årsak": ${årsak.toJsonValue()},
                      "begrunnelse": ${begrunnelse.toJsonValue()},
                      "rammebehandlingId": ${rammebehandlingId.toJsonValue()},
                      "brevtekst": ${if (brevtekst.isEmpty()) "[]" else "[${brevtekst.joinToString()}]"}
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
