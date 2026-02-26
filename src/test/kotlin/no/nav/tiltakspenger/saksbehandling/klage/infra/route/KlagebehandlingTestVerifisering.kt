package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDateTime

fun String.shouldBeKlagebehandlingDTO(
    ignorerTidspunkt: Boolean = true,
    sakId: SakId,
    saksnummer: Saksnummer = Saksnummer("202501011001"),
    klagebehandlingId: KlagebehandlingId,
    opprettet: LocalDateTime = LocalDateTime.parse("2025-01-01T01:02:07.456789"),
    sistEndret: LocalDateTime = LocalDateTime.parse("2025-01-01T01:02:07.456789"),
    fnr: String = "12345678912",
    iverksattTidspunkt: String? = null,
    saksbehandler: String? = "saksbehandlerKlagebehandling",
    journalpostId: String = "12345",
    journalpostOpprettet: LocalDateTime = LocalDateTime.parse("2025-01-01T01:02:06.456789"),
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
    hjemler: List<String>? = emptyList(),
    iverksattOpprettholdelseTidspunkt: String? = null,
    journalføringstidspunktInnstillingsbrev: String? = null,
    distribusjonstidspunktInnstillingsbrev: String? = null,
    oversendtKlageinstansenTidspunkt: String? = null,
    klageinstanshendelser: List<String>? = emptyList(),
) {
    val expected =
        """
       {
         "id": "$klagebehandlingId",
         "sakId": "$sakId",
         "saksnummer": "$saksnummer",
         "fnr": "$fnr",
         "opprettet": "$opprettet",
         "sistEndret": "$sistEndret",
         "iverksattTidspunkt": ${iverksattTidspunkt.toJsonValue()},
         "saksbehandler": ${saksbehandler.toJsonValue()},
         "journalpostId": "$journalpostId",
         "journalpostOpprettet": "$journalpostOpprettet",
         "status": "$status",
         "resultat": ${resultat.toJsonValue()},
         "vedtakDetKlagesPå": ${vedtakDetKlagesPå.toJsonValue()},
         "erKlagerPartISaken": $erKlagerPartISaken,
         "klagesDetPåKonkreteElementerIVedtaket": $klagesDetPåKonkreteElementerIVedtaket,
         "erKlagefristenOverholdt": $erKlagefristenOverholdt,
         "erUnntakForKlagefrist": ${erUnntakForKlagefrist.toJsonValue()},
         "erKlagenSignert": $erKlagenSignert,
         "innsendingsdato": "$innsendingsdato",
         "innsendingskilde": "$innsendingskilde",
         "brevtekst": ${if (brevtekst.isEmpty()) "[]" else brevtekst.toString()},
         "avbrutt": $avbrutt,
         "kanIverksetteVedtak": $kanIverksetteVedtak,
         "kanIverksetteOpprettholdelse": $kanIverksetteOpprettholdelse,
         "årsak": ${årsak.toJsonValue()},
         "begrunnelse": ${begrunnelse.toJsonValue()},
         "rammebehandlingId": ${rammebehandlingId.toJsonValue()},
         "ventestatus": $ventestatus,
         "hjemler": ${hjemler?.let { "[ ${it.joinToString { "\"$it\"" }} ]" } ?: "null"},
         "iverksattOpprettholdelseTidspunkt": ${iverksattOpprettholdelseTidspunkt.toJsonValue()},
         "journalføringstidspunktInnstillingsbrev": ${journalføringstidspunktInnstillingsbrev.toJsonValue()},
         "distribusjonstidspunktInnstillingsbrev": ${distribusjonstidspunktInnstillingsbrev.toJsonValue()},
         "oversendtKlageinstansenTidspunkt": ${oversendtKlageinstansenTidspunkt.toJsonValue()},
         "klageinstanshendelser": ${klageinstanshendelser?.let { "[ ${it.joinToString { "\"$it\"" }} ]" } ?: "null"}
       }
        """.trimIndent()
    if (ignorerTidspunkt) {
        this.shouldEqualJsonIgnoringTimestamps(expected)
    } else {
        this.shouldEqualJson(expected)
    }
}

private fun String?.toJsonValue(): String = if (this == null) "null" else "\"$this\""
