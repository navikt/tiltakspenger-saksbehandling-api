package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import arrow.core.Either
import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.barnetillegg.UgyldigBarnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Barna saksbehandler har valgt å gi barnetillegg for i en forlengelse.
 * Tom liste betyr at forlengelsen ikke har barnetillegg.
 */
data class ValgtBarnetillegg(
    val barn: List<BarnMedBarnetillegg>,
    val begrunnelse: Begrunnelse?,
) {
    fun tilBarnetillegg(forlengelsesperiode: Periode): Either<UgyldigBarnetillegg, BarnetilleggV2> =
        BarnetilleggV2.create(
            barn = barn,
            innvilgelsesperioder = nonEmptyListOf(forlengelsesperiode),
            begrunnelse = begrunnelse,
        )
}
