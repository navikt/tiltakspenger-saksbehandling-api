package no.nav.tiltakspenger.vedtak.meldekort.domene

import no.nav.tiltakspenger.vedtak.barnetillegg.AntallBarn
import no.nav.tiltakspenger.vedtak.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.vedtak.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.vedtak.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.vedtak.utbetaling.domene.Satser
import java.time.LocalDate

data class Beregningsdag(
    val beløp: Int,
    val beløpBarnetillegg: Int,
    val prosent: Int,
    val satsdag: Satsdag,
    val dato: LocalDate,
    val antallBarn: AntallBarn,
) {
    init {
        require(dato == satsdag.dato)
        if (beløpBarnetillegg > 0) {
            require(antallBarn.value > 0) {
                "Antall barn må være større enn 0 for å få barnetillegg"
            }
        }
        if (beløpBarnetillegg > 0) {
            require(beløp > 0) {
                "Man kan ikke få barnetillegg uten å få hovedytelse"
            }
        }
    }
}

fun beregnDag(
    dato: LocalDate,
    reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
    antallBarn: AntallBarn,
): Beregningsdag = Satser.sats(dato).let {
    val prosent = when (reduksjon) {
        IngenReduksjon -> 100
        Reduksjon -> 75
        YtelsenFallerBort -> 0
    }
    Beregningsdag(
        beløp = when (reduksjon) {
            IngenReduksjon -> it.sats
            Reduksjon -> it.satsRedusert
            YtelsenFallerBort -> 0
        },
        beløpBarnetillegg = when (reduksjon) {
            IngenReduksjon -> it.satsBarnetillegg * antallBarn.value
            Reduksjon -> it.satsBarnetilleggRedusert * antallBarn.value
            YtelsenFallerBort -> 0
        },
        prosent = prosent,
        satsdag = it,
        dato = dato,
        antallBarn = antallBarn,
    )
}
