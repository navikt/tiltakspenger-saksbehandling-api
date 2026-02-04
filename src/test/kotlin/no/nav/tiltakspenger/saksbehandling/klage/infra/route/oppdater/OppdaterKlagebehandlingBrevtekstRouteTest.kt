package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingBrevtekst
import org.junit.jupiter.api.Test

class OppdaterKlagebehandlingBrevtekstRouteTest {
    @Test
    fun `kan oppdatere klagebehandling - brevtekst`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, behandling, json) = opprettSakOgOppdaterKlagebehandlingBrevtekst(
                tac = tac,
            )!!
            json.toString().shouldEqualJson(
                """
                {
                  "id": "${behandling.id}",
                  "sakId": "${behandling.sakId}",
                  "saksnummer": "202505011001",
                  "fnr": "12345678911",
                  "opprettet": "2025-05-01T01:02:07.456789",
                  "sistEndret": "2025-05-01T01:02:08.456789",
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
                  "brevtekst": [
                    {
                      "tittel": "Avvisning av klage",
                      "tekst": "Din klage er dessverre avvist."
                    }
                  ],
                  "avbrutt": null,
                  "kanIverksette": true,
                  "årsak": null,
                  "begrunnelse": null,
                  "rammebehandlingId": null,
                  "ventestatus": null
                }
                """.trimIndent(),
            )
        }
    }
}
