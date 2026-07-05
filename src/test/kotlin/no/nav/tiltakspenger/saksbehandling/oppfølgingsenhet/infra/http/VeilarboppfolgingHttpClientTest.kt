package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.BruktNavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import org.junit.jupiter.api.Test
import java.time.Instant

internal class VeilarboppfolgingHttpClientTest {
    private val fnr = Fnr.random()

    private fun client(baseUrl: String) = VeilarboppfolgingHttpClient(
        baseUrl = baseUrl,
        getToken = { AccessToken("token", Instant.MAX) },
    )

    @Test
    fun `henter oppfølgingsenhet - 200 gir kontor og POSTer til endpoint`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/veilarboppfolging/api/v2/person/system/hent-oppfolgingsstatus"
            } returns {
                statusCode = 200
                body = """{"oppfolgingsenhet":{"navn":"Nav Askim","enhetId":"1234"},"veilederId":null,"formidlingsgruppe":null,"servicegruppe":null,"hovedmaalkode":null}"""
                header = "Content-Type" to "application/json"
            }

            runTest {
                val result = client(wiremock.baseUrl()).hentOppfolgingsenhet(
                    fnr,
                    sakId = "123",
                    saksnummer = "456",
                    rammebehandlingId = "789",
                    meldekortbehandlingId = "012",
                )
                val metadata = result.fold({ null }, { it })
                metadata!!.navkontor shouldBe Navkontor(kontornummer = "1234", kontornavn = "Nav Askim")
                metadata.brukteKlient shouldBe BruktNavkontorKlient.VEILARBOPPFOLGING
                metadata.veilarboppfolgingKall!!.httpStatus shouldBe 200
            }
        }
    }

    @Test
    fun `henter oppfølgingsenhet - uventet status gir Left`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/veilarboppfolging/api/v2/person/system/hent-oppfolgingsstatus"
            } returns {
                statusCode = 500
                body = "feil"
                header = "Content-Type" to "text/plain"
            }

            runTest {
                val result = client(wiremock.baseUrl()).hentOppfolgingsenhet(fnr, sakId = "123")
                result.isLeft() shouldBe true
            }
        }
    }
}
