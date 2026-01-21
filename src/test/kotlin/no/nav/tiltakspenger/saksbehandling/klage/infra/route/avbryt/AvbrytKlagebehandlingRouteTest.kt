package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgavbrytKlagebehandling
import org.junit.jupiter.api.Test

class AvbrytKlagebehandlingRouteTest {
    @Test
    fun `oppretter klage og deretter avbryter`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val (sak, klagebehandling, json) = opprettSakOgavbrytKlagebehandling(
                tac = tac,
            )!!
            json.toString().shouldEqualJson(
                """
                   {
                     "id": "${klagebehandling.id}",
                     "sakId": "${sak.id}",
                     "saksnummer": "${sak.saksnummer}",
                     "fnr": "12345678911",
                     "opprettet": "2025-01-01T01:02:06.456789",
                     "sistEndret": "2025-01-01T01:02:07.456789",
                     "iverksattTidspunkt": null,
                     "saksbehandler": "Z12345",
                     "journalpostId": "12345",
                     "journalpostOpprettet": "2025-01-01T01:02:05.456789",
                     "status": "AVBRUTT",
                     "resultat": "AVVIST",
                     "vedtakDetKlagesPå": null,
                     "erKlagerPartISaken": true,
                     "klagesDetPåKonkreteElementerIVedtaket": true,
                     "erKlagefristenOverholdt": true,
                     "erUnntakForKlagefrist": null,
                     "erKlagenSignert": true,
                     "brevtekst": [],
                     "avbrutt": {
                          "avbruttAv": "Z12345",
                          "avbruttTidspunkt": "2025-01-01T01:02:08.456789",
                          "begrunnelse": "oppdaterKlagebehandlingFormkravForSakId"
                     },
                     "kanIverksette": false
                   }
                """.trimIndent(),
            )
        }
    }
}
