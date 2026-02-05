package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType

data class StartRevurderingDTO(
    val revurderingType: StartRevurderingTypeDTO,
    val rammevedtakIdSomOmgjøres: String? = null,
    val nyOmgjøring: Boolean = false,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): StartRevurderingKommando {
        return StartRevurderingKommando(
            sakId = sakId,
            correlationId = correlationId,
            saksbehandler = saksbehandler,
            revurderingType = revurderingType.tilKommando(),
            vedtakIdSomOmgjøres = rammevedtakIdSomOmgjøres?.let { VedtakId.fromString(it) },
            klagebehandlingId = null,
            nyOmgjøring = nyOmgjøring,
        )
    }
}

enum class StartRevurderingTypeDTO {
    REVURDERING_INNVILGELSE,
    STANS,
    OMGJØRING,
    ;

    fun tilKommando(): StartRevurderingType = when (this) {
        REVURDERING_INNVILGELSE -> StartRevurderingType.INNVILGELSE
        STANS -> StartRevurderingType.STANS
        OMGJØRING -> StartRevurderingType.OMGJØRING
    }
}
