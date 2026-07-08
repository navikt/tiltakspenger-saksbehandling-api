package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.time.Duration

internal class NavkontorServiceTest {
    private val fnr = Fnr.random()

    private fun klientSomSvarer(
        resultat: Either<KanIkkeHenteNavkontor, NavkontorMedMetadata>,
    ) = object : NavkontorKlient {
        override suspend fun hentNavkontor(
            fnr: Fnr,
            loggkontekst: String,
        ) = resultat
    }

    @Test
    fun `returnerer navkontor når klienten svarer OK`() {
        val navkontor = Navkontor(kontornummer = "1234", kontornavn = "Nav Askim")
        val service = NavkontorService(
            klientSomSvarer(
                NavkontorMedMetadata(
                    navkontor = navkontor,
                    brukteKlient = BruktNavkontorKlient.VEILARBOPPFOLGING,
                ).right(),
            ),
        )

        runTest {
            service.hentNavkontor(fnr, loggkontekst = "sakId: 123") shouldBe navkontor
        }
    }

    @Test
    fun `feil fra klienten kaster med nøytral beskrivelse - uten rå request-data med persondata`() {
        val service = NavkontorService(
            klientSomSvarer(
                KanIkkeHenteNavkontor.KallFeilet(
                    httpKlientError = HttpKlientError.NetworkError(
                        throwable = IOException("connection refused"),
                        metadata = metadataMedFnrIRequest(),
                    ),
                ).left(),
            ),
        )

        runTest {
            val exception = shouldThrow<IllegalStateException> {
                service.hentNavkontor(fnr, loggkontekst = "sakId: 123")
            }
            exception.message shouldBe "Kunne ikke hente navkontor: KallFeilet(NetworkError)"
            exception.message shouldNotContain fnr.verdi
        }
    }

    /** Rå request med fnr, slik at testen fanger opp om beskrivelsen lekker request-data ([Klientkall] avledes herfra). */
    private fun metadataMedFnrIRequest() = HttpKlientMetadata(
        rawRequestString = """POST http://veilarboppfolging.test {"fnr":"${fnr.verdi}"}""",
        rawResponseString = null,
        requestHeaders = emptyMap(),
        responseHeaders = emptyMap(),
        statusCode = null,
        attempts = 1,
        attemptDurations = listOf(Duration.ZERO),
        totalDuration = Duration.ZERO,
        tidsstempler = HttpKlientTidsstempler.INGEN,
    )
}
