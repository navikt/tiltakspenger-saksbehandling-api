package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingFormkrav
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgavbrytKlagebehandling
import org.junit.jupiter.api.Test

class AvbrytKlagebehandlingRouteTest {
    @Test
    fun `kan oppdatere klagebehandling - formkrav`() {
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
                     "erKlagenSignert": true,
                     "brevtekst": [],
                     "erAvbrutt": true,
                     "kanIverksette": false
                   }
                """.trimIndent(),
            )
        }
    }
}
