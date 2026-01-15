package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

data class BarnetilleggDTO(
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

data class BarnetilleggPeriodeDTO(
    val antallBarn: Int,
    val periode: PeriodeDTO,
)

fun Barnetillegg.toBarnetilleggDTO(): BarnetilleggDTO = BarnetilleggDTO(
    perioder = periodisering.perioderMedVerdi.map {
        BarnetilleggPeriodeDTO(
            antallBarn = it.verdi.value,
            periode = it.periode.toDTO(),
        )
    },
    begrunnelse = begrunnelse?.verdi,
)

fun List<BarnetilleggPeriodeDTO>.tilPeriodisering(): Periodisering<AntallBarn> {
    // Vi ønsker ikke fylle hull med 0 på dette tidspunktet. Det gjøres av domenet siden man skal bruke innvilgelsesperiode på behandlingen dersom den er satt.
    return this.map { Pair(it.periode.toDomain(), AntallBarn(it.antallBarn)) }.tilPeriodisering()
}
