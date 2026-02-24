package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

class EndretTiltaksdeltakerBehandlingService(
    private val startRevurderingService: StartRevurderingService,
    private val leggTilbakeBehandlingService: LeggTilbakeRammebehandlingService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun opprettBehandling(
        harApneBehandlinger: Boolean,
        endringer: List<TiltaksdeltakerEndring>,
        nyesteVedtak: Rammevedtak?,
        sakId: SakId,
        deltakerId: String,
    ) {
        if (harApneBehandlinger || nyesteVedtak == null) {
            log.info { "Oppretter ikke revurdering hvis det finnes åpne behandlinger eller vedtak mangler, tiltaksdeltakelse $deltakerId, sakId $sakId" }
            return
        }

        val vedtakInneholderFlereDeltakelser = vedtakInneholderFlereDeltakelser(nyesteVedtak)
        if (skalOppretteRevurderingStans(vedtakInneholderFlereDeltakelser, endringer)) {
            log.info { "Skal opprette revurdering stans for tiltaksdeltakelse $deltakerId, sakId $sakId" }
            startRevurdering(sakId, StartRevurderingType.STANS, null)
        } else if (skalOppretteRevurderingForlengelse(endringer)) {
            log.info { "Skal opprette revurdering forlengelse/innvilgelse for tiltaksdeltakelse $deltakerId, sakId $sakId" }
            startRevurdering(sakId, StartRevurderingType.INNVILGELSE, null)
        } else if (skalOppretteRevurderingOmgjoring(vedtakInneholderFlereDeltakelser, endringer)) {
            log.info { "Skal opprette revurdering omgjøring for tiltaksdeltakelse $deltakerId, sakId $sakId" }
            startRevurdering(sakId, StartRevurderingType.OMGJØRING, nyesteVedtak.id)
        }
    }

    private fun vedtakInneholderFlereDeltakelser(vedtak: Rammevedtak): Boolean {
        val tiltaksdeltakelserFraVedtak = vedtak.innvilgelsesperioder?.valgteTiltaksdeltagelser?.verdier?.distinct()
        return tiltaksdeltakelserFraVedtak != null && tiltaksdeltakelserFraVedtak.size > 1
    }

    private fun skalOppretteRevurderingStans(
        vedtakInneholderFlereDeltakelser: Boolean,
        endringer: List<TiltaksdeltakerEndring>,
    ): Boolean {
        if (vedtakInneholderFlereDeltakelser) {
            return false
        }
        return endringer.any { it == TiltaksdeltakerEndring.AVBRUTT_DELTAKELSE }
    }

    private fun skalOppretteRevurderingForlengelse(
        endringer: List<TiltaksdeltakerEndring>,
    ): Boolean {
        return endringer.any { it == TiltaksdeltakerEndring.FORLENGELSE }
    }

    private fun skalOppretteRevurderingOmgjoring(
        vedtakInneholderFlereDeltakelser: Boolean,
        endringer: List<TiltaksdeltakerEndring>,
    ): Boolean {
        if (vedtakInneholderFlereDeltakelser && endringer.any { it == TiltaksdeltakerEndring.AVBRUTT_DELTAKELSE }) {
            return true
        }
        return endringer.any { it == TiltaksdeltakerEndring.ENDRET_STARTDATO || it == TiltaksdeltakerEndring.ENDRET_SLUTTDATO || it == TiltaksdeltakerEndring.ENDRET_DELTAKELSESMENGDE }
    }

    private suspend fun startRevurdering(
        sakId: SakId,
        revurderingType: StartRevurderingType,
        vedtakIdSomOmgjores: VedtakId?,
    ) {
        val kommando = StartRevurderingKommando(
            sakId = sakId,
            correlationId = CorrelationId.generate(),
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            revurderingType = revurderingType,
            vedtakIdSomOmgjøres = vedtakIdSomOmgjores,
            klagebehandlingId = null,
        )
        val (_, revurdering) = startRevurderingService.startRevurdering(kommando)

        log.info { "Opprettet revurdering med id ${revurdering.id}" }

        leggTilbakeBehandlingService.leggTilbakeBehandling(
            sakId = sakId,
            behandlingId = revurdering.id,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
        )

        log.info { "Fjerner automatisk saksbehandler fra revurdering med id ${revurdering.id}" }
    }
}
