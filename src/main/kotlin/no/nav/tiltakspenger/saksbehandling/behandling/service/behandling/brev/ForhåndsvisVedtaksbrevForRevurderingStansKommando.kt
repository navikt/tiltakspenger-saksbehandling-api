package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import java.time.LocalDate

/**
 * Merk at man alltid stanser ut hele den gjeldende innvilgelsesperioden.
 * @param stansFraOgMed kan være null, som betyr at stansen skjer fra og med første dag som gir rett.
 */
data class ForhåndsvisVedtaksbrevForRevurderingStansKommando(
    override val sakId: SakId,
    override val behandlingId: BehandlingId,
    override val correlationId: CorrelationId,
    override val saksbehandler: Saksbehandler,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val valgteHjemler: NonEmptySet<ValgtHjemmelForStans>,
    val stansFraOgMed: LocalDate?,
) : ForhåndsvisVedtaksbrevForRevurderingKommando {

    fun utledStansperiode(førsteDagSomGirRett: LocalDate, sisteDagSomGirRett: LocalDate): Periode {
        if (stansFraOgMed != null) {
            require(stansFraOgMed >= førsteDagSomGirRett) {
                "Stans fra og med $stansFraOgMed kan ikke være før første dag som gir rett $førsteDagSomGirRett"
            }
            require(stansFraOgMed <= sisteDagSomGirRett) {
                "Stans fra og med $stansFraOgMed kan ikke være etter siste dag som gir rett $sisteDagSomGirRett"
            }
        }
        return Periode(
            fraOgMed = stansFraOgMed ?: førsteDagSomGirRett,
            tilOgMed = sisteDagSomGirRett,
        )
    }
}
