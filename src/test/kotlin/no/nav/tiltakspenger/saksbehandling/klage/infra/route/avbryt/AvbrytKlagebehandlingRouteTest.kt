package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgAvbrytKlagebehandling
import org.junit.jupiter.api.Test

class AvbrytKlagebehandlingRouteTest {
    @Test
    fun `oppretter klage og deretter avbryter`() {
        withTestApplicationContext { tac ->
            val (sak, klagebehandling, json) = opprettSakOgAvbrytKlagebehandling(
                tac = tac,
            )!!
            json.toString().shouldEqualJson(
                """
                   {
                     "id": "${klagebehandling.id}",
                     "sakId": "${sak.id}",
                     "saksnummer": "${sak.saksnummer}",
                     "fnr": "12345678911",
                     "opprettet": "2025-05-01T01:02:06.456789",
                     "sistEndret": "2025-05-01T01:02:07.456789",
                     "iverksattTidspunkt": null,
                     "saksbehandler": "Z12345",
                     "journalpostId": "12345",
                     "journalpostOpprettet": "2025-05-01T01:02:05.456789",
                     "status": "AVBRUTT",
                     "resultat": "AVVIST",
                     "vedtakDetKlagesPå": null,
                     "erKlagerPartISaken": true,
                     "klagesDetPåKonkreteElementerIVedtaket": true,
                     "erKlagefristenOverholdt": true,
                     "erUnntakForKlagefrist": null,
                     "erKlagenSignert": true,
                     "brevtekst": [],
                     "avbrutt": {
                          "avbruttAv": "Z12345",
                          "avbruttTidspunkt": "2025-05-01T01:02:08.456789",
                          "begrunnelse": "begrunnelse for avbryt klagebehandling"
                     },
                     "kanIverksette": false,
                     "årsak": null,
                     "begrunnelse": null,
                     "rammebehandlingId": null
                   }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan ikke avbryte hvis åpen tilknyttet rammebehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, klagebehandling) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            val rammebehandlingId = sak.rammebehandlinger[1].id
            avbrytKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Klagebehandlingen kan ikke avbrytes fordi den er knyttet til en rammebehandling som ikke er avbrutt: $rammebehandlingId",
                        "kode": "knyttet_til_ikke_avbrutt_rammebehandling"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }
}
