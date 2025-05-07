package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall
import org.junit.jupiter.api.Test

class BehandlingsutfallDbTest {
    @Test
    fun `mapper til db type`() {
        Behandlingsutfall.INNVILGELSE.toDb() shouldBe "INNVILGELSE"
        Behandlingsutfall.AVSLAG.toDb() shouldBe "AVSLAG"
        Behandlingsutfall.STANS.toDb() shouldBe "STANS"
    }

    @Test
    fun `mapper til domene type`() {
        BehandlingsutfallDb.INNVILGELSE.toDomain() shouldBe Behandlingsutfall.INNVILGELSE
        BehandlingsutfallDb.AVSLAG.toDomain() shouldBe Behandlingsutfall.AVSLAG
        BehandlingsutfallDb.STANS.toDomain() shouldBe Behandlingsutfall.STANS
    }

    @Test
    fun `string til db type`() {
        "INNVILGELSE".toBehandlingsutfallDb() shouldBe BehandlingsutfallDb.INNVILGELSE
        "AVSLAG".toBehandlingsutfallDb() shouldBe BehandlingsutfallDb.AVSLAG
        "STANS".toBehandlingsutfallDb() shouldBe BehandlingsutfallDb.STANS
    }
}
