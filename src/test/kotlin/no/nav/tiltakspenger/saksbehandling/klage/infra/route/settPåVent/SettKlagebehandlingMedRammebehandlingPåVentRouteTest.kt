package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SettKlagebehandlingMedRammebehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling med rammebehandling på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
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
                  "ventestatus": {
                    "sattPåVentAv": "saksbehandlerKlagebehandling",
                    "tidspunkt": "2025-01-01T01:02:43.456789",
                    "begrunnelse": "begrunnelse for å sette klage på vent",
                    "erSattPåVent": true,
                    "frist": "2025-01-14"
                  }
                }
                """.trimIndent(),
            )
            rammebehandlingMedKlagebehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            rammebehandlingMedKlagebehandling.saksbehandler shouldBe null
            rammebehandlingMedKlagebehandling.ventestatus shouldBe Ventestatus(
                listOf(
                    VentestatusHendelse(
                        tidspunkt = LocalDateTime.parse("2025-01-01T01:02:42.456789"),
                        endretAv = "saksbehandlerKlagebehandling",
                        begrunnelse = "begrunnelse for å sette klage på vent",
                        erSattPåVent = true,
                        status = "UNDER_BEHANDLING",
                        frist = LocalDate.parse("2025-01-14"),
                    ),
                ),
            )
            rammebehandlingMedKlagebehandling.sistEndret shouldBe LocalDateTime.parse("2025-01-01T01:02:44.456789")
        }
    }
}
