package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilSammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.trekkFra
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.LocalDate

data class Innvilgelsesperioder(
    val periodisering: IkkeTomPeriodisering<Innvilgelsesperiode>,
) {
    val totalPeriode: Periode = periodisering.totalPeriode
    val fraOgMed: LocalDate = totalPeriode.fraOgMed
    val tilOgMed: LocalDate = totalPeriode.tilOgMed

    val perioder: List<Periode> = periodisering.perioder

    val valgteTiltaksdeltagelser: IkkeTomPeriodisering<Tiltaksdeltakelse> by lazy {
        periodisering.map { it.verdi.valgtTiltaksdeltakelse }
    }

    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode> by lazy {
        periodisering.map { it.verdi.antallDagerPerMeldeperiode }
    }

    /**
     * @return [Innvilgelsesperioder] med oppdaterte perioder som overlapper med [tiltaksdeltakelser]
     * eller null dersom ingen overlapper
     * */
    fun krympTilTiltaksdeltakelsesperioder(tiltaksdeltakelser: Tiltaksdeltakelser): Innvilgelsesperioder? {
        val nyeInnvilgelsesperioder = periodisering.verdier.mapNotNull {
            val oppdatertTiltaksdeltakelse =
                tiltaksdeltakelser.getTiltaksdeltakelse(it.valgtTiltaksdeltakelse.eksternDeltakelseId)

            if (oppdatertTiltaksdeltakelse == null || !oppdatertTiltaksdeltakelse.kanInnvilges) {
                return@mapNotNull null
            }

            // Perioden i tiltaksdeltagelsen er alltid definert dersom den kan innvilges
            val overlappendePeriode =
                it.periode.overlappendePeriode(oppdatertTiltaksdeltakelse.periode as Periode) ?: return@mapNotNull null

            it.copy(
                periode = overlappendePeriode,
                valgtTiltaksdeltakelse = oppdatertTiltaksdeltakelse,
            ).tilPeriodeMedVerdi()
        }

        if (nyeInnvilgelsesperioder.isEmpty()) {
            return null
        }

        return Innvilgelsesperioder(
            nyeInnvilgelsesperioder.tilIkkeTomPeriodisering(),
        )
    }

    /** Sjekker om alle innvilgelsesperiodene er innenfor tiltaksdeltakelsesperiodene */
    fun erInnenforTiltaksperiodene(saksopplysninger: Saksopplysninger): Boolean {
        return this.perioder.trekkFra(saksopplysninger.tiltaksdeltakelser.perioder).isEmpty()
    }

    companion object {

        /**
         *  TODO abn: dette er en midlertidig løsning for å opprette [Innvilgelsesperioder] fra eksisterende db-schema
         *  og DTO/kommando-formater. Når vi får migrert databasen etc trenger vi ikke denne lengre.
         * **/
        fun create(
            saksopplysninger: Saksopplysninger,
            innvilgelsesperiode: Periode,
            antallDagerPerMeldeperiode: List<Pair<Periode, AntallDagerForMeldeperiode>>,
            tiltaksdeltakelser: List<Pair<Periode, String>>,
        ): Innvilgelsesperioder {
            val antallDagerPeriodisert = antallDagerPerMeldeperiode.map {
                PeriodeMedVerdi(it.second, it.first)
            }.tilSammenhengendePeriodisering()

            val tiltakPeriodisert = tiltaksdeltakelser.map {
                PeriodeMedVerdi(it.second, it.first)
            }.tilSammenhengendePeriodisering()

            require(antallDagerPeriodisert.totalPeriode == innvilgelsesperiode) {
                "Periodisering av antall dager må ha totalperiode lik innvilgelsesperioden"
            }

            require(tiltakPeriodisert.totalPeriode == innvilgelsesperiode) {
                "Periodisering av tiltaksdeltakelse må ha totalperiode lik innvilgelsesperioden"
            }

            val unikePerioder = nonEmptyListOf(innvilgelsesperiode)
                .plus(antallDagerPeriodisert.perioder)
                .plus(tiltakPeriodisert.perioder)
                .tilPerioderUtenHullEllerOverlapp()

            val innvilgelsesperioder: List<PeriodeMedVerdi<Innvilgelsesperiode>> = unikePerioder.map {
                Innvilgelsesperiode(
                    periode = it,
                    valgtTiltaksdeltakelse = saksopplysninger.getTiltaksdeltakelse(
                        tiltakPeriodisert.hentVerdiForDag(it.fraOgMed)!!,
                    )!!,
                    antallDagerPerMeldeperiode = antallDagerPeriodisert.hentVerdiForDag(it.fraOgMed)!!,
                ).tilPeriodeMedVerdi()
            }

            return Innvilgelsesperioder(periodisering = innvilgelsesperioder.tilIkkeTomPeriodisering())
        }
    }
}

data class Innvilgelsesperiode(
    val periode: Periode,
    val valgtTiltaksdeltakelse: Tiltaksdeltakelse,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
) {

    init {
        require(valgtTiltaksdeltakelse.deltakelseFraOgMed != null && valgtTiltaksdeltakelse.deltakelseTilOgMed != null) {
            "Kan ikke velge tiltaksdeltakelse med id ${valgtTiltaksdeltakelse.eksternDeltakelseId} som mangler start- eller sluttdato"
        }

        val deltakelsesperiode =
            Periode(valgtTiltaksdeltakelse.deltakelseFraOgMed, valgtTiltaksdeltakelse.deltakelseTilOgMed)

        require(deltakelsesperiode.inneholderHele(periode)) {
            "Valgt deltakelsesperiode $deltakelsesperiode for tiltak med id ${valgtTiltaksdeltakelse.eksternDeltakelseId} må være inneholde hele innvilgelsesperioden $periode"
        }
    }

    fun tilPeriodeMedVerdi(): PeriodeMedVerdi<Innvilgelsesperiode> {
        return PeriodeMedVerdi(
            periode = periode,
            verdi = this,
        )
    }
}

private fun NonEmptyList<Periode>.tilPerioderUtenHullEllerOverlapp(): NonEmptyList<Periode> {
    val unikePerioder: NonEmptyList<Periode> = this.distinct()

    if (unikePerioder.size == 1) {
        return unikePerioder
    }

    val sisteTilOgMed: LocalDate = unikePerioder.map { it.tilOgMed }.max()

    val fraOgMedDatoer = unikePerioder.toList().flatMap { periode ->
        if (periode.tilOgMed == sisteTilOgMed) {
            listOf(periode.fraOgMed)
        } else {
            listOf(periode.fraOgMed, periode.tilOgMed.plusDays(1))
        }
    }.distinct().sorted()

    return fraOgMedDatoer.mapIndexed { index, fraOgMed ->
        val nesteFraOgMed = fraOgMedDatoer.getOrNull(index + 1)

        val tilOgMed = nesteFraOgMed?.minusDays(1) ?: sisteTilOgMed

        Periode(
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
        )
    }.toNonEmptyListOrNull()!!
}
