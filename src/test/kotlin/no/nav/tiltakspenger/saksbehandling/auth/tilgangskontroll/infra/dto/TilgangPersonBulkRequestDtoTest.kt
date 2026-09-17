package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import org.junit.jupiter.api.Test

/**
 * Pinner wireformatet mot tilgangsmaskinen.
 * Bulk-requesten delegerer til listen for å serialiseres som et rent json-array, slik endepunktet forventer.
 */
class TilgangPersonBulkRequestDtoTest {

    @Test
    fun `bulk-requesten serialiseres til et array av strings, ikke et objekt`() {
        val fnr1 = Fnr.random()
        val fnr2 = Fnr.random()

        objectMapper.writeValueAsString(TilgangPersonBulkRequestDto.fraFnrs(listOf(fnr1, fnr2))) shouldEqualJson
            """["${fnr1.verdi}", "${fnr2.verdi}"]"""
    }
}
