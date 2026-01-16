package no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgIverksettKlagebehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class IverksettKlagebehandlingRouteTest {
    @Test
    fun `kan iverksette klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val (sak, klagevedtak, json) = opprettSakOgIverksettKlagebehandling(
                tac = tac,
            )!!
            val klagebehandling = klagevedtak.behandling
            klagevedtak shouldBe Klagevedtak(
                id = klagevedtak.id,
                opprettet = LocalDateTime.parse("2025-01-01T01:02:10.456789"),
                behandling = klagebehandling,
                journalpostId = null,
                journalføringstidspunkt = null,
                distribusjonId = null,
                distribusjonstidspunkt = null,
                vedtaksdato = null,
                sendtTilDatadeling = null,
                brevJson = null,
            )
            json.toString().shouldEqualJson(
                """
                   {
                     "id": "${klagebehandling.id}",
                     "sakId": "${sak.id}",
                     "saksnummer": "${sak.saksnummer}",
                     "fnr": "12345678911",
                     "opprettet": "2025-01-01T01:02:07.456789",
                     "sistEndret": "2025-01-01T01:02:09.456789",
                     "saksbehandler": "Z12345",
                     "journalpostId": "12345",
                     "journalpostOpprettet": "2025-01-01T01:02:06.456789",
                     "status": "IVERKSATT",
                     "resultat": "AVVIST",
                     "vedtakDetKlagesPå": null,
                     "erKlagerPartISaken": true,
                     "klagesDetPåKonkreteElementerIVedtaket": true,
                     "erKlagefristenOverholdt": true,
                     "erKlagenSignert": true,
                     "brevtekst": [
                        {
                          "tittel": "Avvisning av klage",
                          "tekst": "Din klage er dessverre avvist."
                        }
                      ],
                     "erAvbrutt": false,
                     "kanIverksette": false,
                     "iverksattTidspunkt": "2025-01-01T01:02:09.456789"
                   }
                """.trimIndent(),
            )
            hentSakForSaksnummer(tac = tac, saksnummer = klagebehandling.saksnummer)!!.getJSONArray("alleKlagevedtak")
                .also {
                    it.length() shouldBe 1
                    val hentetKlagevedtakJson = it.getJSONObject(0)
                    hentetKlagevedtakJson.toString().shouldEqualJson(
                        """
                    {
                      "klagebehandlingId": "${klagebehandling.id}",
                      "journalføringstidspunkt": null,
                      "opprettet": "2025-01-01T01:02:10.456789",
                      "distribusjonstidspunkt": null,
                      "distribusjonId": null,
                      "sakId": "${sak.id}",
                      "klagevedtakId": "${klagevedtak.id}",
                      "vedtaksdato": null,
                      "journalpostId": null
                    }
                        """.trimIndent(),
                    )
                }
        }
    }
}
