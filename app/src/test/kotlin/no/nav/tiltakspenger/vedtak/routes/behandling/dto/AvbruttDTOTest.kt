package no.nav.tiltakspenger.vedtak.routes.behandling.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Avbrutt
import org.junit.jupiter.api.Test

class AvbruttDTOTest {

    @Test
    fun `går fra domain til dto`() {
        Avbrutt(
            tidspunkt = førsteNovember24,
            saksbehandler = "Sak S. Behandler",
            begrunnelse = "s",
        ).toDTO() shouldBe AvbruttDTO(
            avbruttAv = "Sak S. Behandler",
            avbruttTidspunkt = "2024-10-01T12:30",
            begrunnelse = "s",
        )
    }
}
