package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TiltakshistorikkForBehandlingTest {

    private fun Rammebehandling.innslag(
        innhentet: LocalDateTime,
        tiltaksdeltakerId: TiltaksdeltakerId = saksopplysninger.tiltaksdeltakelser.single().internDeltakelseId,
        behandlingId: RammebehandlingId? = null,
    ): Tiltakshistorikkinnslag {
        val deltakelse = saksopplysninger.tiltaksdeltakelser.single()
        return Tiltakshistorikkinnslag(
            id = TiltakshistorikkId.random(),
            sakId = sakId,
            tiltaksdeltakerId = tiltaksdeltakerId,
            eksternDeltakerId = deltakelse.eksternDeltakelseId,
            innhentet = innhentet,
            verdi = deltakelse.tilLibsDeltakelse(),
            endring = null,
            behandlingId = behandlingId,
            oppgaveId = null,
        )
    }

    @Test
    fun `tar med innslag for behandlingens deltakelser fra den ble opprettet, og varsler om innslag etter at den sist ble endret`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
        val førOpprettet = behandling.innslag(behandling.opprettet.minusSeconds(1))
        val vedOpprettet = behandling.innslag(behandling.opprettet)
        val annenDeltaker = behandling.innslag(behandling.sistEndret.plusSeconds(1), tiltaksdeltakerId = TiltaksdeltakerId.random())
        val etterSistEndret = behandling.innslag(behandling.sistEndret.plusSeconds(2))

        val historikk = listOf(etterSistEndret, annenDeltaker, vedOpprettet, førOpprettet).forBehandling(behandling)

        historikk shouldBe TiltakshistorikkForBehandling(
            innslag = listOf(vedOpprettet, etterSistEndret),
            utløsendeInnslag = null,
            harEndringEtterSistEndret = true,
        )
    }

    @Test
    fun `innslaget som utløste behandlingen tas med og markeres, men gir ikke varsel`() {
        val behandling = ObjectMother.nyOpprettetSøknadsbehandling()
        val utløsende = behandling.innslag(behandling.sistEndret.plusSeconds(1), behandlingId = behandling.id)

        listOf(utløsende).forBehandling(behandling) shouldBe TiltakshistorikkForBehandling(
            innslag = listOf(utløsende),
            utløsendeInnslag = utløsende,
            harEndringEtterSistEndret = false,
        )
    }

    @Test
    fun `varsler om innslag etter at behandlingen ble sendt til beslutning`() {
        val behandling = ObjectMother.nySøknadsbehandlingKlarTilBeslutning()
        val etterSistEndret = behandling.innslag(behandling.sistEndret.plusSeconds(1))

        listOf(etterSistEndret).forBehandling(behandling).harEndringEtterSistEndret shouldBe true
    }

    @Test
    fun `tar ikke med innslag etter iverksettelse, og varsler ikke for iverksatte behandlinger`() {
        val behandling = ObjectMother.nyVedtattSøknadsbehandling()
        val iverksatt = behandling.iverksattTidspunkt.shouldNotBeNull()
        val vedIverksettelse = behandling.innslag(iverksatt)
        val etterIverksettelse = behandling.innslag(iverksatt.plusSeconds(1))

        listOf(vedIverksettelse, etterIverksettelse).forBehandling(behandling) shouldBe TiltakshistorikkForBehandling(
            innslag = listOf(vedIverksettelse),
            utløsendeInnslag = null,
            harEndringEtterSistEndret = false,
        )
    }

    @Test
    fun `tar ikke med innslag etter at behandlingen ble avbrutt`() {
        val behandling = ObjectMother.nyAvbruttSøknadsbehandling()
        val avbrutt = behandling.avbrutt.shouldNotBeNull().tidspunkt
        val etterAvbrudd = behandling.innslag(avbrutt.plusSeconds(1))

        listOf(etterAvbrudd).forBehandling(behandling).innslag shouldBe emptyList()
    }
}
