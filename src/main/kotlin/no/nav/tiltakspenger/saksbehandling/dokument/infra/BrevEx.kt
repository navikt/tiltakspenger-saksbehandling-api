package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.leggSammen
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.dokument.infra.BrevRammevedtakInnvilgelseBaseDTO.SatserDTO

fun Satser.Companion.tilSatserDTO(periode: Periode): List<SatserDTO> {
    return satser.filter { it.periode.overlapperMed(periode) }.map {
        SatserDTO(
            år = it.periode.fraOgMed.year,
            ordinær = it.sats,
            barnetillegg = it.satsBarnetillegg,
        )
    }
}

/**
 * Dagsbeløpene for hver sammenhengende innvilgelsesperiode, slik brevet viser dem i én tabell per periode.
 * Brukes av alle vedtaksbrev som gir rett til tiltakspenger.
 *
 * Beløpet regnes ut for hver dag med satsen som gjelder den dagen og antall barn med barnetillegg.
 * Like nabodager slås sammen, så det blir en ny rad når satsen eller antall barn endrer seg.
 * Dager uten barnetillegg regnes med null barn.
 *
 * [Satser.sats] gir siste kjente sats for dager etter siste kjente satsår.
 * Da kan en rad krysse årsskiftet, og pdfgenrs deler den og sier fra om det ved hjelp av [Satser.Companion.sisteKjenteSatsår].
 *
 * Beløpene er per dag og ikke utbetalingen for perioden, som beregnes fra meldekortene.
 */
fun totalDagSatsForAllePeriodene(
    innvilgelsesperioder: Innvilgelsesperioder,
    barnetillegg: Periodisering<AntallBarn>?,
): List<PeriodeInnvilgelseDTO> {
    return innvilgelsesperioder.perioder.leggSammen(false).map { periode ->

        val beløpPerPeriode = periode.tilDager().map { dag ->

            val satsdag = Satser.sats(dato = dag)
            val antallBarn = barnetillegg?.hentVerdiForDag(dag)?.value ?: 0

            Periode(fraOgMed = dag, tilOgMed = dag) to DagBeløp(
                antallBarn = antallBarn,
                tiltakspenger = satsdag.sats,
                barnetillegg = satsdag.satsBarnetillegg * antallBarn,
            )
        }.tilPeriodisering()

        PeriodeInnvilgelseDTO(
            periode = BrevPeriodeDTO.fraPeriode(periode),
            beløp = beløpPerPeriode.perioderMedVerdi.map { (beløp, strekning) ->
                PeriodeBeløpInnvilgelseDTO(
                    periode = BrevPeriodeDTO.fraPeriode(strekning),
                    antallBarn = beløp.antallBarn,
                    tiltakspenger = beløp.tiltakspenger,
                    barnetilleggBeløp = beløp.barnetillegg,
                    total = beløp.tiltakspenger + beløp.barnetillegg,
                )
            },
        )
    }
}

/**
 * Beløpene for én dag.
 * Må være en data class, slik at like nabodager slås sammen av tilPeriodisering.
 */
private data class DagBeløp(
    val antallBarn: Int,
    val tiltakspenger: Int,
    val barnetillegg: Int,
)

/**
 * Året den siste kjente satsen trådte i kraft.
 * pdfgenrs bruker det til å si fra når beløp etter dette året er vist med satsene for dette året.
 */
fun Satser.Companion.sisteKjenteSatsår(): Int = satser.last().fraOgMed.year
