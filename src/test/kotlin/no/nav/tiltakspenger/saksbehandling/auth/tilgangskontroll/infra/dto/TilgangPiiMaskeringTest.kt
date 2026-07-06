package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class TilgangPiiMaskeringTest {
    private val fnr = "01010199999"

    @Test
    fun `AvvistMetadata maskerer brukerIdent i toString`() {
        val metadata = AvvistMetadata(type = "AVVIST_SKJERMING", navIdent = "Z12345", brukerIdent = fnr)
        metadata.toString() shouldNotContain fnr
        metadata.toString() shouldBe "AvvistMetadata(type=AVVIST_SKJERMING, navIdent=Z12345, brukerIdent=*****)"
    }

    @Test
    fun `TilgangResponse maskerer brukerId i toString`() {
        val response = TilgangBulkResponseDto.TilgangResponse(brukerId = fnr, status = 204)
        response.toString() shouldNotContain fnr
        response.toString() shouldBe "TilgangResponse(brukerId=*****, status=204)"
    }

    @Test
    fun `TilgangPersonRequestDto maskerer brukerId i toString`() {
        val request = TilgangPersonRequestDto(brukerId = fnr)
        request.toString() shouldNotContain fnr
        request.toString() shouldBe "TilgangPersonRequestDto(brukerId=*****)"
    }

    @Test
    fun `AvvistTilgangResponse maskerer brukerIdent i toString`() {
        val response = AvvistTilgangResponse(
            type = "type",
            title = "AVVIST_SKJERMING",
            status = 403,
            brukerIdent = fnr,
            navIdent = "Z12345",
            begrunnelse = "Du har ikke tilgang",
        )
        response.toString() shouldNotContain fnr
    }
}
