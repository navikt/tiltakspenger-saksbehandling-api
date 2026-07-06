package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class TilgangskontrollServiceTest {
    private val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
    private val sakService = mockk<SakService>()
    private val tilgangskontrollService = TilgangskontrollService(tilgangsmaskinClient, sakService)
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

    @Test
    fun `harTilgangTilPerson - har tilgang - kaster ikke feil`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent.right()

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    @Test
    fun `harTilgangTilPerson - har ikke tilgang - kaster TilgangException`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Avvist(
            årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
            metadata = AvvistMetadata(
                type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                navIdent = "Z12345",
                brukerIdent = fnr.verdi,
            ),
        ).right()

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    @Test
    fun `harTilgangTilPerson - generell feil - kaster RuntimeException`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns uventetFeil.left()

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - har tilgang - kaster ikke feil`() = runTest {
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent.right()

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - har ikke tilgang - kaster TilgangException`() = runTest {
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Avvist(
            årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
            metadata = AvvistMetadata(
                type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                navIdent = "Z12345",
                brukerIdent = fnr.verdi,
            ),
        ).right()

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - generell feil - kaster RuntimeException`() = runTest {
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns uventetFeil.left()

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - fant ikke sak - kaster TilgangException`() = runTest {
        coEvery { sakService.hentFnrForSakId(sakId) } throws IkkeFunnetException("Fant ikke sak med sakId $sakId")

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - har tilgang - kaster ikke feil`() = runTest {
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent.right()

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - har ikke tilgang - kaster TilgangException`() = runTest {
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Avvist(
            årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
            metadata = AvvistMetadata(
                type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                navIdent = "Z12345",
                brukerIdent = fnr.verdi,
            ),
        ).right()

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - generell feil - kaster RunTimeException`() = runTest {
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns uventetFeil.left()

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - fant ikke sak - kaster TilgangException`() = runTest {
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } throws IkkeFunnetException("Fant ikke sak med saksnummer $saksnummer")

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersoner - har tilgang til en og ikke tilgang til annen - returnerer riktig map`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns mapOf(
            fnr to true,
            fnr2 to false,
        ).right()

        val tilgangsmap = tilgangskontrollService.harTilgangTilPersoner(fnrs, "token", saksbehandler)

        tilgangsmap.size shouldBe 2
        tilgangsmap[fnr] shouldBe true
        tilgangsmap[fnr2] shouldBe false
    }

    @Test
    fun `harTilgangTilPersoner - kaster feil - kaster TilgangException`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } throws RuntimeException("feilmelding")

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPersoner(fnrs, "token", saksbehandler)
        }
    }
}
