package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.Tidsgrenser
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.common.Loggfanger
import no.nav.tiltakspenger.saksbehandling.common.Sikkerloggfanger
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.time.Duration.Companion.seconds

class TilgangskontrollServiceTest {
    private val fnr = Fnr.random()
    private val fnr2 = Fnr.random()
    private val fnrs = listOf(fnr, fnr2)
    private val saksbehandler = ObjectMother.saksbehandler()
    private val sakId = SakId.random()
    private val clock = ObjectMother.clock
    private val saksnummer = Saksnummer.genererSaknummer(løpenr = "0001", clock = clock)

    private val uventetFeil = TilgangskontrollFeil.Uventet(
        HttpKlientError.UventetStatus(
            statusCode = 500,
            body = "",
            metadata = HttpKlientMetadata(
                method = "POST",
                uri = URI.create("http://tilgangsmaskin.test/api/v1/kjerne"),
                uriSynlighet = UriSynlighet.VanligLogg,
                tidsgrenser = Tidsgrenser(svar = 30.seconds, oppkobling = 10.seconds),
                rawRequestString = "",
                rawResponseString = null,
                requestHeaders = emptyMap(),
                responseHeaders = emptyMap(),
                statusCode = 500,
                attempts = 1,
                attemptDurations = emptyList(),
                totalDuration = kotlin.time.Duration.ZERO,
                tidsstempler = HttpKlientTidsstempler.INGEN,
            ),
        ),
    )

    private val avvistVurdering = Tilgangsvurdering.Avvist(
        årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
        begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
        metadata = AvvistMetadata(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            avvisningskode = "AVVIST_FORTROLIG_ADRESSE",
            navIdent = "Z12345",
            brukerIdent = fnr,
        ),
    )

    @Test
    fun `harTilgangTilPerson - har tilgang - kaster ikke feil`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent.right()

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    @Test
    fun `harTilgangTilPerson - har ikke tilgang - kaster TilgangException`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns avvistVurdering.right()

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    /**
     * En avvisningskode vi ikke kjenner, er fortsatt en avvisning.
     * Saksbehandleren skal få 403 med en generell melding, ikke en 500.
     * Koden telles opp, slik at alarmen fanger den også når den kom fra et enkeltoppslag.
     */
    @Test
    fun `harTilgangTilPerson - ukjent avvisningskode - kaster TilgangException med annet-kode og teller opp koden`() = runTest {
        val kode = "AVVIST_EN_REGEL_BARE_ENKELTOPPSLAGSTESTEN_BRUKER"
        val førTelling = tellerVerdiFor(kode)
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Avvist(
            årsak = TilgangsvurderingAvvistÅrsak.UKJENT,
            begrunnelse = "Avvist av en regel vi ikke kjenner",
            metadata = AvvistMetadata(
                type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                avvisningskode = kode,
                navIdent = "Z12345",
                brukerIdent = fnr,
            ),
        ).right()

        val exception = shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }

