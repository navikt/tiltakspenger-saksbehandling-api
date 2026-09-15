package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet

/**
 * Utgående barnetillegg.
 * Begrunnelsen er saksbehandlers fritekst og kan sladdes.
 * Barnetillegget som kommer inn fra frontenden er [OppdaterBarnetilleggDTO], som har rå verdier.
 */
data class BarnetilleggDTO(
    val perioder: List<BarnetilleggPeriodeDTO>,
    val begrunnelse: SladdbarVerdi<String?>,
)

data class BarnetilleggPeriodeDTO(
    val antallBarn: Int,
    val periode: PeriodeDTO,
)

fun Barnetillegg.toBarnetilleggDTO(): BarnetilleggDTO = BarnetilleggDTO(
    perioder = periodisering.tilBarnetilleggPerioderDTO(),
    begrunnelse = begrunnelse?.verdi.ikkeSladdet(),
)

fun List<BarnetilleggPeriodeDTO>.tilPeriodisering(): Periodisering<AntallBarn> {
    // Vi ønsker ikke fylle hull med 0 på dette tidspunktet.
    // Det gjøres av domenet siden man skal bruke innvilgelsesperiode på behandlingen dersom den er satt.
    return this.map { Pair(it.periode.toDomain(), AntallBarn(it.antallBarn)) }.tilPeriodisering()
}

fun Periodisering<AntallBarn>.tilBarnetilleggPerioderDTO(): List<BarnetilleggPeriodeDTO> {
    return this.perioderMedVerdi.map {
        BarnetilleggPeriodeDTO(
            antallBarn = it.verdi.value,
            periode = it.periode.toDTO(),
        )
    }
}
