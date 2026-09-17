package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollFeil
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponseDto
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

class TilgangsmaskinHttpClientTest {

    private val fnr = ObjectMother.gyldigFnr()
    private val fnr2 = ObjectMother.gyldigFnr()
    private val fnr3 = ObjectMother.gyldigFnr()

    private fun nyClient(
        texasClient: TexasClient,
        transport: FakeHttpTransport,
    ) = TilgangsmaskinHttpClient(
        baseUrl = "https://tilgangsmaskin.test",
        scope = "scope",
        texasClient = texasClient,
        clock = fixedClock,
        transport = transport,
    )

    private fun texasClientMedOboVeksling(): TexasClient = mockk<TexasClient>().also {
        coEvery {
            it.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } returns AccessToken("obo-token", Instant.parse("2026-01-01T00:00:00Z"))
    }

    @Test
    fun `harTilgangTilPerson returnerer godkjent for 204`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøTomRespons(statusCode = 204)

        val result = client.harTilgangTilPerson(fnr, "token")

        result.fold({ throw AssertionError(it) }, { it }) shouldBe Tilgangsvurdering.Godkjent
    }

    @ParameterizedTest
    @CsvSource(
        "AVVIST_STRENGT_FORTROLIG_ADRESSE, STRENGT_FORTROLIG",
        "AVVIST_STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG_UTLAND",
        "AVVIST_FORTROLIG_ADRESSE, FORTROLIG",
        "AVVIST_SKJERMING, SKJERMET",
    )
    fun `harTilgangTilPerson bevarer avvisning for adressebeskyttelse og skjerming`(
        kode: String,
        årsak: TilgangsvurderingAvvistÅrsak,
    ) = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøJson(
            json = """
                {
                  "type": "https://example.com/type",
                  "title": "$kode",
                  "status": 403,
                  "brukerIdent": "${fnr.verdi}",
                  "navIdent": "Z12345",
                  "begrunnelse": "Du har ikke tilgang"
                }
            """.trimIndent(),
            statusCode = 403,
        )

        val result = client.harTilgangTilPerson(fnr, "token")

        val vurdering = result.fold({ throw AssertionError(it) }, { it })
        vurdering shouldBe Tilgangsvurdering.Avvist(
            årsak = årsak,
            begrunnelse = "Du har ikke tilgang",
            metadata = AvvistMetadata(
                type = "https://example.com/type",
                avvisningskode = kode,
                navIdent = "Z12345",
                brukerIdent = fnr,
            ),
        )
    }

    /**
     * Rå json slik Tilgangsmaskinen faktisk svarer, med feltene vi ikke modellerer.
     * Da beviser testen at deserialiseringen tåler dem, i stedet for bare å speile vår egen DTO.
     */
    @Test
    fun `harTilgangTilPersoner bevarer godkjent og avvisningskodene fra 207-svar`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøJson(
            json = """
                {
                  "ansattId": "Z990883",
                  "resultater": [
                    {"brukerId": "${fnr.verdi}", "status": 204},
                    {
                      "brukerId": "${fnr2.verdi}",
                      "status": 403,
                      "detaljer": {
                        "type": "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                        "title": "AVVIST_STRENGT_FORTROLIG_ADRESSE",
                        "status": 403,
                        "instance": "Z990883/${fnr2.verdi}",
                        "brukerIdent": "${fnr2.verdi}",
                        "navIdent": "Z990883",
                        "begrunnelse": "Du har ikke tilgang til brukere med strengt fortrolig adresse",
                        "traceId": "f85c9caa87a57b6dfde1068ce97f10a5",
                        "kanOverstyres": false
                      }
                    },
                    {
                      "brukerId": "${fnr3.verdi}",
                      "status": 403,
                      "detaljer": {
                        "type": "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                        "title": "AVVIST_GEOGRAFISK",
                        "status": 403,
                        "instance": "Z990883/${fnr3.verdi}",
                        "brukerIdent": "${fnr3.verdi}",
                        "navIdent": "Z990883",
                        "begrunnelse": "Du har ikke geografisk tilgang",
                        "traceId": "f85c9caa87a57b6dfde1068ce97f10a5",
                        "kanOverstyres": true
                      }
                    }
                  ]
                }
            """.trimIndent(),
            statusCode = 207,
        )

        val result = client.harTilgangTilPersoner(listOf(fnr, fnr2, fnr3), "token")

        val tilgangsvurderinger = result.fold({ throw AssertionError(it) }, { it.body })
        tilgangsvurderinger.perFnr shouldBe mapOf(
            fnr to TilgangsvurderingBulk.Godkjent,
            fnr2 to TilgangsvurderingBulk.Avvist(
                årsak = TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG,
                begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
            ),
            // Geografisk tilgang er en overstyrbar regel utenfor kjernesettet, og bulkoppslaget er det eneste som kan gi den.
            fnr3 to TilgangsvurderingBulk.Avvist(
                årsak = TilgangsvurderingAvvistÅrsak.GEOGRAFISK,
                begrunnelse = "Du har ikke geografisk tilgang",
            ),
        )
        tilgangsvurderinger.ukjenteAvvisningskoder shouldBe emptySet()
    }

    @Test
    fun `harTilgangTilPersoner gir UgyldigSvar når resultat mangler`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøJson(
            TilgangBulkResponseDto(
                resultater = listOf(
                    TilgangBulkResponseDto.TilgangResponse(
                        brukerId = fnr.verdi,
                        status = 204,
                        detaljer = null,
                    ),
                ),
            ),
            statusCode = 207,
        )

        val result = client.harTilgangTilPersoner(listOf(fnr, fnr2), "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<TilgangskontrollFeil.UgyldigSvar>()
            .beskrivelse shouldBe "Tilgangsmaskinen returnerte ikke resultater for nøyaktig de etterspurte personene."
    }

    /** 403 er en avvisning, og uten detaljer vet vi verken hvilken regel som avviste eller hva saksbehandleren skal få se. */
    @Test
    fun `harTilgangTilPersoner gir UgyldigSvar for 403 uten detaljer`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøJson(
            TilgangBulkResponseDto(
                resultater = listOf(
                    TilgangBulkResponseDto.TilgangResponse(
                        brukerId = fnr.verdi,
                        status = 403,
                        detaljer = null,
                    ),
                ),
            ),
            statusCode = 207,
        )

        val result = client.harTilgangTilPersoner(listOf(fnr), "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<TilgangskontrollFeil.UgyldigSvar>()
            .beskrivelse shouldBe "Tilgangsmaskinen returnerte 403 uten detaljer."
    }

    /** En status vi ikke kjenner, kan verken tolkes som tilgang eller avvisning, og skal felle bulksvaret. */
    @Test
    fun `harTilgangTilPersoner gir UgyldigSvar for ukjent status i bulksvaret`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøJson(
            TilgangBulkResponseDto(
                resultater = listOf(
                    TilgangBulkResponseDto.TilgangResponse(
                        brukerId = fnr.verdi,
                        status = 500,
                        detaljer = null,
                    ),
                ),
            ),
            statusCode = 207,
        )

        val result = client.harTilgangTilPersoner(listOf(fnr), "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<TilgangskontrollFeil.UgyldigSvar>()
            .beskrivelse shouldBe "Tilgangsmaskinen returnerte ukjent status 500 i bulksvaret."
    }

    @Test
    fun `harTilgangTilPersoner gir UgyldigSvar for ugyldig brukerId`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøJson(
            TilgangBulkResponseDto(
                resultater = listOf(
                    TilgangBulkResponseDto.TilgangResponse(
                        brukerId = "123",
                        status = 204,
                        detaljer = null,
                    ),
                ),
            ),
            statusCode = 207,
        )

        val result = client.harTilgangTilPersoner(listOf(fnr), "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<TilgangskontrollFeil.UgyldigSvar>()
            .beskrivelse shouldBe "Tilgangsmaskinen returnerte en ugyldig brukerId."
    }

    @Test
    fun `harTilgangTilPersoner gir Uventet når token-exchange feiler`() = runTest {
        val texasClient = mockk<TexasClient>()
        coEvery {
            texasClient.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } throws RuntimeException("boom")
        val client = nyClient(texasClient, FakeHttpTransport())

        val result = client.harTilgangTilPersoner(listOf(fnr), "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<TilgangskontrollFeil.Uventet>()
    }

    /**
     * Statuskoden har allerede avgjort tilgangen, så en kode vi ikke kjenner, er metadata og skal ikke felle kallet.
     * Den rå koden følger med i metadata, slik at loggen viser hvilken regel Tilgangsmaskinen avviste på.
     */
    @Test
    fun `harTilgangTilPerson gir avvist med ukjent årsak for ukjent regel`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøJson(
            json = """
                {
                  "type": "https://example.com/type",
                  "title": "AVVIST_EN_NY_REGEL",
                  "status": 403,
                  "brukerIdent": "${fnr.verdi}",
                  "navIdent": "Z12345",
                  "begrunnelse": "Ukjent regel"
                }
            """.trimIndent(),
            statusCode = 403,
        )

        val result = client.harTilgangTilPerson(fnr, "token")

        result.fold({ throw AssertionError(it) }, { it }) shouldBe Tilgangsvurdering.Avvist(
            årsak = TilgangsvurderingAvvistÅrsak.UKJENT,
            begrunnelse = "Ukjent regel",
            metadata = AvvistMetadata(
                type = "https://example.com/type",
                avvisningskode = "AVVIST_EN_NY_REGEL",
                navIdent = "Z12345",
                brukerIdent = fnr,
            ),
        )
    }

    /**
     * En ukjent kode gjelder én rad, og skal verken felle bulksvaret eller de radene vi kjenner igjen.
     * Den rå koden returneres sammen med vurderingene, siden den kategoriserte årsaken ikke sier hvilken regel som var ukjent.
     */
    @Test
    fun `harTilgangTilPersoner gir avvist med ukjent årsak for ukjent avvisningskode`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøJson(
            json = """
                {
                  "resultater": [
                    {"brukerId": "${fnr.verdi}", "status": 204},
                    {
                      "brukerId": "${fnr2.verdi}",
                      "status": 403,
                      "detaljer": {
                        "title": "AVVIST_EN_NY_REGEL",
                        "begrunnelse": "Ukjent regel"
                      }
                    }
                  ]
                }
            """.trimIndent(),
            statusCode = 207,
        )

        val result = client.harTilgangTilPersoner(listOf(fnr, fnr2), "token")

        val tilgangsvurderinger = result.fold({ throw AssertionError(it) }, { it.body })
        tilgangsvurderinger.perFnr shouldBe mapOf(
            fnr to TilgangsvurderingBulk.Godkjent,
            fnr2 to TilgangsvurderingBulk.Avvist(
                årsak = TilgangsvurderingAvvistÅrsak.UKJENT,
                begrunnelse = "Ukjent regel",
            ),
        )
        tilgangsvurderinger.ukjenteAvvisningskoder shouldBe setOf("AVVIST_EN_NY_REGEL")
    }

    @Test
    fun `harTilgangTilPerson gir Uventet for andre feilstatuser`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøStatus(statusCode = 500, body = "intern serverfeil", contentType = "text/plain")

        val result = client.harTilgangTilPerson(fnr, "token")

        result.fold({ it }, { throw AssertionError(it) }).shouldBeInstanceOf<TilgangskontrollFeil.Uventet>()
    }

    /** En feil uten mottatt respons (her: nettverksfeil) skal også ende som Uventet, ikke tolkes som avvist tilgang. */
    @Test
    fun `harTilgangTilPerson gir Uventet for nettverksfeil`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøKast(java.io.IOException("simulert nettverksfeil"))

        val result = client.harTilgangTilPerson(fnr, "token")

        result.fold({ it }, { throw AssertionError(it) }).shouldBeInstanceOf<TilgangskontrollFeil.Uventet>()
    }

    @Test
    fun `bygger produksjonstransport når transport ikke sendes inn`() {
        TilgangsmaskinHttpClient(
            baseUrl = "https://tilgangsmaskin.test",
            scope = "scope",
            texasClient = mockk<TexasClient>(),
            clock = fixedClock,
        )
    }

    @Test
    fun `harTilgangTilPerson gir Uventet når token-exchange feiler`() = runTest {
        val texasClient = mockk<TexasClient>()
        coEvery {
            texasClient.exchangeToken(
                userToken = "token",
                audienceTarget = "scope",
                identityProvider = IdentityProvider.AZUREAD,
            )
        } throws RuntimeException("boom")
        val client = nyClient(texasClient, FakeHttpTransport())

        val result = client.harTilgangTilPerson(fnr, "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") })
            .shouldBeInstanceOf<TilgangskontrollFeil.Uventet>()
    }

    @Test
    fun `harTilgangTilPersoner gir ForMangeIdenter for 413`() = runTest {
        val fakeTransport = FakeHttpTransport()
        val client = nyClient(texasClientMedOboVeksling(), fakeTransport)
        fakeTransport.leggIKøStatus(statusCode = 413, body = "For mange identer", contentType = "text/plain")

        val result = client.harTilgangTilPersoner(listOf(fnr), "token")

        result.fold({ it }, { throw AssertionError("Forventet Left, fikk $it") }) shouldBe
            TilgangskontrollFeil.ForMangeIdenter
    }
}
