package no.nav.tiltakspenger.saksbehandling.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.inneholderOverlapp
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import java.time.LocalDate

/**
 * Representerer en periodisering av barnetillegg.
 */
data class Barnetillegg(
    val periodisering: Periodisering<AntallBarn>,
    val begrunnelse: BegrunnelseVilkårsvurdering?,
) {
    init {
        require(periodisering.any { it.verdi != AntallBarn.ZERO }) { "Barnetillegg må ha minst én periode med antall barn > 0" }
    }

    /** @return 0 dersom datoen er utenfor periodiseringen. */
    fun antallBarnPåDato(dato: LocalDate): AntallBarn {
        return periodisering.hentVerdiForDag(dato) ?: AntallBarn.ZERO
    }

    /**
     * Endrer ikke eksisterende periode.
     */
    fun utvidPeriode(utvidTil: Periode, antallBarn: AntallBarn): Barnetillegg {
        return Barnetillegg(
            periodisering = periodisering.utvid(antallBarn, utvidTil),
            begrunnelse = begrunnelse,
        )
    }

    companion object {
        fun periodiserOgFyllUtHullMed0(
            perioder: BarnetilleggPerioder,
            begrunnelse: BegrunnelseVilkårsvurdering?,
            virkningsperiode: Periode?,
        ) = Barnetillegg(
            periodisering = perioder.periodiserOgFyllUtHullMed0(virkningsperiode),
            begrunnelse = begrunnelse,
        )
    }
}

typealias BarnetilleggPerioder = List<Pair<Periode, AntallBarn>>

/**
 * Periodiserer og fyller ut hull med 0.
 * @throws IllegalArgumentException Dersom periodene er utenfor virkningsperioden eller overlapper.
 */
internal fun BarnetilleggPerioder.periodiserOgFyllUtHullMed0(virkningsperiode: Periode?): Periodisering<AntallBarn> {
    if (this.map { it.first }.inneholderOverlapp()) {
        throw IllegalArgumentException("Periodene kan ikke overlappe")
    }

    return this.fold(
        Periodisering(
            AntallBarn.ZERO,
            virkningsperiode ?: this.map { it.first }
                .let { perioder ->
                    Periode(
                        perioder.minOf { it.fraOgMed },
                        perioder.maxOf { it.tilOgMed },
                    )
                },
        ),
    ) { periodisering, (periode, antallBarn) ->
        periodisering.setVerdiForDelPeriode(antallBarn, periode)
    }
}
