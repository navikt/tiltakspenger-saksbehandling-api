package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev

data class ForhåndsvisVedtaksbrevForSøknadsbehandlingAvslagKommando(
    override val sakId: SakId,
    override val behandlingId: RammebehandlingId,
    override val correlationId: CorrelationId,
    override val saksbehandler: Saksbehandler,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
) : ForhåndsvisVedtaksbrevForSøknadsbehandlingKommando
