package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingsutfallGammel
import org.junit.jupiter.api.Test

class BehandlingsutfallDbTest {
    @Test
    fun `mapper til db type`() {
        BehandlingsutfallGammel.INNVILGELSE.toDb() shouldBe "INNVILGELSE"
        BehandlingsutfallGammel.AVSLAG.toDb() shouldBe "AVSLAG"
        BehandlingsutfallGammel.STANS.toDb() shouldBe "STANS"
    }

    @Test
    fun `mapper til domene type`() {
        BehandlingsutfallDb.INNVILGELSE.toDomain() shouldBe BehandlingsutfallGammel.INNVILGELSE
        BehandlingsutfallDb.AVSLAG.toDomain() shouldBe BehandlingsutfallGammel.AVSLAG
        BehandlingsutfallDb.STANS.toDomain() shouldBe BehandlingsutfallGammel.STANS
    }
}
