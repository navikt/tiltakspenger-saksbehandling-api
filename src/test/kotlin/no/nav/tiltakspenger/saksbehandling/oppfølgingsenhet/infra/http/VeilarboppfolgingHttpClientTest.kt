package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.BruktNavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteNavkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import org.junit.jupiter.api.Test
import java.time.Instant

internal class VeilarboppfolgingHttpClientTest {
    private val baseUrl = "http://veilarboppfolging.test"
    private val fnr = Fnr.random()

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = VeilarboppfolgingHttpClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        transport = transport,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        VeilarboppfolgingHttpClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `henter oppfølgingsenhet - 200 gir kontor og POSTer til endepunktet med riktige headere`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                Response(
                    oppfolgingsenhet = Oppfolgingsenhet(navn = "Nav Askim", enhetId = "1234"),
                    veilederId = null,
                    formidlingsgruppe = null,
                    servicegruppe = null,
                    hovedmaalkode = null,
                ),
            )
        }

        runTest {
            val result = client(transport).hentOppfolgingsenhet(fnr)
            val metadata = result.fold({ null }, { it })
            metadata!!.navkontor shouldBe Navkontor(kontornummer = "1234", kontornavn = "Nav Askim")
            metadata.brukteKlient shouldBe BruktNavkontorKlient.VEILARBOPPFOLGING
            metadata.veilarboppfolgingKall!!.httpStatus shouldBe 200
            metadata.httpKlientMetadata.shouldNotBeNull().statusCode shouldBe 200
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/veilarboppfolging/api/v2/person/system/hent-oppfolgingsstatus"
        kall.request.headers().allValues("Nav-Consumer-Id") shouldBe listOf("tiltakspenger-saksbehandling-api")
        kall.request.headers().allValues("Content-Type") shouldBe listOf("application/json")
    }

    @Test
    fun `henter oppfølgingsenhet - manglende oppfolgingsenhet gir ManglerOppfolgingsenhet`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                Response(
                    oppfolgingsenhet = null,
                    veilederId = null,
                    formidlingsgruppe = null,
                    servicegruppe = null,
                    hovedmaalkode = null,
                ),
            )
        }

        runTest {
            val result = client(transport).hentOppfolgingsenhet(fnr)
            val feil = result.fold({ it }, { null }).shouldBeInstanceOf<KanIkkeHenteNavkontor.ManglerOppfolgingsenhet>()
            feil.httpKlientMetadata.shouldNotBeNull().statusCode shouldBe 200
            feil.httpKlientError shouldBe null
        }
    }

    @Test
    fun `henter oppfølgingsenhet - uventet status gir UventetHttpStatus`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil", contentType = "text/plain") }

        runTest {
            val result = client(transport).hentOppfolgingsenhet(fnr)
            val feil = result.fold({ it }, { null }) as KanIkkeHenteNavkontor.UventetHttpStatus
            feil.status shouldBe 500
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
        }
    }

    @Test
    fun `henter oppfølgingsenhet - nettverksfeil gir KallFeilet`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(java.io.IOException("simulert nettverksfeil")) }

        runTest {
            val result = client(transport).hentOppfolgingsenhet(fnr)
            val feil = result.fold({ it }, { null }).shouldBeInstanceOf<KanIkkeHenteNavkontor.KallFeilet>()
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.NetworkError>()
            feil.veilarboppfolgingKall.shouldNotBeNull().httpStatus shouldBe null
        }
    }

    @Test
    fun `henter oppfølgingsenhet - deserialiseringsfeil gir KallFeilet`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 200, body = "ikke json") }

        runTest {
            val result = client(transport).hentOppfolgingsenhet(fnr)
            val feil = result.fold({ it }, { null }).shouldBeInstanceOf<KanIkkeHenteNavkontor.KallFeilet>()
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.DeserializationError>()
        }
    }
}
