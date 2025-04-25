package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MeldekortBehandlingManuellTest {
    @Test
    fun `underkjenner en MeldekortBehandlet`() {
        val meldekortBehandlet = ObjectMother.meldekortBehandletManuelt(
            status = MeldekortBehandlingStatus.UNDER_BESLUTNING,
            iverksattTidspunkt = null,
            beslutter = ObjectMother.saksbehandler().navIdent,
            opprettet = LocalDateTime.now(fixedClock),
        )

        val actual = meldekortBehandlet.underkjenn(
            begrunnelse = NonBlankString.create("skal ikke kunne underkjenne"),
            beslutter = ObjectMother.saksbehandler(),
            clock = fixedClock,
        )

        val expectedAttestering = Attestering(
            // ignorert
            id = AttesteringId.random(),
            status = Attesteringsstatus.SENDT_TILBAKE,
            begrunnelse = NonBlankString.create("skal ikke kunne underkjenne"),
            beslutter = ObjectMother.saksbehandler().navIdent,
            tidspunkt = LocalDateTime.now(fixedClock),
        )

        actual.getOrFail().let {
            it.shouldBeInstanceOf<MeldekortUnderBehandling>()
            it.attesteringer.size shouldBe 1
            it.attesteringer.first().shouldBeEqualToIgnoringFields(expectedAttestering, Attestering::id)
        }
    }
}
