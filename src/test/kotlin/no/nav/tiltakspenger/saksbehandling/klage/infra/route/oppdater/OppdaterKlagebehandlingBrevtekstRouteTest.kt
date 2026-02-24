package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst
import org.junit.jupiter.api.Test

class OppdaterKlagebehandlingBrevtekstRouteTest {
    @Test
    fun `kan oppdatere klagebehandling - brevtekst`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, behandling, json) = opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
                tac = tac,
            )!!
            json.toString().shouldEqualJsonIgnoringTimestamps(
                """
                {
                  "id": "${behandling.id}",
                  "sakId": "${behandling.sakId}",
                  "saksnummer": "202505011001",
                  "fnr": "12345678911",
                  "opprettet": "2025-05-01T01:02:07.456789",
                  "sistEndret": "2025-05-01T01:02:09.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-05-01T01:02:06.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "AVVIST",
                  "vedtakDetKlagesPå": null,
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                  "brevtekst": [
                    {
                      "tittel": "Avvisning av klage",
                      "tekst": "Din klage er dessverre avvist."
                    }
                  ],
                  "avbrutt": null,
                  "kanIverksetteVedtak": true,
                  "kanIverksetteOpprettholdelse": false,
                  "årsak": null,
                  "begrunnelse": null,
                  "rammebehandlingId": null,
                  "ventestatus": null,
                  "hjemler": null,
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null,
                  "klageinstanshendelser": null
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan oppdatere klagebehandling (opprettholdelse) - brevtekst`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, rammevedtak, klagebehandling, json) = opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
                tac = tac,
            )!!
            json.toString().shouldEqualJsonIgnoringTimestamps(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${klagebehandling.sakId}",
                  "saksnummer": "202505011001",
                  "fnr": "12345678911",
                  "opprettet": "2025-05-01T01:02:36.456789",
                  "sistEndret": "2025-05-01T01:02:39.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-05-01T01:02:35.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "OPPRETTHOLDT",
                  "vedtakDetKlagesPå": "${rammevedtak.id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                     "brevtekst": [
                       {
                         "tittel": "Hva klagesaken gjelder",
                         "tekst": "Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"
                       },
                       {
                         "tittel": "Klagers anførsler",
                         "tekst": "<saksbehandler fyller ut>"
                       },
                       {
                         "tittel": "Vurdering av klagen",
                         "tekst": "<saksbehandler fyller ut>"
                       }
                     ],
                  "avbrutt": null,
                  "kanIverksetteVedtak": false,
                  "kanIverksetteOpprettholdelse": true,
                  "årsak": null,
                  "begrunnelse": null,
                  "rammebehandlingId": null,
                  "ventestatus": null,
                  "hjemler": ["ARBEIDSMARKEDSLOVEN_17"],
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null,
                  "klageinstanshendelser": []
                }
                """.trimIndent(),
            )
        }
    }
}
