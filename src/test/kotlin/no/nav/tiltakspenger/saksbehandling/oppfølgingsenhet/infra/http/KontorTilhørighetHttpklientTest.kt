package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteKontorTilhørighet
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighet
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighet.KontorType
import org.junit.jupiter.api.Test
import java.time.Instant

class KontorTilhørighetHttpklientTest {
    private val baseUrl = "http://ao-oppfolgingskontor.test"
    private val fnr = Fnr.random()

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = KontorTilhørighetHttpklient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        transport = transport,
    )

    private fun transportMed(kontorTilhørighet: KontorTilhørighetDto?) = FakeHttpTransport().apply {
        leggIKøJson(GraphQlResponse(data = GraphQlData(kontorTilhorighet = kontorTilhørighet)))
    }

    private fun dto(
        kontorId: String = "0123",
        kontorNavn: String = "NAV Oslo",
        kontorType: KontorTypeDto = KontorTypeDto.ARBEIDSOPPFOLGING,
    ) = KontorTilhørighetDto(
        kontorId = kontorId,
        kontorNavn = kontorNavn,
        kontorType = kontorType,
    )

    @Test
    fun `bygger default HttpKlient når transport ikke sendes inn`() {
        KontorTilhørighetHttpklient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    /**
     * Vi sammenligner mot hele objektet bevisst, slik at testen brekker dersom vi legger til (eller mister) felter på
     * [KontorTilhørighet] uten å tenke gjennom personvernkonsekvenser.
     */
    @Test
    fun `mapper kontortilhørigheten og POSTer kontorTilhorighet-spørringen til endepunktet`() {
        val transport = transportMed(dto(kontorId = "0123", kontorNavn = "NAV Oslo", kontorType = KontorTypeDto.ARENA))

        runTest {
            val resultat = client(transport).hentKontorTilhørighet(fnr).getOrNull().shouldNotBeNull()

            resultat.kontorTilhørighet shouldBe KontorTilhørighet(
                kontorId = "0123",
                kontorNavn = "NAV Oslo",
                kontorType = KontorType.ARENA,
            )
            resultat.httpKlientMetadata.statusCode shouldBe 200
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/graphql"
        kall.bodyTekst shouldContain "kontorTilhorighet(ident:"
        kall.bodyTekst shouldContain fnr.verdi
    }

    @Test
    fun `null kontorTilhorighet i responsen gir ingen kontortilhørighet`() {
        val transport = transportMed(null)

        runTest {
            client(transport).hentKontorTilhørighet(fnr).getOrNull().shouldNotBeNull().kontorTilhørighet shouldBe null
        }
    }

    @Test
    fun `null data uten errors gir ingen kontortilhørighet`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(GraphQlResponse(data = null))
        }

        runTest {
            client(transport).hentKontorTilhørighet(fnr).getOrNull().shouldNotBeNull().kontorTilhørighet shouldBe null
        }
    }

    @Test
    fun `non-200 fra tjenesten gir Left UventetHttpStatus`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 503, body = """{"message": "noe gikk galt"}""")
        }

        runTest {
            val feil = client(transport).hentKontorTilhørighet(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorTilhørighet.UventetHttpStatus>()
            feil.status shouldBe 503
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 503
            feil.httpKlientMetadata.statusCode shouldBe 503
        }
    }

    @Test
    fun `nettverksfeil gir Left KallFeilet`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(java.io.IOException("simulert nettverksfeil")) }

        runTest {
            val feil = client(transport).hentKontorTilhørighet(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorTilhørighet.KallFeilet>()
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.NetworkError>()
            feil.httpKlientMetadata.statusCode shouldBe null
        }
    }

    /** En feil før requesten er sendt (her: token-henting som kaster) skal også ende som KallFeilet. */
    @Test
    fun `feilet token-henting gir Left KallFeilet uten at noe kall sendes`() {
        val transport = FakeHttpTransport()
        val klientMedFeilendeAuth = KontorTilhørighetHttpklient(
            baseUrl = baseUrl,
            authTokenProvider = object : AuthTokenProvider {
                override suspend fun hentToken(skipCache: Boolean): AccessToken = throw IllegalStateException("simulert token-feil")
            },
            clock = ObjectMother.clock,
            transport = transport,
        )

        runTest {
            val feil = klientMedFeilendeAuth.hentKontorTilhørighet(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorTilhørighet.KallFeilet>()
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.AuthError>()
        }

        transport.mottatteKall shouldBe emptyList()
    }

    @Test
    fun `GraphQL errors i respons gir Left GraphQlFeil`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                GraphQlResponse(
                    data = null,
                    errors = listOf(mapOf("message" to "noe gikk galt")),
                ),
            )
        }

        runTest {
            val feil = client(transport).hentKontorTilhørighet(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorTilhørighet.GraphQlFeil>()
            feil.httpKlientMetadata.statusCode shouldBe 200
        }
    }

    @Test
    fun `tomt errors-felt behandles som suksess`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                GraphQlResponse(
                    data = GraphQlData(kontorTilhorighet = dto()),
                    errors = emptyList(),
                ),
            )
        }

        runTest {
            client(transport).hentKontorTilhørighet(fnr).getOrNull().shouldNotBeNull().kontorTilhørighet.shouldNotBeNull()
        }
    }

    @Test
    fun `ukjent kontorType gir Left KallFeilet med DeserializationError`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(
                statusCode = 200,
                body = """{"data": {"kontorTilhorighet": {"kontorId": "0123", "kontorNavn": "NAV Oslo", "kontorType": "EN_HELT_NY_TYPE"}}}""",
            )
        }

        runTest {
            val feil = client(transport).hentKontorTilhørighet(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorTilhørighet.KallFeilet>()
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.DeserializationError>()
            feil.httpKlientMetadata.statusCode shouldBe 200
        }
    }

    @Test
    fun `mapper alle kjente kontorType-verdier til domene-enum`() {
        KontorTypeDto.entries.map { it.toDomene() } shouldBe listOf(
            KontorType.ARBEIDSOPPFOLGING,
            KontorType.ARENA,
            KontorType.GEOGRAFISK_TILKNYTNING,
        )
    }
}
