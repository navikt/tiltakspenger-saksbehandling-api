package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistTilgangResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
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
    private val saksnummer = Saksnummer.genererSaknummer(løpenr = "0001")

    @Test
    fun `harTilgangTilPerson - har tilgang - kaster ikke feil`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    @Test
    fun `harTilgangTilPerson - har ikke tilgang - kaster TilgangException`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Avvist(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
            status = 403,
            brukerIdent = fnr.verdi,
            navIdent = "Z12345",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
        )

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    @Test
    fun `harTilgangTilPerson - generell feil - kaster RuntimeException`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.GenerellFeilMotTilgangsmaskin

        shouldThrow<RuntimeException> {
            tilgangskontrollService.harTilgangTilPerson(fnr, "token", saksbehandler)
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - har tilgang - kaster ikke feil`() = runTest {
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - har ikke tilgang - kaster TilgangException`() = runTest {
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Avvist(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
            status = 403,
            brukerIdent = fnr.verdi,
            navIdent = "Z12345",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
        )

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSakId(sakId, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSakId - generell feil - kaster RuntimeException`() = runTest {
        coEvery { sakService.hentFnrForSakId(sakId) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.GenerellFeilMotTilgangsmaskin

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
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Godkjent

        shouldNotThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - har ikke tilgang - kaster TilgangException`() = runTest {
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.Avvist(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
            status = 403,
            brukerIdent = fnr.verdi,
            navIdent = "Z12345",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
        )

        shouldThrow<TilgangException> {
            tilgangskontrollService.harTilgangTilPersonForSaksnummer(saksnummer, saksbehandler, "token")
        }
    }

    @Test
    fun `harTilgangTilPersonForSaksnummer - generell feil - kaster RunTimeException`() = runTest {
        coEvery { sakService.hentFnrForSaksnummer(saksnummer) } returns fnr
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns Tilgangsvurdering.GenerellFeilMotTilgangsmaskin

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
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns TilgangBulkResponse(
            ansattId = "Z123456",
            resultater = listOf(
                TilgangBulkResponse.TilgangResponse(
                    brukerId = fnr.verdi,
                    status = 204,
                ),
                TilgangBulkResponse.TilgangResponse(
                    brukerId = fnr2.verdi,
                    status = 403,
                    detaljer = AvvistTilgangResponse(
                        type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                        title = "AVVIST_STRENGT_FORTROLIG_ADRESSE",
                        status = 403,
                        brukerIdent = fnr.verdi,
                        navIdent = "Z12345",
                        begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
                    ),
                ),
            ),
        )

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
