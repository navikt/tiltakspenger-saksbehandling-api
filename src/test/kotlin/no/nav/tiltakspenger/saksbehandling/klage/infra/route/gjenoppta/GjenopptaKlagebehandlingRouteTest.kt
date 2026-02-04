package no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgGjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgLeggKlagebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOvertaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgTaKlagebehandling
import org.junit.jupiter.api.Test

class GjenopptaKlagebehandlingRouteTest {
    @Test
    fun `kan gjenoppta klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgGjenopptaKlagebehandling(
                tac = tac,
            )!!
            json.get("klageBehandlinger").first().toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:33.456789",
                  "sistEndret": "2025-01-01T01:02:49.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:32.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": null,
                  "vedtakDetKlagesPå": "${rammevedtakSøknadsbehandling.id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksette": false,
                  "årsak": null,
                  "begrunnelse": null,
                  "rammebehandlingId": null,
                  "ventestatus": {
                    "sattPåVentAv": "saksbehandlerKlagebehandling",
                    "tidspunkt": "2025-01-01T01:02:49.456789",
                    "begrunnelse": "",
                    "erSattPåVent": false
                  }
                }
                """.trimIndent(),
            )
        }
    }
}
