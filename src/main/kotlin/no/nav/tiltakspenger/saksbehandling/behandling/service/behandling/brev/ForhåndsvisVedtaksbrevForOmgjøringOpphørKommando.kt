package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør

data class ForhåndsvisVedtaksbrevForOmgjøringOpphørKommando(
    override val sakId: SakId,
    override val behandlingId: BehandlingId,
    override val correlationId: CorrelationId,
    override val saksbehandler: Saksbehandler,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val vedtaksperiode: Periode,
    val valgteHjemler: NonEmptySet<HjemmelForStansEllerOpphør>,
) : ForhåndsvisVedtaksbrevForRevurderingKommando
