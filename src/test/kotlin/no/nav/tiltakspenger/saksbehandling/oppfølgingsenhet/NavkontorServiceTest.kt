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
    private inner class Oppsett(
        tilhørighet: Either<KanIkkeHenteOppfølgingskontor, KontorTilhørighetMedMetadata> = tilhørighet(null),
        historikk: Either<KanIkkeHenteOppfølgingskontor, KontorhistorikkMedMetadata> = historikk(),
    ) {
        val logger = Loggfanger(NavkontorService::class.java.name)
        val sikkerlogg = Sikkerloggfanger()
        val service = NavkontorService(
            oppfølgingskontorKlient = OppfølgingskontorFakeKlient(
                kontorTilhørighet = { tilhørighet },
                kontorhistorikk = { historikk },
            ),
            logger = logger,
            sikkerlogg = sikkerlogg,
        )

        val errorlinjer get() = logger.linjerPå(Level.ERROR).map { it.melding!! }
        val sikkerloggErrorlinjer get() = sikkerlogg.linjerPå(Sikkerloggfanger.Nivå.ERROR).map { it.melding!! }

        /** Vanlig logg skal aldri inneholde fnr eller loggkontekst-løse meldinger. */
        fun verifiserVanligLogg() {
            errorlinjer.forEach {
                it shouldContain loggkontekst
                it shouldNotContain fnr.verdi
            }
        }
    }

    private fun tilhørighet(kontorId: String?) = KontorTilhørighetMedMetadata(
        kontorTilhørighet = kontorId?.let { KontorTilhørighet(kontorId = it, kontorNavn = "Nav $it", kontorType = KontorType.ARBEIDSOPPFOLGING) },
        httpKlientMetadata = metadataMedFnrIRequest(statusCode = 200, rawResponseString = """{"kilde":"tilhørighet"}"""),
    ).right()

    private fun historikk(vararg innslag: Kontorhistorikkinnslag) = KontorhistorikkMedMetadata(
        kontorhistorikk = Kontorhistorikk(innslag.toList()),
        httpKlientMetadata = metadataMedFnrIRequest(statusCode = 200, rawResponseString = """{"kilde":"historikk"}"""),
    ).right()

    private fun innslag(
        kontorId: String,
        kontorType: KontorType = KontorType.ARENA,
        endretTidspunkt: String = "2024-05-01T10:00:00",
    ) = Kontorhistorikkinnslag(
        kontorId = kontorId,
        kontorNavn = null,
        kontorType = kontorType,
        endretTidspunkt = LocalDateTime.parse(endretTidspunkt),
    )

    private fun kallFeilet() = KanIkkeHenteOppfølgingskontor.KallFeilet(
        httpKlientError = HttpKlientError.NetworkError(
            throwable = IOException("connection refused"),
            metadata = metadataMedFnrIRequest(),
        ),
    ).left()

    @Test
    fun `likt svar bruker kontortilhørigheten uten å logge`() {
        val oppsett = Oppsett(
            tilhørighet = tilhørighet("1234"),
            historikk = historikk(innslag("1234")),
        )

        runTest {
            oppsett.service.hentNavkontor(fnr, loggkontekst) shouldBe Navkontor(kontornummer = "1234", kontornavn = "Nav 1234")
        }
        oppsett.logger.logglinjer shouldBe emptyList()
        oppsett.sikkerlogg.sikkerlogglinjer shouldBe emptyList()
    }

    @Test
    fun `ulikt svar bruker nyeste aktuelle kontor fra kontorhistorikken og logger avviket`() {
        val oppsett = Oppsett(
            tilhørighet = tilhørighet("1234"),
            historikk = historikk(
                innslag("0220", KontorType.ARENA, "2024-05-01T10:00:00"),
                innslag("0219", KontorType.ARENA, "2024-01-01T10:00:00"),
            ),
        )

        runTest {
            oppsett.service.hentNavkontor(fnr, loggkontekst) shouldBe Navkontor(kontornummer = "0220", kontornavn = null)
        }
        oppsett.errorlinjer.single() shouldContain "ulikt svar fra kontortilhørighet og kontorhistorikk, bruker kontorhistorikk"
        oppsett.errorlinjer.single() shouldNotContain "0220"
        oppsett.verifiserVanligLogg()
        oppsett.sikkerloggErrorlinjer.single().let {
            it shouldContain "Kontortilhørighet: kontorId=1234 (type=ARBEIDSOPPFOLGING)"
            it shouldContain "Kontorhistorikk: kontorId=0220 (type=ARENA)"
        }
    }

    @Test
    fun `ingen kontortilhørighet bruker kontorhistorikken og logger avviket`() {
        val oppsett = Oppsett(
            tilhørighet = tilhørighet(null),
            historikk = historikk(innslag("0220")),
        )

        runTest {
            oppsett.service.hentNavkontor(fnr, loggkontekst).kontornummer shouldBe "0220"
        }
        oppsett.errorlinjer.single() shouldContain "bruker kontorhistorikk"
        oppsett.verifiserVanligLogg()
        oppsett.sikkerloggErrorlinjer.single() shouldContain "Kontortilhørighet: ingen kontor"
    }

    @Test
    fun `ingen aktuelle kontor i kontorhistorikken bruker kontortilhørigheten og logger avviket`() {
        val oppsett = Oppsett(
            tilhørighet = tilhørighet("1234"),
            historikk = historikk(),
        )

        runTest {
            oppsett.service.hentNavkontor(fnr, loggkontekst).kontornummer shouldBe "1234"
        }
        oppsett.errorlinjer.single() shouldContain "bruker kontortilhørighet"
        oppsett.verifiserVanligLogg()
        oppsett.sikkerloggErrorlinjer.single() shouldContain "Kontorhistorikk: ingen kontor"
    }

    @Test
    fun `feilet kontortilhørighet-kall faller tilbake på kontorhistorikken og logger kun kallfeilen`() {
        val oppsett = Oppsett(
            tilhørighet = kallFeilet(),
            historikk = historikk(innslag("0220")),
        )

        runTest {
            oppsett.service.hentNavkontor(fnr, loggkontekst).kontornummer shouldBe "0220"
        }
        oppsett.errorlinjer.single() shouldContain "henting av kontortilhørighet"
        oppsett.verifiserVanligLogg()
        oppsett.sikkerloggErrorlinjer.single() shouldContain fnr.verdi
    }

    @Test
    fun `feilet kontorhistorikk-kall faller tilbake på kontortilhørigheten og logger kun kallfeilen`() {
        val oppsett = Oppsett(
            tilhørighet = tilhørighet("1234"),
            historikk = KanIkkeHenteOppfølgingskontor.UventetHttpStatus(
                httpKlientError = ObjectMother.httpKlientUventetStatus(statusCode = 503),
            ).left(),
        )

        runTest {
            oppsett.service.hentNavkontor(fnr, loggkontekst).kontornummer shouldBe "1234"
        }
        oppsett.errorlinjer.single() shouldContain "henting av kontorhistorikk"
        oppsett.sikkerloggErrorlinjer shouldHaveSize 1
    }

    @Test
    fun `kaster med nøytral beskrivelse når ingen av kallene lyktes`() {
        val oppsett = Oppsett(
            tilhørighet = kallFeilet(),
            historikk = KanIkkeHenteOppfølgingskontor.GraphQlFeil(httpKlientMetadata = metadataMedFnrIRequest(statusCode = 200)).left(),
        )

        runTest {
            val exception = shouldThrow<IllegalStateException> { oppsett.service.hentNavkontor(fnr, loggkontekst) }
            exception.message shouldBe "Kunne ikke hente navkontor: kontortilhørighet: KallFeilet(NetworkError), kontorhistorikk: GraphQlFeil"
        }
        // Én linje per feilet kall, pluss én for at ingen kilder hadde kontor.
        oppsett.errorlinjer shouldHaveSize 3
        oppsett.verifiserVanligLogg()
        oppsett.sikkerloggErrorlinjer shouldHaveSize 3
    }

    @Test
    fun `kaster og logger rådata til sikkerlogg når ingen av kildene har et kontor`() {
        val oppsett = Oppsett(
            tilhørighet = tilhørighet(null),
            historikk = historikk(),
        )

        runTest {
            shouldThrow<IllegalStateException> { oppsett.service.hentNavkontor(fnr, loggkontekst) }
                .message shouldBe "Kunne ikke hente navkontor: kontortilhørighet: ingen kontor, kontorhistorikk: ingen kontor"
        }
        oppsett.errorlinjer.single() shouldContain "fant ikke kontor i noen av kildene"
        oppsett.verifiserVanligLogg()
        oppsett.sikkerloggErrorlinjer.single().let {
            it shouldContain fnr.verdi
            it shouldContain """{"kilde":"tilhørighet"}"""
            it shouldContain """{"kilde":"historikk"}"""
        }
    }

    @Test
    fun `GraphQL-feil logges med rådata kun til sikkerlogg`() {
        val oppsett = Oppsett(
            tilhørighet = KanIkkeHenteOppfølgingskontor.GraphQlFeil(httpKlientMetadata = metadataMedFnrIRequest(statusCode = 200)).left(),
            historikk = historikk(innslag("0220")),
        )

        runTest {
            oppsett.service.hentNavkontor(fnr, loggkontekst)
        }
        oppsett.errorlinjer.single() shouldContain "GraphQL-feil"
        oppsett.verifiserVanligLogg()
        oppsett.sikkerloggErrorlinjer.single() shouldContain fnr.verdi
    }

    @Test
    fun `hentKontorhistorikk returnerer hele historikken uten å logge`() {
        val oppsett = Oppsett(historikk = historikk(innslag("0220"), innslag("0219")))

        runTest {
            oppsett.service.hentKontorhistorikk(fnr, loggkontekst) shouldBe Kontorhistorikk(listOf(innslag("0220"), innslag("0219")))
        }
        oppsett.logger.logglinjer shouldBe emptyList()
    }

    @Test
    fun `hentKontorhistorikk kaster med nøytral beskrivelse og logger når kallet feiler`() {
        val oppsett = Oppsett(historikk = kallFeilet())

        runTest {
            val exception = shouldThrow<IllegalStateException> { oppsett.service.hentKontorhistorikk(fnr, loggkontekst) }
            exception.message shouldBe "Kunne ikke hente kontorhistorikk: KallFeilet(NetworkError)"
        }
        oppsett.errorlinjer.single() shouldContain "henting av kontorhistorikk"
        oppsett.verifiserVanligLogg()
        oppsett.sikkerloggErrorlinjer.single() shouldContain fnr.verdi
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
