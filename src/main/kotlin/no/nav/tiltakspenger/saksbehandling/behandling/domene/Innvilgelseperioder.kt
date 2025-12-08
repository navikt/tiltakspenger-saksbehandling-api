package no.nav.tiltakspenger.saksbehandling.behandling.domene

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

        fun create(
            saksopplysninger: Saksopplysninger,
            innvilgelsesperioder: List<Periode>,
            antallDagerPerMeldeperiode: List<Pair<Periode, AntallDagerForMeldeperiode>>,
            tiltaksdeltakelser: List<Pair<Periode, String>>,
        ): Innvilgelsesperioder {
            val unikePerioder = innvilgelsesperioder
                .plus(antallDagerPerMeldeperiode.map { it.first })
                .plus(tiltaksdeltakelser.map { it.first })
                .distinctBy { it }
                .sortedBy { it.fraOgMed }

            if (unikePerioder.size == 1) {
                val periode = unikePerioder.first()

                return Innvilgelsesperioder(
                    periodisering = listOf(
                        Innvilgelsesperiode(
                            periode = periode,
                            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode.single().second,
                            valgtTiltaksdeltakelse = saksopplysninger.getTiltaksdeltakelse(tiltaksdeltakelser.single().second)!!,
                        ).tilPeriodeMedVerdi(),
                    ).tilIkkeTomPeriodisering(),
                )
            }

            val antallDagerPeriodisert = antallDagerPerMeldeperiode.map {
                PeriodeMedVerdi(it.second, it.first)
            }.tilIkkeTomPeriodisering()

            val tiltakPeriodisert = tiltaksdeltakelser.map {
                PeriodeMedVerdi(it.second, it.first)
            }.tilIkkeTomPeriodisering()

            val innvilgelsesperioder: List<PeriodeMedVerdi<Innvilgelsesperiode>> = unikePerioder.zipWithNext { a, b ->
                val periode = Periode(
                    fraOgMed = a.fraOgMed,
                    tilOgMed = minOf(
                        a.tilOgMed,
                        b.fraOgMed.minusDays(1),
                    ),
                )

                Innvilgelsesperiode(
                    periode = periode,
                    valgtTiltaksdeltakelse = saksopplysninger.getTiltaksdeltakelse(
                        tiltakPeriodisert.hentVerdiForDag(periode.fraOgMed)!!,
                    )!!,
                    antallDagerPerMeldeperiode = antallDagerPeriodisert.hentVerdiForDag(periode.tilOgMed)!!,
                ).tilPeriodeMedVerdi()
            }.let {
                val nestSistePeriode = it.last().periode
                val sistePeriode = unikePerioder.last()

                val nySistePeriode = Periode(
                    fraOgMed = maxOf(sistePeriode.fraOgMed, nestSistePeriode.tilOgMed.plusDays(1)),
                    tilOgMed = sistePeriode.tilOgMed,
                )

                it.plus(
                    Innvilgelsesperiode(
                        periode = nySistePeriode,
                        valgtTiltaksdeltakelse = saksopplysninger.getTiltaksdeltakelse(
                            tiltakPeriodisert.hentVerdiForDag(nySistePeriode.fraOgMed)!!,
                        )!!,
                        antallDagerPerMeldeperiode = antallDagerPeriodisert.hentVerdiForDag(nySistePeriode.tilOgMed)!!,
                    ).tilPeriodeMedVerdi(),
                )
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
