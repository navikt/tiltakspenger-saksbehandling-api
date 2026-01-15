package no.nav.tiltakspenger.saksbehandling.klage.infra.route.start

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import org.junit.jupiter.api.Test

class OpprettKlagebehandlingRouteTest {
    @Test
    fun `kan opprette klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val fnr = Fnr.fromString("12345678912")
            val (sak, klagebehandling, json) = opprettSakOgKlagebehandlingTilAvvisning(
                tac = tac,
                fnr = fnr,
            )!!
            json.toString().shouldEqualJson(
                """
                   {
                     "id": "${klagebehandling.id}",
                     "sakId": "${sak.id}",
                     "saksnummer": "${sak.saksnummer}",
                     "fnr": "12345678912",
                     "opprettet": "${klagebehandling.opprettet}",
                     "sistEndret": "${klagebehandling.sistEndret}",
                     "iverksattTidspunkt": null,
                     "saksbehandler": "Z12345",
                     "journalpostId": "12345",
                     "journalpostOpprettet": "2025-01-01T01:02:05.456789",
                     "status": "UNDER_BEHANDLING",
                     "resultat": "AVVIST",
                     "vedtakDetKlagesPå": null,
                     "erKlagerPartISaken": true,
                     "klagesDetPåKonkreteElementerIVedtaket": true,
                     "erKlagefristenOverholdt": true,
                     "erKlagenSignert": true,
                     "brevtekst": [],
                     "erAvbrutt": false,
                     "kanIverksette": false
                   }
                """.trimIndent(),
            )
        }
    }
}
