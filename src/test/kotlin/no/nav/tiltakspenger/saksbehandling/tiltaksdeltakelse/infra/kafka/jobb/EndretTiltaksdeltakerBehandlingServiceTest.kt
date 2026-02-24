package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class EndretTiltaksdeltakerBehandlingServiceTest {
    private val startRevurderingService = mockk<StartRevurderingService>()
    private val leggTilbakeBehandlingService = mockk<LeggTilbakeRammebehandlingService>()
    private val endretTiltaksdeltakerBehandlingService = EndretTiltaksdeltakerBehandlingService(
        startRevurderingService = startRevurderingService,
        leggTilbakeBehandlingService = leggTilbakeBehandlingService,
    )

    @BeforeEach
    fun clearMockData() {
        clearMocks(startRevurderingService, leggTilbakeBehandlingService)
    }

    @Test
    fun `opprettBehandling - har åpen behandling - oppretter ikke revurdering`() = runBlocking {
        val id = UUID.randomUUID().toString()
        val fnr = Fnr.random()
        val (sak, _) = ObjectMother.sakMedOpprettetBehandling(fnr = fnr)
        val endringer = listOf(TiltaksdeltakerEndring.FORLENGELSE)

        endretTiltaksdeltakerBehandlingService.opprettBehandling(
            harApneBehandlinger = true,
            endringer = endringer,
            nyesteVedtak = null,
            sakId = sak.id,
            deltakerId = id,
        )

        coVerify(exactly = 0) { startRevurderingService.startRevurdering(any()) }
        coVerify(exactly = 0) { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) }
    }

    @Test
    fun `opprettBehandling - iverksatt behandling, forlengelse - oppretter revurdering innvilgelse`() = runBlocking {
        val id = UUID.randomUUID().toString()
        val fnr = Fnr.random()
        val (sak, vedtak, _) = ObjectMother.nySakMedVedtak(fnr = fnr)
        val endringer = listOf(TiltaksdeltakerEndring.FORLENGELSE)
        val revurdering = ObjectMother.nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = fnr,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
        )
        coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
            sak,
            revurdering,
        )
        coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
            sak,
            revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
        )

        endretTiltaksdeltakerBehandlingService.opprettBehandling(
            harApneBehandlinger = false,
            endringer = endringer,
            nyesteVedtak = vedtak as Rammevedtak,
            sakId = sak.id,
            deltakerId = id,
        )

        coVerify(exactly = 1) { startRevurderingService.startRevurdering(match { it.sakId == sak.id && it.revurderingType == StartRevurderingType.INNVILGELSE }) }
        coVerify(exactly = 1) {
            leggTilbakeBehandlingService.leggTilbakeBehandling(
                sak.id,
                any(),
                AUTOMATISK_SAKSBEHANDLER,
            )
        }
    }

    @Test
    fun `opprettBehandling - iverksatt behandling, avbrutt - oppretter revurdering stans`() = runBlocking {
        val id = UUID.randomUUID().toString()
        val fnr = Fnr.random()
        val (sak, vedtak, _) = ObjectMother.nySakMedVedtak(fnr = fnr)
        val endringer = listOf(TiltaksdeltakerEndring.AVBRUTT_DELTAKELSE)
        val revurdering = ObjectMother.nyOpprettetRevurderingStans(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = fnr,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
        )
        coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
            sak,
            revurdering,
        )
        coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
            sak,
            revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
        )

        endretTiltaksdeltakerBehandlingService.opprettBehandling(
            harApneBehandlinger = false,
            endringer = endringer,
            nyesteVedtak = vedtak as Rammevedtak,
            sakId = sak.id,
            deltakerId = id,
        )

        coVerify(exactly = 1) {
            startRevurderingService.startRevurdering(
                match {
                    it.sakId == sak.id && it.revurderingType == StartRevurderingType.STANS && it.vedtakIdSomOmgjøres == null
                },
            )
        }
        coVerify(exactly = 1) {
            leggTilbakeBehandlingService.leggTilbakeBehandling(
                sak.id,
                any(),
                AUTOMATISK_SAKSBEHANDLER,
            )
        }
    }

    @Test
    fun `opprettBehandling - iverksatt behandling, endret startdato - oppretter revurdering omgjøring`() = runBlocking {
        val id = UUID.randomUUID().toString()
        val fnr = Fnr.random()
        val (sak, vedtak, _) = ObjectMother.nySakMedVedtak(fnr = fnr)
        val endringer = listOf(TiltaksdeltakerEndring.ENDRET_STARTDATO)
        val revurdering = ObjectMother.nyOpprettetRevurderingOmgjøring(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = fnr,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
        )
        coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
            sak,
            revurdering,
        )
        coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
            sak,
            revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
        )

        endretTiltaksdeltakerBehandlingService.opprettBehandling(
            harApneBehandlinger = false,
            endringer = endringer,
            nyesteVedtak = vedtak as Rammevedtak,
            sakId = sak.id,
            deltakerId = id,
        )

        coVerify(exactly = 1) {
            startRevurderingService.startRevurdering(
                match {
                    it.sakId == sak.id && it.revurderingType == StartRevurderingType.OMGJØRING && it.vedtakIdSomOmgjøres == vedtak.id
                },
            )
        }
        coVerify(exactly = 1) {
            leggTilbakeBehandlingService.leggTilbakeBehandling(
                sak.id,
                any(),
                AUTOMATISK_SAKSBEHANDLER,
            )
        }
    }

    @Test
    fun `opprettBehandling - iverksatt behandling, endret sluttdato - oppretter revurdering omgjøring`() = runBlocking {
        val id = UUID.randomUUID().toString()
        val fnr = Fnr.random()
        val (sak, vedtak, _) = ObjectMother.nySakMedVedtak(fnr = fnr)
        val endringer = listOf(TiltaksdeltakerEndring.ENDRET_SLUTTDATO)
        val revurdering = ObjectMother.nyOpprettetRevurderingOmgjøring(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = fnr,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
        )
        coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
            sak,
            revurdering,
        )
        coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
            sak,
            revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
        )

        endretTiltaksdeltakerBehandlingService.opprettBehandling(
            harApneBehandlinger = false,
            endringer = endringer,
            nyesteVedtak = vedtak as Rammevedtak,
            sakId = sak.id,
            deltakerId = id,
        )

        coVerify(exactly = 1) {
            startRevurderingService.startRevurdering(
                match {
                    it.sakId == sak.id && it.revurderingType == StartRevurderingType.OMGJØRING && it.vedtakIdSomOmgjøres == vedtak.id
                },
            )
        }
        coVerify(exactly = 1) {
            leggTilbakeBehandlingService.leggTilbakeBehandling(
                sak.id,
                any(),
                AUTOMATISK_SAKSBEHANDLER,
            )
        }
    }

    @Test
    fun `opprettBehandling - iverksatt behandling, endret deltakelsesmengde - oppretter revurdering omgjøring`() = runBlocking {
        val id = UUID.randomUUID().toString()
        val fnr = Fnr.random()
        val (sak, vedtak, _) = ObjectMother.nySakMedVedtak(fnr = fnr)
        val endringer = listOf(TiltaksdeltakerEndring.ENDRET_DELTAKELSESMENGDE)
        val revurdering = ObjectMother.nyOpprettetRevurderingOmgjøring(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = fnr,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
        )
        coEvery { startRevurderingService.startRevurdering(any()) } returns Pair(
            sak,
            revurdering,
        )
        coEvery { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) } returns Pair(
            sak,
            revurdering.copy(saksbehandler = null, status = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING),
        )

        endretTiltaksdeltakerBehandlingService.opprettBehandling(
            harApneBehandlinger = false,
            endringer = endringer,
            nyesteVedtak = vedtak as Rammevedtak,
            sakId = sak.id,
            deltakerId = id,
        )

        coVerify(exactly = 1) {
            startRevurderingService.startRevurdering(
                match {
                    it.sakId == sak.id && it.revurderingType == StartRevurderingType.OMGJØRING && it.vedtakIdSomOmgjøres == vedtak.id
                },
            )
        }
        coVerify(exactly = 1) {
            leggTilbakeBehandlingService.leggTilbakeBehandling(
                sak.id,
                any(),
                AUTOMATISK_SAKSBEHANDLER,
            )
        }
    }

    @Test
    fun `opprettBehandling - iverksatt behandling, endret status - oppretter ikke revurdering`() = runBlocking {
        val id = UUID.randomUUID().toString()
        val fnr = Fnr.random()
        val (sak, vedtak, _) = ObjectMother.nySakMedVedtak(fnr = fnr)
        val endringer = listOf(TiltaksdeltakerEndring.ENDRET_STATUS)

        endretTiltaksdeltakerBehandlingService.opprettBehandling(
            harApneBehandlinger = false,
            endringer = endringer,
            nyesteVedtak = vedtak as Rammevedtak,
            sakId = sak.id,
            deltakerId = id,
        )

        coVerify(exactly = 0) { startRevurderingService.startRevurdering(any()) }
        coVerify(exactly = 0) { leggTilbakeBehandlingService.leggTilbakeBehandling(any(), any(), any()) }
    }
}
