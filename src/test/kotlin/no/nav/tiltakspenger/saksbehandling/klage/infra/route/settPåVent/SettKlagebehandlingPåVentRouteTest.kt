package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingPåVent
import org.junit.jupiter.api.Test

class SettKlagebehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgSettKlagebehandlingPåVent(
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
                  "sistEndret": "2025-01-01T01:02:34.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": null,
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:32.456789",
                  "status": "KLAR_TIL_BEHANDLING",
                  "resultat": null,
                  "vedtakDetKlagesPå": "${rammevedtakSøknadsbehandling.id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksetteVedtak": false,
                  "kanIverksetteOpprettholdelse": false,
                  "årsak": null,
                  "begrunnelse": null,
                  "rammebehandlingId": null,
                  "ventestatus": {
                    "sattPåVentAv": "saksbehandlerKlagebehandling",
                    "tidspunkt": "2025-01-01T01:02:34.456789",
                    "begrunnelse": "begrunnelse for å sette klage på vent",
                    "erSattPåVent": true,
                    "frist": "2025-01-14"
                  },
                  "hjemler": null
                }
                """.trimIndent(),
            )
        }
    }
}
