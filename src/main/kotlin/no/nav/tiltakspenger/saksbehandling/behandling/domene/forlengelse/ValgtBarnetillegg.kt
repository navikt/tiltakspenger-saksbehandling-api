package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import arrow.core.Either
import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.barnetillegg.UgyldigBarnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Barnetillegget saksbehandler har valgt, før det er validert mot innvilgelsesperiodene.
 *
 * @param barn Barna det skal gis barnetillegg for.
 * Tom liste betyr ingen barnetillegg.
 */
data class ValgtBarnetillegg(
    val barn: List<BarnMedBarnetillegg>,
    val begrunnelse: Begrunnelse?,
) {
    fun tilBarnetillegg(innvilgelsesperioder: NonEmptyList<Periode>): Either<UgyldigBarnetillegg, BarnetilleggV2> =
        BarnetilleggV2.create(
            barn = barn,
            innvilgelsesperioder = innvilgelsesperioder,
            begrunnelse = begrunnelse,
        )
}
