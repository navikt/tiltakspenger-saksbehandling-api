package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.trekkFra
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
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

    val valgteTiltaksdeltagelser: IkkeTomPeriodisering<NonEmptyList<TiltaksdeltakelseIntern>> by lazy {
        periodisering.map { it.verdi.valgteTiltaksdeltakelser }
    }

    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode> by lazy {
        periodisering.map { it.verdi.antallDagerPerMeldeperiode }
    }

    /** Om noen av periodene er innvilget som [ReversertOpphør], altså uten gyldig tiltaksdeltakelse i saksopplysningene. */
    val harReversertOpphør: Boolean by lazy {
        periodisering.verdier.any { it.erReversertOpphør }
    }

    init {
        periodisering.perioderMedVerdi.forEach { (verdi, periode) ->
            verdi.valgteTiltaksdeltakelser.forEach { valgtTiltaksdeltakelse ->
                val deltakelsesperiode = valgtTiltaksdeltakelse.periode as Periode

                require(deltakelsesperiode.inneholderHele(periode)) {
                    "Valgt deltakelsesperiode $deltakelsesperiode for tiltak med id ${valgtTiltaksdeltakelse.internDeltakelseId} må inneholde hele innvilgelsesperioden $periode"
                }
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
     * Oppdaterer tiltaksdeltakelsene fra [tiltaksdeltakelser], og krymper innvilgelsen til periodene hver av dem fortsatt kan innvilges for.
     * Tiltaksdeltakelser som ikke lenger kan innvilges, fjernes fra perioden.
     * En periode deles opp der tiltaksdeltakelsene i den slutter eller starter på ulike dager, og fjernes der ingen av dem kan innvilges.
     * Perioder som er innvilget som [ReversertOpphør] beholdes uendret, siden de ikke skal ha gyldig tiltaksdeltakelse.
     *
     * @return [Innvilgelsesperioder] med oppdaterte tiltaksdeltakelser, eller null dersom ingen av periodene kan innvilges
     */
    fun oppdaterTiltaksdeltakelser(tiltaksdeltakelser: Tiltaksdeltakelser): Innvilgelsesperioder? {
        val nyeInnvilgelsesperioder = periodisering.perioderMedVerdi.toList().flatMap { (verdi, periode) ->
            if (verdi.erReversertOpphør) {
                return@flatMap listOf(PeriodeMedVerdi(verdi, periode))
            }

            val oppdaterteMedPeriode = verdi.tiltaksdeltakelser.mapNotNull { innvilget ->
                val oppdatertTiltaksdeltakelse = tiltaksdeltakelser.getTiltaksdeltakelse(innvilget.internDeltakelseId)

                if (oppdatertTiltaksdeltakelse == null || !oppdatertTiltaksdeltakelse.kanInnvilges) {
                    return@mapNotNull null
                }

                // Perioden i tiltaksdeltagelsen er alltid definert dersom den kan innvilges
                periode.overlappendePeriode(oppdatertTiltaksdeltakelse.periode as Periode)?.let {
                    innvilget.medOppdatertTiltaksdeltakelse(oppdatertTiltaksdeltakelse) to it
                }
            }

            periode.delOppEtterOverlapp(oppdaterteMedPeriode).map {
                PeriodeMedVerdi(verdi.medTiltaksdeltakelser(it.verdi), it.periode)
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
     * Oppdaterer tiltaksdeltakelsene i innvilgelsesperiodene som fortsatt passer med [tiltaksdeltakelser], uten å endre periodene.
     * Tiltaksdeltakelser som ikke lenger passer med perioden, beholdes slik de ble innvilget, slik at saksbehandler kan rette dem selv.
     * Perioder som er innvilget som [ReversertOpphør] beholder alltid tiltaksdeltakelsen fra det opprinnelige vedtaket.
     * Se [ugyldigePerioder].
     */
    fun medOppdaterteTiltaksdeltakelser(tiltaksdeltakelser: Tiltaksdeltakelser): Innvilgelsesperioder {
        return Innvilgelsesperioder(
            periodisering.map { (verdi, periode) ->
                if (verdi.erReversertOpphør) {
                    return@map verdi
                }

                verdi.medTiltaksdeltakelser(
                    verdi.tiltaksdeltakelser.map {
                        if (tiltaksdeltakelser.validerInnvilgelsesperiode(periode, it.internDeltakelseId) == null) {
                            it.medOppdatertTiltaksdeltakelse(tiltaksdeltakelser.getTiltaksdeltakelse(it.internDeltakelseId)!!)
                        } else {
                            it
                        }
                    },
                )
            },
        )
    }

    /**
     * Perioder som er innvilget som [ReversertOpphør] valideres ikke mot [tiltaksdeltakelser], siden de innvilges uten gyldig tiltaksdeltakelse.
     * @return én [UgyldigInnvilgelsesperiode] per tiltaksdeltakelse som ikke passer med perioden sin i [tiltaksdeltakelser], eller null dersom alle er gyldige
     */
    fun ugyldigePerioder(tiltaksdeltakelser: Tiltaksdeltakelser): NonEmptyList<UgyldigInnvilgelsesperiode>? {
        return periodisering.perioderMedVerdi.filterNot { it.verdi.erReversertOpphør }.flatMap { (verdi, periode) ->
            verdi.tiltaksdeltakelser.mapNotNull {
                tiltaksdeltakelser.validerInnvilgelsesperiode(periode, it.internDeltakelseId)
            }
        }.toNonEmptyListOrNull()
    }

    /** Sjekker om alle innvilgelsesperiodene, bortsett fra de som er innvilget som [ReversertOpphør], er innenfor tiltaksdeltakelsesperiodene */
    fun erInnenforTiltaksperiodene(saksopplysninger: Saksopplysninger): Boolean {
        return periodisering.perioderMedVerdi
            .filterNot { it.verdi.erReversertOpphør }
            .map { it.periode }
            .trekkFra(saksopplysninger.tiltaksdeltakelser.perioder)
            .isEmpty()
    }

    fun hentVerdiForDag(dato: LocalDate): InnvilgelsesperiodeVerdi? {
        return periodisering.hentVerdiForDag(dato)
    }
}

/**
 * @param tiltaksdeltakelser Tiltaksdeltakelsene det innvilges for i perioden, slik de var registrert da perioden ble innvilget.
 * Den første tiltaksdeltakelsen er den saksbehandler har valgt først, og den bestemmer tiltakstypen for perioden, se [tiltakstype].
 * @param reversertOpphør Er satt dersom perioden er innvilget som [ReversertOpphør], altså på nytt etter å ha vært opphørt eller stanset, uten gyldig tiltaksdeltakelse i saksopplysningene.
 * Da er [tiltaksdeltakelser] tiltaksdeltakelsene fra det opprinnelige vedtaket.
 */
data class InnvilgelsesperiodeVerdi(
    val tiltaksdeltakelser: NonEmptyList<InnvilgetTiltaksdeltakelse>,
    val reversertOpphør: ReversertOpphør?,
) {
    val erReversertOpphør: Boolean get() = reversertOpphør != null

    val valgteTiltaksdeltakelser: NonEmptyList<TiltaksdeltakelseIntern> get() = tiltaksdeltakelser.map { it.tiltaksdeltakelse }

    /** Summen av antall dager per meldeperiode for tiltaksdeltakelsene, se [summer]. */
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode get() = tiltaksdeltakelser.map { it.antallDagerPerMeldeperiode }.summer()

    /**
     * Tiltakstypen dagene i perioden beregnes og utbetales med.
     * En dag kan bare utbetales med én tiltakstype, så når det innvilges for flere tiltaksdeltakelser, er det den første som gjelder.
     */
    val tiltakstype: TiltakstypeSomGirRett get() = tiltaksdeltakelser.head.tiltaksdeltakelse.tiltakstype

    init {
        require(tiltaksdeltakelser.map { it.internDeltakelseId }.distinct().size == tiltaksdeltakelser.size) {
            "En innvilgelsesperiode kan ikke ha samme tiltaksdeltakelse flere ganger: ${tiltaksdeltakelser.map { it.internDeltakelseId }}"
        }
    }

    fun innvilgerTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId): Boolean {
        return tiltaksdeltakelser.any { it.internDeltakelseId == internDeltakelseId }
    }

    fun hentTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId): InnvilgetTiltaksdeltakelse? {
        return tiltaksdeltakelser.singleOrNull { it.internDeltakelseId == internDeltakelseId }
    }

    /** @return perioden uten tiltaksdeltakelsen med [internDeltakelseId], eller null dersom det ikke er andre tiltaksdeltakelser igjen */
    fun utenTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId): InnvilgelsesperiodeVerdi? {
        return tiltaksdeltakelser.filterNot { it.internDeltakelseId == internDeltakelseId }
            .toNonEmptyListOrNull()
            ?.let { medTiltaksdeltakelser(it) }
    }

    fun medTiltaksdeltakelser(tiltaksdeltakelser: NonEmptyList<InnvilgetTiltaksdeltakelse>): InnvilgelsesperiodeVerdi {
        return this.copy(tiltaksdeltakelser = tiltaksdeltakelser)
    }

    fun tilPeriodeMedVerdi(periode: Periode): PeriodeMedVerdi<InnvilgelsesperiodeVerdi> {
        return PeriodeMedVerdi(
            periode = periode,
            verdi = this,
        )
    }
}

/**
 * @param tiltaksdeltakelse Tiltaksdeltakelsen det innvilges for, slik den var registrert da perioden ble innvilget.
 * @param antallDagerPerMeldeperiode Antall dager per meldeperiode det innvilges for denne tiltaksdeltakelsen.
 */
data class InnvilgetTiltaksdeltakelse(
    val tiltaksdeltakelse: TiltaksdeltakelseIntern,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
) {
    val internDeltakelseId: TiltaksdeltakerId get() = tiltaksdeltakelse.internDeltakelseId

    init {
        require(tiltaksdeltakelse.periode is Periode) {
            "Kan ikke innvilge for tiltaksdeltakelse med id ${tiltaksdeltakelse.internDeltakelseId} som mangler start- eller sluttdato"
        }
    }

    fun medOppdatertTiltaksdeltakelse(oppdatert: TiltaksdeltakelseIntern): InnvilgetTiltaksdeltakelse {
        require(oppdatert.internDeltakelseId == internDeltakelseId) {
            "Kan ikke bytte tiltaksdeltakelse $internDeltakelseId med ${oppdatert.internDeltakelseId}"
        }
        return this.copy(tiltaksdeltakelse = oppdatert)
    }
}

/** Periodene der tiltaksdeltakelsen med [internDeltakelseId] er innvilget, med antall dager som er innvilget for den. */
fun Periodisering<InnvilgelsesperiodeVerdi>.forTiltaksdeltakelse(
    internDeltakelseId: TiltaksdeltakerId,
): Periodisering<InnvilgetTiltaksdeltakelse> {
    // Bruker ikke Periodisering.flatMap, siden den kaster når resultatet blir tomt eller får hull.
    return perioderMedVerdi
        .mapNotNull { (verdi, periode) -> verdi.hentTiltaksdeltakelse(internDeltakelseId)?.let { PeriodeMedVerdi(it, periode) } }
        .tilPeriodisering()
}

/**
 * Deler [this] opp i delperioder der de samme elementene overlapper, og utelater delperiodene uten elementer.
 * Elementene i hver delperiode har samme rekkefølge som i [elementer].
 */
fun <T> Periode.delOppEtterOverlapp(elementer: List<Pair<T, Periode>>): List<PeriodeMedVerdi<NonEmptyList<T>>> {
    val overlappende = elementer.mapNotNull { (element, periode) -> overlappendePeriode(periode)?.let { element to it } }

    val grenser = (overlappende.flatMap { (_, periode) -> listOf(periode.fraOgMed, periode.tilOgMed.plusDays(1)) } + fraOgMed + tilOgMed.plusDays(1))
        .distinct()
        .sorted()

    return grenser.zipWithNext().mapNotNull { (fraOgMed, tilOgMedEksklusiv) ->
        val delperiode = Periode(fraOgMed, tilOgMedEksklusiv.minusDays(1))
        overlappende.filter { (_, periode) -> periode.inneholderHele(delperiode) }
            .map { it.first }
            .toNonEmptyListOrNull()
            ?.let { PeriodeMedVerdi(it, delperiode) }
    }
}
