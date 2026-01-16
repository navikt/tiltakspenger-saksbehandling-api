package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import org.junit.jupiter.api.Test

class AvbruttDTOTest {

    @Test
    fun `går fra domain til dto`() {
        Avbrutt(
            tidspunkt = førsteNovember24,
            saksbehandler = "Sak S. Behandler",
            begrunnelse = "s".toNonBlankString(),
        ).toAvbruttDTO() shouldBe AvbruttDTO(
            avbruttAv = "Sak S. Behandler",
            avbruttTidspunkt = "2024-10-01T12:30",
            begrunnelse = "s",
        )
    }
}
