package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Barnetillegget slik frontenden sender det inn.
 * Inngående DTO-er har rå verdier, siden sladding kun gjelder det vi sender ut.
 */
data class OppdaterBarnetilleggDTO(
    val perioder: List<BarnetilleggPeriodeDTO>,
    val begrunnelse: String?,
) {
    fun tilBarnetillegg(innvilgelsesperioder: NonEmptyList<Periode>): Barnetillegg =
        if (this.perioder.isNotEmpty()) {
            Barnetillegg.periodiserOgFyllUtHullMed0(
                begrunnelse = begrunnelse?.let { (Begrunnelse.create(it)) },
                perioderMedBarn = perioder
                    .map { Pair(it.periode.toDomain(), AntallBarn(it.antallBarn)) }
                    .toNonEmptyListOrThrow(),
                innvilgelsesperioder = innvilgelsesperioder,
            )
        } else {
            Barnetillegg.utenBarnetillegg(innvilgelsesperioder)
        }
}
