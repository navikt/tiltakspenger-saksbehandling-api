package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn

sealed interface StartRevurderingKommando {
    val sakId: SakId
    val correlationId: CorrelationId

    /** Null når revurderingen er automatisk opprettet. */
    val saksbehandler: Saksbehandler?

    data class Stans(
        override val sakId: SakId,
        override val correlationId: CorrelationId,
        override val saksbehandler: Saksbehandler,
    ) : StartRevurderingKommando

    data class Innvilgelse(
        override val sakId: SakId,
        override val correlationId: CorrelationId,
        override val saksbehandler: Saksbehandler,
        val klagebehandlingId: KlagebehandlingId?,
    ) : StartRevurderingKommando

    data class Omgjøring(
        override val sakId: SakId,
        override val correlationId: CorrelationId,
        override val saksbehandler: Saksbehandler?,
        val vedtakIdSomOmgjøres: VedtakId?,
        val klagebehandlingId: KlagebehandlingId?,
        val automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn? = null,
    ) : StartRevurderingKommando

    data class Forlengelse(
        override val sakId: SakId,
        override val correlationId: CorrelationId,
        override val saksbehandler: Saksbehandler,
        val vedtakIdSomForlenges: VedtakId,
    ) : StartRevurderingKommando

    /**
     * En revurdering systemet oppretter når det ikke kan utlede hvilken type revurdering [automatiskOpprettetGrunn] krever.
     * Kan bare opprettes av systemet, og har derfor aldri en saksbehandler.
     * Se [Revurdering.opprettUbestemt].
     */
    data class Ubestemt(
        override val sakId: SakId,
        override val correlationId: CorrelationId,
        val automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
    ) : StartRevurderingKommando {
        override val saksbehandler: Saksbehandler? = null
    }
}
