package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando

data class StartRevurderingDTO(
    val revurderingType: StartRevurderingTypeDTO,
    val rammevedtakIdSomOmgjøres: String? = null,
    val rammevedtakIdSomForlenges: String? = null,
) {
    fun tilKommando(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): StartRevurderingKommando {
        return when (revurderingType) {
            StartRevurderingTypeDTO.REVURDERING_INNVILGELSE -> StartRevurderingKommando.Innvilgelse(
                sakId = sakId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
                klagebehandlingId = null,
            )

            StartRevurderingTypeDTO.STANS -> StartRevurderingKommando.Stans(
                sakId = sakId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
            )

            StartRevurderingTypeDTO.OMGJØRING -> StartRevurderingKommando.Omgjøring(
                sakId = sakId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
                vedtakIdSomOmgjøres = rammevedtakIdSomOmgjøres?.let { VedtakId.fromString(it) },
                klagebehandlingId = null,
            )

            StartRevurderingTypeDTO.FORLENGELSE -> StartRevurderingKommando.Forlengelse(
                sakId = sakId,
                correlationId = correlationId,
                saksbehandler = saksbehandler,
                vedtakIdSomForlenges = VedtakId.fromString(
                    requireNotNull(rammevedtakIdSomForlenges) { "rammevedtakIdSomForlenges må være satt for å starte en forlengelse." },
                ),
            )
        }
    }
}

enum class StartRevurderingTypeDTO {
    REVURDERING_INNVILGELSE,
    STANS,
    OMGJØRING,
    FORLENGELSE,
}
