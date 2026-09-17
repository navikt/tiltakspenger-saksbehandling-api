package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import org.junit.jupiter.api.Test

class TilgangPiiMaskeringTest {
    private val fnr = Fnr.random()

    @Test
    fun `AvvistMetadata maskerer brukerIdent i toString`() {
        val metadata = AvvistMetadata(
            type = "https://example.com/type",
            avvisningskode = "AVVIST_SKJERMING",
            navIdent = "Z12345",
            brukerIdent = fnr,
        )
        metadata.toString() shouldNotContain fnr.verdi
        metadata.toString() shouldBe "AvvistMetadata(type=https://example.com/type, avvisningskode=AVVIST_SKJERMING, navIdent=Z12345, brukerIdent=*****)"
    }

    @Test
    fun `TilgangResponse maskerer brukerId i toString`() {
        val response = TilgangBulkResponseDto.TilgangResponse(
            brukerId = fnr.verdi,
            status = 204,
            detaljer = null,
        )
        response.toString() shouldNotContain fnr.verdi
        response.toString() shouldBe "TilgangResponse(brukerId=*****, status=204)"
    }

    @Test
    fun `TilgangPersonBulkRequestDto maskerer brukerIder i toString`() {
        val request = TilgangPersonBulkRequestDto.fraFnrs(listOf(fnr))
        request.toString() shouldNotContain fnr.verdi
        request.toString() shouldBe "TilgangPersonBulkRequestDto(brukerIder=*****)"
    }

    /**
     * DTO-en bærer ikke lenger fnr-et; det kommer inn via [AvvistTilgangResponseDto.tilAvvistTilgangsvurdering].
     * Hele vurderingen skal likevel være maskert.
     */
    @Test
    fun `avvist tilgangsvurdering maskerer brukerIdent i toString`() {
        val vurdering = AvvistTilgangResponseDto(
            type = "type",
            title = "AVVIST_SKJERMING",
            status = 403,
            navIdent = "Z12345",
            begrunnelse = "Du har ikke tilgang",
        ).tilAvvistTilgangsvurdering(fnr)
        vurdering.metadata.brukerIdent shouldBe fnr
        vurdering.toString() shouldNotContain fnr.verdi
    }
}
