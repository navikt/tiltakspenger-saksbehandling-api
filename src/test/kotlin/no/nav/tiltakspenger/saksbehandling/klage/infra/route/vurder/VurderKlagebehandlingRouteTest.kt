package no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandling
import org.junit.jupiter.api.Test

class VurderKlagebehandlingRouteTest {
    @Test
    fun `kan vurdere klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
            )!!
            json.toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:33.456789",
                  "sistEndret": "2025-01-01T01:02:34.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:32.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "OMGJØR",
                  "vedtakDetKlagesPå": "${rammevedtakSøknadsbehandling.id}",
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
                  "rammebehandlingId": null
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan endre årsak og begrunnelse ved åpen rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, søknadsbehandlingOpprettetFraKlage, klagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            søknadsbehandlingOpprettetFraKlage as Søknadsbehandling
            val vedtakDetKlagesPå = sak.rammevedtaksliste.first()
            val (_, _, json) = vurderKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!),
                begrunnelse = Begrunnelse.createOrThrow("oppdatert begrunnelse for omgjøring"),
                årsak = KlageOmgjøringsårsak.ANNET,
            )!!
            json.toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:33.456789",
                  "sistEndret": "2025-01-01T01:02:42.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:32.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "OMGJØR",
                  "vedtakDetKlagesPå": "${vedtakDetKlagesPå.id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksette": false,
                  "årsak": "ANNET",
                  "begrunnelse": "oppdatert begrunnelse for omgjøring",
                  "rammebehandlingId": "${søknadsbehandlingOpprettetFraKlage.id}"
                }
                """.trimIndent(),
            )
        }
    }
}
