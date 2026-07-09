package no.nav.tiltakspenger.saksbehandling.saksbehandler.infra

import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlientFake
import no.nav.tiltakspenger.libs.httpklient.HttpMethod
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandler.KanIkkeHenteNavnForNavIdent
import no.nav.tiltakspenger.saksbehandling.saksbehandler.hentNavnForNavIdentEllerKast
import org.junit.jupiter.api.Test
import java.time.Instant

internal class MicrosoftGraphApiClientTest {
    private val baseUrl = "graph.test"
    private val navIdent = "Z123456"

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(httpKlient: HttpKlientFake) = MicrosoftGraphApiClient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        httpKlient = httpKlient,
    )

    private fun HttpKlientFake.enqueueBrukere(vararg brukere: MicrosoftGraphResponse) {
        enqueueResponse(body = ListOfMicrosoftGraphResponse(value = brukere.toList()))
    }

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        MicrosoftGraphApiClient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    @Test
    fun `automatisk saksbehandler gir fast navn uten kall mot Graph`() {
        val httpKlient = HttpKlientFake()

        runTest {
            client(httpKlient).hentNavnForNavIdent(AUTOMATISK_SAKSBEHANDLER_ID) shouldBe "Automatisk saksbehandlet".right()
        }

        httpKlient.requests shouldBe emptyList()
    }

    @Test
    fun `henter navn - én bruker gir navn og GET mot users-endepunktet med riktig encoding og headere`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueBrukere(MicrosoftGraphResponse(givenName = "Saks", surname = "Behandler"))
        }

        runTest {
            client(httpKlient).hentNavnForNavIdent(navIdent) shouldBe "Saks Behandler".right()
        }

        val request = httpKlient.requests.single()
        request.method shouldBe HttpMethod.GET
        request.uri.toString() should {
            it shouldContain "http://graph.test/users?"
            // Apostrofene rundt navIdenten i $filter skal være URL-encodet (%27) - det er hele poenget med URLBuilder-bruken i klienten.
            it shouldContain "%27$navIdent%27"
            it shouldNotContain "'"
        }
        request.headers["ConsistencyLevel"] shouldBe listOf("eventual")
    }

    @Test
    fun `henter navn - navnet trimmes`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueBrukere(MicrosoftGraphResponse(givenName = "", surname = "Behandler"))
        }

        runTest {
            client(httpKlient).hentNavnForNavIdent(navIdent) shouldBe "Behandler".right()
        }
    }

    @Test
    fun `uventet status gir KallFeilet`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "feil") }

        runTest {
            val feil = client(httpKlient).hentNavnForNavIdent(navIdent).leftOrNull()
            feil.shouldBeInstanceOf<KanIkkeHenteNavnForNavIdent.KallFeilet>()
        }
    }

    @Test
    fun `ingen brukere i svaret gir FantIkkeEntydigBruker`() {
        val httpKlient = HttpKlientFake().apply { enqueueBrukere() }

        runTest {
            val feil = client(httpKlient).hentNavnForNavIdent(navIdent).leftOrNull()
            feil.shouldBeInstanceOf<KanIkkeHenteNavnForNavIdent.FantIkkeEntydigBruker>().antallTreff shouldBe 0
        }
    }

    @Test
    fun `flere brukere i svaret gir FantIkkeEntydigBruker`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueBrukere(
                MicrosoftGraphResponse(givenName = "Saks", surname = "Behandler"),
                MicrosoftGraphResponse(givenName = "Beslutter", surname = "Besluttersen"),
            )
        }

        runTest {
            val feil = client(httpKlient).hentNavnForNavIdent(navIdent).leftOrNull()
            feil.shouldBeInstanceOf<KanIkkeHenteNavnForNavIdent.FantIkkeEntydigBruker>().antallTreff shouldBe 2
        }
    }

    @Test
    fun `blankt navn gir NavnetErBlankt`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueBrukere(MicrosoftGraphResponse(givenName = " ", surname = " "))
        }

        runTest {
            val feil = client(httpKlient).hentNavnForNavIdent(navIdent).leftOrNull()
            feil.shouldBeInstanceOf<KanIkkeHenteNavnForNavIdent.NavnetErBlankt>()
        }
    }

    @Test
    fun `hentNavnForNavIdentEllerKast - returnerer navnet ved suksess`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueBrukere(MicrosoftGraphResponse(givenName = "Saks", surname = "Behandler"))
        }

        runTest {
            client(httpKlient).hentNavnForNavIdentEllerKast(navIdent) shouldBe "Saks Behandler"
        }
    }

    @Test
    fun `hentNavnForNavIdentEllerKast - kaster med nøytral beskrivelse ved feil`() {
        val httpKlient = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 500, body = "rå responsbody") }

        runTest {
            val exception = shouldThrow<IllegalStateException> {
                client(httpKlient).hentNavnForNavIdentEllerKast(navIdent)
            }
            exception.message shouldBe "Kunne ikke hente navn for navIdent $navIdent: KallFeilet(UventetStatus)"
        }
    }

    @Test
    fun `hentNavnForNavIdentEllerKast - kaster med antall treff når brukeren ikke er entydig`() {
        val httpKlient = HttpKlientFake().apply { enqueueBrukere() }

        runTest {
            val exception = shouldThrow<IllegalStateException> {
                client(httpKlient).hentNavnForNavIdentEllerKast(navIdent)
            }
            exception.message shouldBe "Kunne ikke hente navn for navIdent $navIdent: FantIkkeEntydigBruker(antallTreff=0)"
        }
    }

    @Test
    fun `hentNavnForNavIdentEllerKast - kaster når navnet er blankt`() {
        val httpKlient = HttpKlientFake().apply {
            enqueueBrukere(MicrosoftGraphResponse(givenName = " ", surname = " "))
        }

        runTest {
            val exception = shouldThrow<IllegalStateException> {
                client(httpKlient).hentNavnForNavIdentEllerKast(navIdent)
            }
            exception.message shouldBe "Kunne ikke hente navn for navIdent $navIdent: NavnetErBlankt"
        }
    }
}
