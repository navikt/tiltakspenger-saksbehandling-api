package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkForBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltakDeltakerstatusDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime

class TiltakshistorikkForBehandlingDTOTest {

    @ParameterizedTest
    @CsvSource(
        "2025-01-01T23:59:59, VenterPåOppstart",
        "2025-01-02T00:00:00, Deltar",
    )
    fun `Arena GJENNOMFORES tolkes slik statusen var da innslaget ble innhentet`(
        innhentet: LocalDateTime,
        forventetStatus: TiltakDeltakerstatusDto,
    ) {
        val verdi = testdeltakelse(
            id = "TA1",
            kildestatus = Arenastatus.Kjent(Arenastatus.Type.GJENNOMFORES),
            fraOgMed = 2.januar(2025),
        ) as Tiltaksdeltakelse.GirRett
        val innslag = Tiltakshistorikkinnslag(
            id = TiltakshistorikkId.random(),
            sakId = SakId.random(),
            tiltaksdeltakerId = TiltaksdeltakerId.random(),
            eksternDeltakerId = "TA1",
            innhentet = innhentet,
            verdi = verdi,
            endring = null,
            behandlingId = null,
            oppgaveId = null,
        )

        val dto = TiltakshistorikkForBehandling(
            innslag = listOf(innslag),
            utløsendeInnslag = null,
            harEndringEtterSistEndret = false,
        ).tilDTO().innslag.single()

        dto.deltakelseStatus shouldBe forventetStatus
        dto.kildestatus shouldBe "GJENNOMFORES"
    }
}
