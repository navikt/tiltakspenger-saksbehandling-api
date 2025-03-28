package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import org.junit.jupiter.api.Test

class AvbruttDTOTest {

    @Test
    fun `går fra domain til dto`() {
        no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt(
            tidspunkt = førsteNovember24,
            saksbehandler = "Sak S. Behandler",
            begrunnelse = "s",
        ).toAvbruttDTO() shouldBe AvbruttDTO(
            avbruttAv = "Sak S. Behandler",
            avbruttTidspunkt = "2024-10-01T12:30",
            begrunnelse = "s",
        )
    }
}
