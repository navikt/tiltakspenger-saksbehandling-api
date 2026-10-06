package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test

/**
 * Pinner strengene frontend mottar for hver deltakerstatus.
 */
class TiltakDeltakerstatusDtoTest {

    @Test
    fun `hver status serialiseres til sitt eget navn`() {
        TiltakDeltakerstatus.entries.associateWith { serialize(it.toDto()) } shouldBe mapOf(
            TiltakDeltakerstatus.VenterPåOppstart to "\"VenterPåOppstart\"",
            TiltakDeltakerstatus.Deltar to "\"Deltar\"",
            TiltakDeltakerstatus.HarSluttet to "\"HarSluttet\"",
            TiltakDeltakerstatus.Avbrutt to "\"Avbrutt\"",
            TiltakDeltakerstatus.Fullført to "\"Fullført\"",
            TiltakDeltakerstatus.IkkeAktuell to "\"IkkeAktuell\"",
            TiltakDeltakerstatus.Feilregistrert to "\"Feilregistrert\"",
            TiltakDeltakerstatus.PåbegyntRegistrering to "\"PåbegyntRegistrering\"",
            TiltakDeltakerstatus.SøktInn to "\"SøktInn\"",
            TiltakDeltakerstatus.Venteliste to "\"Venteliste\"",
            TiltakDeltakerstatus.Vurderes to "\"Vurderes\"",
        )
    }
}
