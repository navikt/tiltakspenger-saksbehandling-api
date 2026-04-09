package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import org.junit.jupiter.api.Test

class AvbruttDTOTest {

    @Test
    fun `går fra domain til dto`() {
        Avbrutt(
            tidspunkt = 1.november(2024).atStartOfDay(),
            saksbehandler = "Sak S. Behandler",
            begrunnelse = "s".toNonBlankString(),
        ).toAvbruttDTO() shouldBe AvbruttDTO(
            avbruttAv = "Sak S. Behandler",
            avbruttTidspunkt = "2024-11-01T00:00",
            begrunnelse = "s",
        )
    }
}
