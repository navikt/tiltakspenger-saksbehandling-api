package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.right
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class KabalHttpClientTest {

    @Test
    fun `håndterer ok request`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/api/oversendelse/v4/sak"
            } returns {
                statusCode = 200
                header = "Content-Type" to "application/json"
            }
            val kabalclient = KabalHttpClient(
                baseUrl = wiremock.baseUrl(),
                getToken = { ObjectMother.accessToken() },
                clock = fixedClock,
            )

            runTest {
                val klagebehandling = ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse()
                kabalclient.oversend(
                    klagebehandling = klagebehandling,
                    journalpostIdVedtak = JournalpostId("journalpost-vedtak-1"),
                ) shouldBe OversendtKlageTilKabalMetadata(
                    request = """{"sakenGjelder":{"id":{"verdi":"${klagebehandling.fnr.verdi}","type":"PERSON"}},"fagsak":{"fagsakId":"202401011234","fagsystem":"TILTAKSPENGER"},"kildeReferanse":"${klagebehandling.id}","dvhReferanse":"${klagebehandling.id}","hjemler":["FS_TIP_3"],"tilknyttedeJournalposter":[{"type":"BRUKERS_KLAGE","journalpostId":"journalpostId"},{"type":"OPPRINNELIG_VEDTAK","journalpostId":"journalpost-vedtak-1"},{"type":"OVERSENDELSESBREV","journalpostId":"journalpostId"}],"brukersKlageMottattVedtaksinstans":"2026-02-16","forrigeBehandlendeEnhet":"0387","type":"KLAGE","ytelse":"TIL_TIP"}""".trimIndent(),
                    response = "",
                    oversendtTidspunkt = nå(fixedClock),
                ).right()
            }
        }
    }
}
