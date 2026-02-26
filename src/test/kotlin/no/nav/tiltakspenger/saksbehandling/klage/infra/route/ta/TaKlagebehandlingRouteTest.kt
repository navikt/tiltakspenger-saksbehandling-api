package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgTaKlagebehandling
import org.junit.jupiter.api.Test

class TaKlagebehandlingRouteTest {
    @Test
    fun `kan ta klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgTaKlagebehandling(
                tac = tac,
            )!!
            json.get("klageBehandlinger").first().toString().shouldEqualJsonIgnoringTimestamps(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:36.456789",
                  "sistEndret": "2025-01-01T01:02:53.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerSomTar",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:35.456789",
                  "status": "UNDER_BEHANDLING",
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
                  "ventestatus": null,
                  "hjemler": null,
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null,
                  "klageinstanshendelser": null,
                  "ferdigstiltTidspunkt": null
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `idempotent og ta klagebehandling fra seg selv`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertar")
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgTaKlagebehandling(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
                saksbehandlerSomTar = saksbehandler,
            )!!
            json.get("klageBehandlinger").first().toString().shouldEqualJsonIgnoringTimestamps(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:36.456789",
                  "sistEndret": "2025-01-01T01:02:53.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerSomOvertar",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:35.456789",
                  "status": "UNDER_BEHANDLING",
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
                  "ventestatus": null,
                  "hjemler": null,
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null,
                  "klageinstanshendelser": null,
                  "ferdigstiltTidspunkt": null
                }
                """.trimIndent(),
            )
        }
    }
}
