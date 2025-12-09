package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
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
        periodisering.verdier
            .map { PeriodeMedVerdi(it.valgtTiltaksdeltakelse, it.periode) }
            .tilIkkeTomPeriodisering()
    }

    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode> by lazy {
        periodisering.verdier
            .map { PeriodeMedVerdi(it.antallDagerPerMeldeperiode, it.periode) }
            .tilIkkeTomPeriodisering()
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

    companion object {

        /**
         *  TODO abn: dette er en midlertidig løsning for å opprette [Innvilgelsesperioder] fra eksisterende db-schema
         *  og DTO/kommando-formater. Når vi får migrert databasen etc trenger vi ikke denne lengre.
         * **/
        fun create(
            saksopplysninger: Saksopplysninger,
            innvilgelsesperioder: List<Periode>,
            antallDagerPerMeldeperiode: List<Pair<Periode, AntallDagerForMeldeperiode>>,
            tiltaksdeltakelser: List<Pair<Periode, String>>,
        ): Innvilgelsesperioder {
            val unikePerioder = innvilgelsesperioder
                .plus(antallDagerPerMeldeperiode.map { it.first })
                .plus(tiltaksdeltakelser.map { it.first })
                .toNonEmptyListOrNull()!!
                .tilPerioderUtenHullEllerOverlapp()

            val antallDagerPeriodisert = antallDagerPerMeldeperiode.map {
                PeriodeMedVerdi(it.second, it.first)
            }.tilIkkeTomPeriodisering()

            val tiltakPeriodisert = tiltaksdeltakelser.map {
                PeriodeMedVerdi(it.second, it.first)
            }.tilIkkeTomPeriodisering()

            val innvilgelsesperioder: List<PeriodeMedVerdi<Innvilgelsesperiode>> = unikePerioder.map {
                Innvilgelsesperiode(
                    periode = it,
                    valgtTiltaksdeltakelse = saksopplysninger.getTiltaksdeltakelse(
                        tiltakPeriodisert.hentVerdiForDag(it.fraOgMed)!!,
                    )!!,
                    antallDagerPerMeldeperiode = antallDagerPeriodisert.hentVerdiForDag(it.tilOgMed)!!,
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

        val tilOgMed = if (nesteFraOgMed != null) nesteFraOgMed.minusDays(1) else sisteTilOgMed

        Periode(
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
        )
    }.toNonEmptyListOrNull()!!
}