        exception.toErrorJson().kode shouldBe "tilgang_nektet_annet"
        tellerVerdiFor(kode) shouldBe førTelling + 1.0
    }

    @Test
    fun `harTilgangTilPerson - generell feil - kaster RuntimeException`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns uventetFeil.left()

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - har tilgang - kaster ikke feil`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val sakService = mockk<SakService>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, sakService)
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent.right()

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - har ikke tilgang - kaster TilgangException`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val sakService = mockk<SakService>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, sakService)
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns avvistVurdering.right()

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - generell feil - kaster RuntimeException`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val sakService = mockk<SakService>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, sakService)
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns uventetFeil.left()

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - fant ikke sak - kaster TilgangException`() = runTest {
        val sakService = mockk<SakService>()
        val tilgangskontrollService = tilgangskontrollService(mockk<TilgangsmaskinClient>(), sakService)
        coEvery { sakService.hentFnrForSakId(sakId) } throws IkkeFunnetException("Fant ikke sak med sakId $sakId")

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - har tilgang - kaster ikke feil`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val sakService = mockk<SakService>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, sakService)
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent.right()

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - har ikke tilgang - kaster TilgangException`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val sakService = mockk<SakService>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, sakService)
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns avvistVurdering.right()

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - generell feil - kaster RunTimeException`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val sakService = mockk<SakService>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, sakService)
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns uventetFeil.left()

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - fant ikke sak - kaster TilgangException`() = runTest {
        val sakService = mockk<SakService>()
        val tilgangskontrollService = tilgangskontrollService(mockk<TilgangsmaskinClient>(), sakService)
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } throws IkkeFunnetException("Fant ikke sak med saksnummer $saksnummer")

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersoner - har tilgang til en og ikke tilgang til annen - returnerer riktig map`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns ObjectMother.httpKlientResponse(
            body = Tilgangsvurderinger(
                perFnr = mapOf(
                    fnr to TilgangsvurderingBulk.Godkjent,
                    fnr2 to TilgangsvurderingBulk.Avvist(
                        årsak = TilgangsvurderingAvvistÅrsak.GEOGRAFISK,
                        begrunnelse = "Du har ikke geografisk tilgang",
                    ),
                ),
                ukjenteAvvisningskoder = emptySet(),
            ),
            statusCode = 207,
        ).right()

        val tilgangsmap = tilgangskontrollService
            .harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate())
            .getOrElse { throw AssertionError("Forventet Right, fikk $it") }

        tilgangsmap.size shouldBe 2
        tilgangsmap[fnr] shouldBe TilgangsvurderingBulk.Godkjent
        tilgangsmap[fnr2] shouldBe TilgangsvurderingBulk.Avvist(
            årsak = TilgangsvurderingAvvistÅrsak.GEOGRAFISK,
            begrunnelse = "Du har ikke geografisk tilgang",
        )
    }

    /** Bulkstien signaliserer med Left; det er kalleren som avgjør hva saksbehandleren skal se. */
    @Test
    fun `harTilgangTilPersoner - uventet feil - returnerer Left uten kast`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns uventetFeil.left()

        val feil = tilgangskontrollService
            .harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate())
            .fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })

        feil shouldBe uventetFeil
    }

    /** Et svar vi ikke klarte å tolke logges med vår egen beskrivelse i vanlig logg og den rå responsen i sikkerlogg. */
    @Test
    fun `harTilgangTilPersoner - ugyldig svar - returnerer Left uten kast`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        val ugyldigSvar = TilgangskontrollFeil.UgyldigSvar(
            beskrivelse = "Tilgangsmaskinen returnerte 403 uten detaljer.",
            metadata = ObjectMother.httpKlientUventetStatus(statusCode = 207, body = "ugyldig bulksvar").metadata,
        )
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns ugyldigSvar.left()

        val feil = tilgangskontrollService
            .harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate())
            .fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })

        feil shouldBe ugyldigSvar
    }

    /** ForMangeIdenter har sin egen logglinje, og skal også komme ut som Left. */
    @Test
    fun `harTilgangTilPersoner - for mange identer - returnerer Left uten kast`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns TilgangskontrollFeil.ForMangeIdenter.left()

        val feil = tilgangskontrollService
            .harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate())
            .fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })

        feil shouldBe TilgangskontrollFeil.ForMangeIdenter
    }

    /**
     * En avvisningskode vi ikke kjenner, avviser fortsatt tilgangen, men raden får ingen markør.
     * Telleren gjør avviket synlig for teamet, og koden er Tilgangsmaskinens regelnavn, ikke en personopplysning.
     */
    @Test
    fun `harTilgangTilPersoner - ukjent avvisningskode - teller opp koden og returnerer vurderingene`() = runTest {
        val kode = "AVVIST_EN_REGEL_BARE_DENNE_TESTEN_BRUKER"
        val førTelling = tellerVerdiFor(kode)
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val tilgangskontrollService = tilgangskontrollService(tilgangsmaskinClient, mockk<SakService>())
        val avvist = TilgangsvurderingBulk.Avvist(
            årsak = TilgangsvurderingAvvistÅrsak.UKJENT,
            begrunnelse = "Avvist av en regel vi ikke kjenner",
        )
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns ObjectMother.httpKlientResponse(
            body = Tilgangsvurderinger(
                perFnr = mapOf(fnr to TilgangsvurderingBulk.Godkjent, fnr2 to avvist),
                ukjenteAvvisningskoder = setOf(kode),
            ),
            statusCode = 207,
        ).right()

        val tilgangsmap = tilgangskontrollService
            .harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate())
            .getOrElse { throw AssertionError("Forventet Right, fikk $it") }

        tilgangsmap[fnr] shouldBe TilgangsvurderingBulk.Godkjent
        tilgangsmap[fnr2] shouldBe avvist
        tellerVerdiFor(kode) shouldBe førTelling + 1.0
    }

    /**
     * Servicen tar loggerne som konstruktørparametere uten default, så hver test får sine egne fangere.
     * Fangerne deler ingen tilstand, slik at testene tåler å kjøre parallelt.
     */
    private fun tilgangskontrollService(
        tilgangsmaskinClient: TilgangsmaskinClient,
        sakService: SakService,
    ) = TilgangskontrollService(
        tilgangsmaskinClient = tilgangsmaskinClient,
        sakService = sakService,
        log = Loggfanger(TilgangskontrollService::class.java.name),
        sikkerlogg = Sikkerloggfanger(),
    )

    /**
     * Telleren ligger på det globale registeret og nullstilles ikke mellom tester.
     * Testen bruker derfor en kode ingen andre tester rører, og sammenligner før og etter.
     */
    private fun tellerVerdiFor(kode: String): Double {
        return MetricRegister.TILGANGSMASKIN_UKJENT_AVVISNINGSKODE.collect()
            .dataPoints
            .firstOrNull { it.labels.get("kode") == kode }
            ?.value
            ?: 0.0
    }
}
