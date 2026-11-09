package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.trekkFra
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import java.time.LocalDate

data class Innvilgelsesperioder(
    val periodisering: IkkeTomPeriodisering<InnvilgelsesperiodeVerdi>,
) {
    constructor(perioder: List<Pair<InnvilgelsesperiodeVerdi, Periode>>) : this(
        perioder.map {
            it.first.tilPeriodeMedVerdi(it.second)
        }.tilIkkeTomPeriodisering(),
    )

    val totalPeriode: Periode = periodisering.totalPeriode
    val fraOgMed: LocalDate = totalPeriode.fraOgMed
    val tilOgMed: LocalDate = totalPeriode.tilOgMed

    val perioder: NonEmptyList<Periode> = periodisering.perioder

    val valgteTiltaksdeltagelser: IkkeTomPeriodisering<TiltaksdeltakelseIntern> by lazy {
        periodisering.map { it.verdi.valgtTiltaksdeltakelse }
    }

    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode> by lazy {
        periodisering.map { it.verdi.antallDagerPerMeldeperiode }
    }

    init {
        periodisering.perioderMedVerdi.forEach {
            val valgtTiltaksdeltakelse = it.verdi.valgtTiltaksdeltakelse

            val deltakelsesperiode = valgtTiltaksdeltakelse.periode as Periode

            require(deltakelsesperiode.inneholderHele(it.periode)) {
                "Valgt deltakelsesperiode $deltakelsesperiode for tiltak med id ${valgtTiltaksdeltakelse.internDeltakelseId} må inneholde hele innvilgelsesperioden ${it.periode}"
            }
        }
    }

    fun overlapperMed(periode: Periode): Boolean {
        return periodisering.overlapper(periode)
    }

    /**
     * @return [Innvilgelsesperioder] med oppdaterte perioder som overlapper med [perioder] eller null dersom ingen overlapper
     * */
    fun krymp(perioder: List<Periode>): Innvilgelsesperioder? {
        val nyeInnvilgelsesperioder = periodisering.perioderMedVerdi.toList().flatMap {
            it.periode.overlappendePerioder(perioder).map { periode ->
                PeriodeMedVerdi(
                    verdi = it.verdi,
                    periode = periode,
                )
            }
        }

        if (nyeInnvilgelsesperioder.isEmpty()) {
            return null
        }

        return Innvilgelsesperioder(
            nyeInnvilgelsesperioder.tilIkkeTomPeriodisering(),
        )
    }

    /**
     * @return [Innvilgelsesperioder] med oppdaterte tiltaksdeltakelser, for innvilgelsesperioder som overlapper med perioder fra [tiltaksdeltakelser] eller null dersom ingen overlapper
     * */
    fun oppdaterTiltaksdeltakelser(tiltaksdeltakelser: Tiltaksdeltakelser): Innvilgelsesperioder? {
        val nyeInnvilgelsesperioder = periodisering.perioderMedVerdi.mapNotNull {
            val oppdatertTiltaksdeltakelse =
                tiltaksdeltakelser.getTiltaksdeltakelse(it.verdi.valgtTiltaksdeltakelse.internDeltakelseId)

            if (oppdatertTiltaksdeltakelse == null || !oppdatertTiltaksdeltakelse.kanInnvilges) {
                return@mapNotNull null
            }

            // Perioden i tiltaksdeltagelsen er alltid definert dersom den kan innvilges
            val overlappendePeriode =
                it.periode.overlappendePeriode(oppdatertTiltaksdeltakelse.periode as Periode) ?: return@mapNotNull null

            PeriodeMedVerdi(
                periode = overlappendePeriode,
                verdi = it.verdi.copy(
                    valgtTiltaksdeltakelse = oppdatertTiltaksdeltakelse,
                ),
            )
        }

        if (nyeInnvilgelsesperioder.isEmpty()) {
            return null
        }

        return Innvilgelsesperioder(
            nyeInnvilgelsesperioder.tilIkkeTomPeriodisering(),
        )
    }

    /**
     * Oppdaterer tiltaksdeltakelsene i innvilgelsesperiodene som fortsatt passer med [tiltaksdeltakelser], uten å endre periodene.
     * Innvilgelsesperioder som ikke lenger passer, beholder tiltaksdeltakelsen de ble innvilget for, slik at saksbehandler kan rette dem selv.
     * Se [ugyldigePerioder].
     */
    fun medOppdaterteTiltaksdeltakelser(tiltaksdeltakelser: Tiltaksdeltakelser): Innvilgelsesperioder {
        return Innvilgelsesperioder(
            periodisering.map {
                val internDeltakelseId = it.verdi.valgtTiltaksdeltakelse.internDeltakelseId
                if (tiltaksdeltakelser.validerInnvilgelsesperiode(it.periode, internDeltakelseId) == null) {
                    it.verdi.copy(valgtTiltaksdeltakelse = tiltaksdeltakelser.getTiltaksdeltakelse(internDeltakelseId)!!)
                } else {
                    it.verdi
                }
            },
        )
    }

    /** @return innvilgelsesperiodene som ikke passer med [tiltaksdeltakelser], eller null dersom alle er gyldige */
    fun ugyldigePerioder(tiltaksdeltakelser: Tiltaksdeltakelser): NonEmptyList<UgyldigInnvilgelsesperiode>? {
        return periodisering.perioderMedVerdi.mapNotNull {
            tiltaksdeltakelser.validerInnvilgelsesperiode(it.periode, it.verdi.valgtTiltaksdeltakelse.internDeltakelseId)
        }.toNonEmptyListOrNull()
    }

    /** Sjekker om alle innvilgelsesperiodene er innenfor tiltaksdeltakelsesperiodene */
    fun erInnenforTiltaksperiodene(saksopplysninger: Saksopplysninger): Boolean {
        return this.perioder.trekkFra(saksopplysninger.tiltaksdeltakelser.perioder).isEmpty()
    }

    fun hentVerdiForDag(dato: LocalDate): InnvilgelsesperiodeVerdi? {
        return periodisering.hentVerdiForDag(dato)
    }
}

data class InnvilgelsesperiodeVerdi(
    val valgtTiltaksdeltakelse: TiltaksdeltakelseIntern,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
) {
    init {
        require(valgtTiltaksdeltakelse.periode is Periode) {
            "Kan ikke innvilge for tiltaksdeltakelse med id ${valgtTiltaksdeltakelse.internDeltakelseId} som mangler start- eller sluttdato"
        }
    }

    fun tilPeriodeMedVerdi(periode: Periode): PeriodeMedVerdi<InnvilgelsesperiodeVerdi> {
        return PeriodeMedVerdi(
            periode = periode,
            verdi = this,
        )
    }
}
