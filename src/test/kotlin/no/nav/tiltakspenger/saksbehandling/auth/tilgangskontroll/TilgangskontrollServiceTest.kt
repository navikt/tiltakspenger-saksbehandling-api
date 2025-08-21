package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.AvvistTilgangResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangBulkResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinClient
import org.junit.jupiter.api.Test

class TilgangskontrollServiceTest {
    private val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
    private val tilgangskontrollService = TilgangskontrollService(tilgangsmaskinClient)
    private val fnr = Fnr.random()
    private val fnr2 = Fnr.random()
    private val fnrs = listOf(fnr, fnr2)

    @Test
    fun `harTilgangTilPerson - har tilgang - returnerer true`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns true.right()

        tilgangskontrollService.harTilgangTilPerson(fnr, "token").getOrNull() shouldBe true
    }

    @Test
    fun `harTilgangTilPerson - har ikke tilgang - returnerer AvvistTilgang`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } returns AvvistTilgangResponse(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            title = "AVVIST_STRENGT_FORTROLIG_ADRESSE",
            status = 403,
            brukerIdent = fnr.verdi,
            navIdent = "Z12345",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
        ).left()

        tilgangskontrollService.harTilgangTilPerson(fnr, "token")
            .leftOrNull() shouldBe IkkeTilgangDetaljer.AvvistTilgang(
            regel = "AVVIST_STRENGT_FORTROLIG_ADRESSE",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
        )
    }

    @Test
    fun `harTilgangTilPerson - kaster feil - returnerer UkjentFeil`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPerson(fnr, any()) } throws RuntimeException("feilmelding")

        tilgangskontrollService.harTilgangTilPerson(fnr, "token")
            .leftOrNull() shouldBe IkkeTilgangDetaljer.UkjentFeil("feilmelding")
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

        val tilgangsmap = tilgangskontrollService.harTilgangTilPersoner(fnrs, "token").getOrNull()

        tilgangsmap shouldNotBe null
        tilgangsmap!!.size shouldBe 2
        tilgangsmap[fnr] shouldBe true
        tilgangsmap[fnr2] shouldBe false
    }

    @Test
    fun `harTilgangTilPersoner - kaster feil - returnerer UkjentFeil`() = runTest {
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } throws RuntimeException("feilmelding")

        tilgangskontrollService.harTilgangTilPersoner(fnrs, "token")
            .leftOrNull() shouldBe IkkeTilgangDetaljer.UkjentFeil("feilmelding")
    }
}
