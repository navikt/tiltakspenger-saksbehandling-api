package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arrangør
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Deltakelsesomfang
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.EksternDeltakelseId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.GjennomføringId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometårsak
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkForBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltakDeltakerstatusDto
import org.junit.jupiter.api.Test
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
        val innslag = innslag(verdi = verdi, innhentet = innhentet)

        val dto = tilDTO(innslag)

        dto.deltakelseStatus shouldBe forventetStatus
        dto.kildestatus shouldBe "GJENNOMFORES"
        dto.verdi.kildestatus shouldBe TiltakshistorikkVerdiDTO.KildestatusDTO(
            kilde = "Arena",
            kodeIKontrakten = "GJENNOMFORES",
            årsak = null,
            opprettet = null,
        )
    }

    @Test
    fun `inkluderer alle felter fra lagret verdi i DTO-en`() {
        val statusOpprettet = LocalDateTime.of(2025, 2, 10, 8, 30, 15)
        val verdi = Tiltaksdeltakelse.GirRett.MedPeriode(
            id = EksternDeltakelseId("komet-deltaker-1"),
            gjennomføringId = GjennomføringId("gjennomføring-1"),
            gjennomføringsnavn = null,
            tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
            tiltakstypenavn = "Arbeidsmarkedsopplæring i gruppe",
            tiltakskodeFraKilden = "GRUPPE_AMO_KILDE",
            tittel = null,
            arrangør = Arrangør(hovedenhet = null, underenhet = null),
            periode = Periode(2.januar(2025), 31.mars(2025)),
            kildestatus = Kometstatus.Kjent(
                type = Kometstatus.Type.HAR_SLUTTET,
                årsak = Kometårsak.Kjent(Kometstatus.Årsak.FATT_JOBB),
                opprettet = statusOpprettet,
            ),
            omfang = Deltakelsesomfang(
                deltakelsesprosent = 80f,
                dagerPerUke = 4f,
                deltidsprosentPåGjennomføring = 60f,
            ),
        )

        val dto = tilDTO(innslag(verdi = verdi))

        dto.verdi shouldBe TiltakshistorikkVerdiDTO(
            eksternDeltakelseId = "komet-deltaker-1",
            gjennomføringId = "gjennomføring-1",
            tiltakstype = "GRUPPE_AMO",
            tiltakstypenavn = "Arbeidsmarkedsopplæring i gruppe",
            tiltakskodeFraKilden = "GRUPPE_AMO_KILDE",
            fraOgMed = 2.januar(2025),
            tilOgMed = 31.mars(2025),
            kildestatus = TiltakshistorikkVerdiDTO.KildestatusDTO(
                kilde = "Komet",
                kodeIKontrakten = "HAR_SLUTTET",
                årsak = "FATT_JOBB",
                opprettet = statusOpprettet,
            ),
            deltakelsesprosent = 80f,
            dagerPerUke = 4f,
            deltidsprosentPåGjennomføring = 60f,
        )
        dto.verdi.kildestatus.årsak shouldBe "FATT_JOBB"
        dto.verdi.kildestatus.opprettet shouldBe statusOpprettet
    }

    @Test
    fun `avsluttet som forventet tas ikke med som relevant endring i DTO-en`() {
        val verdi = testdeltakelse(id = "komet-1") as Tiltaksdeltakelse.GirRett
        val avsluttetSomForventet = tilDTO(innslag(verdi = verdi, endring = TiltaksdeltakerEndring.AvsluttetSomForventet))
        val avbrutt = tilDTO(innslag(verdi = verdi, endring = TiltaksdeltakerEndring.AvbruttDeltakelse))

        avsluttetSomForventet.endringer shouldBe emptyList()
        avbrutt.endringer shouldBe listOf(TiltaksdeltakerEndringDTO.AvbruttDeltakelse)
    }

    private fun innslag(
        verdi: Tiltaksdeltakelse.GirRett,
        innhentet: LocalDateTime = LocalDateTime.of(2025, 2, 10, 9, 0),
        endring: TiltaksdeltakerEndring? = null,
    ) = Tiltakshistorikkinnslag(
        id = TiltakshistorikkId.random(),
        sakId = SakId.random(),
        tiltaksdeltakerId = TiltaksdeltakerId.random(),
        eksternDeltakerId = verdi.id.verdi,
        innhentet = innhentet,
        verdi = verdi,
        endring = endring,
        behandlingId = null,
        oppgaveId = null,
    )

    private fun tilDTO(innslag: Tiltakshistorikkinnslag): TiltakshistorikkinnslagDTO = TiltakshistorikkForBehandling(
        innslag = listOf(innslag),
        utløsendeInnslag = null,
        harEndringEtterSistEndret = false,
    ).tilDTO().innslag.single()
}
