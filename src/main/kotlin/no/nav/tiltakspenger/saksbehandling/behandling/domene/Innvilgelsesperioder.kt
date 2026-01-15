package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.trekkFra
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.LocalDate

data class Innvilgelsesperioder(
    val periodisering: IkkeTomPeriodisering<Innvilgelsesperiode>,
) {
    constructor(perioder: List<Innvilgelsesperiode>) : this(
        perioder.map { it.tilPeriodeMedVerdi() }.tilIkkeTomPeriodisering(),
    )
    constructor(vararg perioder: Innvilgelsesperiode) : this(perioder.toList())

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

    init {
        require(periodisering.erSammenhengende) {
            "Vi støtter ikke innvilgelsesperioder med hull riktig ennå!"
        }

        // abn: det føles litt rart å duplisere periodene egentlig :think:
        require(periodisering.perioderMedVerdi.all { it.periode == it.verdi.periode }) {
            "Innvilgelsesperiodene må ha samme perioder som i periodiseringen"
        }
    }

    fun overlapperMed(periode: Periode): Boolean {
        return periodisering.overlapper(periode)
    }

    /**
     * @return [Innvilgelsesperioder] med oppdaterte perioder som overlapper med [tiltaksdeltakelser]
     * eller null dersom ingen overlapper
     * */
    fun krympTilTiltaksdeltakelsesperioder(tiltaksdeltakelser: Tiltaksdeltakelser): Innvilgelsesperioder? {
        val nyeInnvilgelsesperioder = periodisering.verdier.mapNotNull {
            val oppdatertTiltaksdeltakelse =
                tiltaksdeltakelser.getTiltaksdeltakelse(it.valgtTiltaksdeltakelse.internDeltakelseId)

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
}

data class Innvilgelsesperiode(
    val periode: Periode,
    val valgtTiltaksdeltakelse: Tiltaksdeltakelse,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
) {
    init {
        require(valgtTiltaksdeltakelse.deltakelseFraOgMed != null && valgtTiltaksdeltakelse.deltakelseTilOgMed != null) {
            "Kan ikke velge tiltaksdeltakelse med id ${valgtTiltaksdeltakelse.internDeltakelseId} som mangler start- eller sluttdato"
        }

        val deltakelsesperiode =
            Periode(valgtTiltaksdeltakelse.deltakelseFraOgMed, valgtTiltaksdeltakelse.deltakelseTilOgMed)

        require(deltakelsesperiode.inneholderHele(periode)) {
            "Valgt deltakelsesperiode $deltakelsesperiode for tiltak med id ${valgtTiltaksdeltakelse.internDeltakelseId} må være inneholde hele innvilgelsesperioden $periode"
        }
    }

    fun tilPeriodeMedVerdi(): PeriodeMedVerdi<Innvilgelsesperiode> {
        return PeriodeMedVerdi(
            periode = periode,
            verdi = this,
        )
    }
}
