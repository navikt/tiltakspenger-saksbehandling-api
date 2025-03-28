package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.left
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attesteringsstatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MeldekortBehandlingTest {

    @Test
    fun `kan ikke underkjenne en MeldekortUnderBehandling`() {
        val meldekortBehandling = ObjectMother.meldekortUnderBehandling()

        meldekortBehandling.underkjenn(
            begrunnelse = NonBlankString.create("skal ikke kunne underkjenne"),
            beslutter = ObjectMother.saksbehandler(),
            clock = ObjectMother.clock,
        ) shouldBe KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErIkkeKlarTilBeslutning.left()
    }

    @Test
    fun `underkjenner en MeldekortBehandlet`() {
        val meldekortBehandlet = ObjectMother.meldekortBehandlet(
            status = MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
            iverksattTidspunkt = null,
            beslutter = null,
            opprettet = LocalDateTime.now(fixedClock),
        )

        val actual = meldekortBehandlet.underkjenn(
            begrunnelse = NonBlankString.create("skal ikke kunne underkjenne"),
            beslutter = ObjectMother.saksbehandler(),
            clock = fixedClock,
        )

        val expetcedAttestering = Attestering(
            // ignorert
            id = AttesteringId.random(),
            status = Attesteringsstatus.SENDT_TILBAKE,
            begrunnelse = NonBlankString.create("skal ikke kunne underkjenne"),
            beslutter = ObjectMother.saksbehandler().navIdent,
            tidspunkt = LocalDateTime.now(fixedClock),
        )

        actual.getOrFail().let {
            it.shouldBeInstanceOf<MeldekortBehandling.MeldekortUnderBehandling>()
            it.attesteringer.size shouldBe 1
            it.attesteringer.first().shouldBeEqualToIgnoringFields(expetcedAttestering, Attestering::id)
        }
    }
}
