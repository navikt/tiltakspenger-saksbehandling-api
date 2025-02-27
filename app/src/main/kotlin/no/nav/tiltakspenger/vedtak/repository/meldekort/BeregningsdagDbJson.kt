package no.nav.tiltakspenger.vedtak.repository.meldekort

import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.meldekort.domene.Beregningsdag
import java.time.LocalDate

data class BeregningsdagDbJson(
    val beløp: Int,
    val prosent: Int,
    val satsdag: SatsdagDbJson,
    val dato: LocalDate,
    // Kommentar jah: Dette er et alternativ til å skrive en databasmigrering.
    val antallBarn: Int?,
    val beløpBarnetillegg: Int?,
)

fun BeregningsdagDbJson.toBeregningsdag(): Beregningsdag {
    return Beregningsdag(
        beløp = beløp,
        prosent = prosent,
        satsdag = satsdag.toSatsdag(),
        dato = dato,
        antallBarn = antallBarn?.let { AntallBarn(it) } ?: AntallBarn.ZERO,
        beløpBarnetillegg = beløpBarnetillegg ?: 0,
    )
}

fun Beregningsdag.toDbJson(): BeregningsdagDbJson {
    return BeregningsdagDbJson(
        beløp = beløp,
        prosent = prosent,
        satsdag = satsdag.toDbJson(),
        dato = dato,
        antallBarn = antallBarn.value,
        beløpBarnetillegg = beløpBarnetillegg,
    )
}
