package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingFormkrav
import org.junit.jupiter.api.Test

class OppdaterKlagebehandlingFormkravRouteTest {
    @Test
    fun `kan oppdatere klagebehandling - formkrav`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val fnr = Fnr.fromString("12345678912")
            val (sak, klagebehandling, json) =
                opprettSakOgOppdaterKlagebehandlingFormkrav(
                    tac = tac,
                    fnr = fnr,
                    erKlagerPartISaken = false,
                    klagesDetPåKonkreteElementerIVedtaket = false,
                    erKlagefristenOverholdt = false,
                    erKlagenSignert = false,
                    erUnntakForKlagefrist = KlagefristUnntakSvarord.NEI,
                    journalpostId = JournalpostId("123456"),
                    vedtakDetKlagesPå = VedtakId.fromString("vedtak_01KEYFMDNGXAFAYW1CD1X47CND"),
                )!!
            json.toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678912",
                  "opprettet": "2025-01-01T01:02:06.456789",
                  "sistEndret": "2025-01-01T01:02:08.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "123456",
                  "journalpostOpprettet": "2025-01-01T01:02:07.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "AVVIST",
                  "vedtakDetKlagesPå": "vedtak_01KEYFMDNGXAFAYW1CD1X47CND",
                  "erKlagerPartISaken": false,
                  "klagesDetPåKonkreteElementerIVedtaket": false,
                  "erKlagefristenOverholdt": false,
                  "erUnntakForKlagefrist": "NEI",
                  "erKlagenSignert": false,
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksette": false,
                  "årsak": null,
                  "begrunnelse": null,
                  "rammebehandlingId": null
                }
                """.trimIndent(),
            )
        }
    }
}
