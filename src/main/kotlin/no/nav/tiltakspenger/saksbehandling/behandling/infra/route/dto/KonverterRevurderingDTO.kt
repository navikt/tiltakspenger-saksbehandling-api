package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter.KonverterRevurderingKommando

data class KonverterRevurderingDTO(
    val revurderingType: StartRevurderingTypeDTO,
    val rammevedtakIdSomForlenges: String? = null,
) {
    fun tilKommando(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
    ): KonverterRevurderingKommando = when (revurderingType) {
        StartRevurderingTypeDTO.STANS -> KonverterRevurderingKommando.TilStans(sakId, behandlingId, saksbehandler)

        StartRevurderingTypeDTO.REVURDERING_INNVILGELSE -> KonverterRevurderingKommando.TilInnvilgelse(sakId, behandlingId, saksbehandler)

        StartRevurderingTypeDTO.OMGJØRING -> KonverterRevurderingKommando.TilOmgjøring(sakId, behandlingId, saksbehandler)

        StartRevurderingTypeDTO.FORLENGELSE -> KonverterRevurderingKommando.TilForlengelse(
            sakId = sakId,
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
            vedtakIdSomForlenges = VedtakId.fromString(
                requireNotNull(rammevedtakIdSomForlenges) { "rammevedtakIdSomForlenges må være satt for å konvertere til forlengelse." },
            ),
        )
    }
}
