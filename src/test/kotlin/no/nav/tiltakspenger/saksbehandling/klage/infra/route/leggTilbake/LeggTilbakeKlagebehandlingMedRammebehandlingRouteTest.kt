package no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgLeggKlagebehandlingMedRammebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgLeggKlagebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOvertaKlagebehandling
import org.junit.jupiter.api.Test

class LeggTilbakeKlagebehandlingMedRammebehandlingRouteTest {
    @Test
    fun `kan legge klagebehandling med rammebehandling tilbake`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgLeggKlagebehandlingMedRammebehandlingTilbake(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.get("klageBehandlinger").first().toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:33.456789",
                  "sistEndret": "2025-01-01T01:02:43.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": null,
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:32.456789",
                  "status": "KLAR_TIL_BEHANDLING",
                  "resultat": "OMGJØR",
                  "vedtakDetKlagesPå": "${sak.rammevedtaksliste.first().id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksette": false,
                  "årsak": "PROSESSUELL_FEIL",
                  "begrunnelse": "Begrunnelse for omgjøring",
                  "rammebehandlingId": "${rammebehandlingMedKlagebehandling.id}",
                  "ventestatus": null
                }
                """.trimIndent(),
            )
        }
    }
}
