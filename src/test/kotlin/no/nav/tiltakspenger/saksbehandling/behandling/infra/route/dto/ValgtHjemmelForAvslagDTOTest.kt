package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import org.junit.jupiter.api.Test

class ValgtHjemmelForAvslagDTOTest {
    @Test
    fun `tar imot både nytt og gammelt navn på rett til dagpenger`() {
        deserializeList<ValgtHjemmelForAvslagDTO>("""["RettTilDagpenger", "ForlengetVentetid"]""")
            .toAvslagsgrunnlag() shouldBe setOf(Avslagsgrunnlag.RettTilDagpenger)
    }

    @Test
    fun `sender ut nytt navn på rett til dagpenger`() {
        setOf(Avslagsgrunnlag.RettTilDagpenger).toValgtHjemmelForAvslagDTO() shouldBe
            listOf(ValgtHjemmelForAvslagDTO.RettTilDagpenger)
    }
}
