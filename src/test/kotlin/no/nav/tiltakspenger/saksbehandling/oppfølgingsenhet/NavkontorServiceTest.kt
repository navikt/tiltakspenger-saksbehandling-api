package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.Level
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.Tidsgrenser
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.saksbehandling.common.Loggfanger
import no.nav.tiltakspenger.saksbehandling.common.Sikkerloggfanger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URI
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NavkontorServiceTest {
    private val fnr = Fnr.random()
    private val loggkontekst = "sakId: 123"

    /** Fangerne lages per test, siden testinstansen deles mellom testene i klassen. */
    private class Oppsett(
        resultat: Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata>,
    ) {
        val logger = Loggfanger(NavkontorService::class.java.name)
        val sikkerlogg = Sikkerloggfanger()
        val service = NavkontorService(
            kontorhistorikkKlient = KontorhistorikkFakeKlient { resultat },
            logger = logger,
            sikkerlogg = sikkerlogg,
        )
    }

    private fun innslag(
        kontorId: String,
        kontorType: KontorType,
        endretTidspunkt: String = "2024-05-01T10:00:00",
    ) = Kontorhistorikkinnslag(
        kontorId = kontorId,
        kontorNavn = "Nav $kontorId",
        kontorType = kontorType,
        endretTidspunkt = LocalDateTime.parse(endretTidspunkt),
    )

    private fun medInnslag(vararg innslag: Kontorhistorikkinnslag) = KontorhistorikkMedMetadata(
        kontorhistorikk = Kontorhistorikk(innslag.toList()),
        httpKlientMetadata = metadataMedFnrIRequest(statusCode = 200, rawResponseString = """{"kontorId":"0220"}"""),
    ).right()

    @Test
    fun `returnerer nyeste aktuelle kontor fra kontorhistorikken uten å logge`() {
        val oppsett = Oppsett(
            medInnslag(
                innslag(kontorId = "0220", kontorType = KontorType.ARENA),
                innslag(kontorId = "1234", kontorType = KontorType.ARBEIDSOPPFOLGING),
                innslag(kontorId = "9999", kontorType = KontorType.GEOGRAFISK_TILKNYTNING),
            ),
        )

        runTest {
            oppsett.service.hentNavkontor(fnr, loggkontekst) shouldBe Navkontor(kontornummer = "1234", kontornavn = "Nav 1234")
        }
        oppsett.logger.logglinjer shouldBe emptyList()
        oppsett.sikkerlogg.sikkerlogglinjer shouldBe emptyList()
    }

    @Test
    fun `kaster og logger når kontorhistorikken ikke har et aktuelt kontor`() {
        val oppsett = Oppsett(medInnslag())

        runTest {
            val exception = shouldThrow<IllegalStateException> { oppsett.service.hentNavkontor(fnr, loggkontekst) }
            exception.message shouldBe "Kunne ikke hente navkontor: kontorhistorikken har ikke et aktuelt kontor"
        }
        oppsett.logger.linjerPå(Level.ERROR).single().melding!!.let {
            it shouldContain loggkontekst
            it shouldNotContain fnr.verdi
        }
        oppsett.sikkerlogg.linjerPå(Sikkerloggfanger.Nivå.ERROR).single().melding!! shouldContain fnr.verdi
    }

    @Test
    fun `kallfeil kaster med nøytral beskrivelse og logges med rådata kun til sikkerlogg`() {
        val oppsett = Oppsett(
            KanIkkeHenteKontorhistorikk.KallFeilet(
                httpKlientError = HttpKlientError.NetworkError(
                    throwable = IOException("connection refused"),
                    metadata = metadataMedFnrIRequest(),
                ),
            ).left(),
        )

        runTest {
            val exception = shouldThrow<IllegalStateException> { oppsett.service.hentNavkontor(fnr, loggkontekst) }
            exception.message shouldBe "Kunne ikke hente navkontor: KallFeilet(NetworkError)"
            exception.message shouldNotContain fnr.verdi
        }
        oppsett.logger.linjerPå(Level.ERROR).single().melding!!.let {
            it shouldContain loggkontekst
            it shouldNotContain fnr.verdi
        }
        oppsett.sikkerlogg.linjerPå(Sikkerloggfanger.Nivå.ERROR).single().melding!! shouldContain fnr.verdi
    }

    @Test
    fun `uventet http-status kaster og logges`() {
        val oppsett = Oppsett(
            KanIkkeHenteKontorhistorikk.UventetHttpStatus(
                httpKlientError = ObjectMother.httpKlientUventetStatus(statusCode = 503),
            ).left(),
        )

        runTest {
            shouldThrow<IllegalStateException> { oppsett.service.hentNavkontor(fnr, loggkontekst) }
                .message shouldBe "Kunne ikke hente navkontor: UventetHttpStatus(status=503)"
        }
        oppsett.logger.linjerPå(Level.ERROR) shouldHaveSize 1
        oppsett.sikkerlogg.linjerPå(Sikkerloggfanger.Nivå.ERROR) shouldHaveSize 1
    }

    @Test
    fun `GraphQL-feil kaster og logges med rådata kun til sikkerlogg`() {
        val oppsett = Oppsett(
            KanIkkeHenteKontorhistorikk.GraphQlFeil(httpKlientMetadata = metadataMedFnrIRequest(statusCode = 200)).left(),
        )

        runTest {
            shouldThrow<IllegalStateException> { oppsett.service.hentNavkontor(fnr, loggkontekst) }
                .message shouldBe "Kunne ikke hente navkontor: GraphQlFeil"
        }
        oppsett.logger.linjerPå(Level.ERROR).single().melding!!.let {
            it shouldContain loggkontekst
            it shouldNotContain fnr.verdi
        }
        oppsett.sikkerlogg.linjerPå(Sikkerloggfanger.Nivå.ERROR).single().melding!! shouldContain fnr.verdi
    }

    /** Rå request med fnr, slik at testene fanger opp om rådata lekker til vanlig logg eller exception-meldinger. */
    private fun metadataMedFnrIRequest(
        statusCode: Int? = null,
        rawResponseString: String? = null,
    ) = HttpKlientMetadata(
        method = "POST",
        uri = URI.create("http://ao-oppfolgingskontor.test/graphql"),
        uriSynlighet = UriSynlighet.VanligLogg,
        tidsgrenser = Tidsgrenser(svar = 3.seconds, oppkobling = 2.seconds),
        rawRequestString = """POST http://ao-oppfolgingskontor.test/graphql {"variables":{"ident":"${fnr.verdi}"}}""",
        rawResponseString = rawResponseString,
        requestHeaders = emptyMap(),
        responseHeaders = emptyMap(),
        statusCode = statusCode,
        attempts = 1,
        attemptDurations = listOf(Duration.ZERO),
        totalDuration = Duration.ZERO,
        tidsstempler = HttpKlientTidsstempler.INGEN,
    )
}
