package no.nav.tiltakspenger.saksbehandling.domene.vedtak

import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Utfallsperiode
import java.time.LocalDate

data class Vedtaksliste(
    val value: List<Rammevedtak>,
) : List<Vedtak> by value {
    @Suppress("unused")
    constructor(value: Rammevedtak) : this(listOf(value))

    val tidslinje: Periodisering<Rammevedtak> by lazy { value.toTidslinje() }

    val førstegangsvedtak: Rammevedtak? by lazy { value.singleOrNullOrThrow { it.erFørstegangsvedtak } }

    /** Nåtilstand. Dette er sakens totale vedtaksperiode. Vær veldig obs når du bruker denne, fordi den sier ikke noe om antall perioder, om de gir rett eller ikke. */
    val vedtaksperiode: Periode? by lazy { tidslinje.ifEmpty { null }?.totalePeriode }

    /** Nåtilstand. Sakens totale vedtaksperioder. Vil kunne ha hull dersom det f.eks. er opphold mellom 2 tiltaksdeltagelsesperioder. Avslag og delvis avslag vil ikke være med her. */
    @Suppress("unused")
    val vedtaksperioder: List<Periode> by lazy { tidslinje.perioder }

    /** Nåtilstand. De periodene som gir rett til tiltakspenger. Vil kunne være hull. */
    val innvilgelsesperioder: List<Periode> by lazy {
        tidslinje.filter { it.verdi.vedtaksType == Vedtakstype.INNVILGELSE }.map { it.periode }
    }

    val innvilgetTidslinje: Periodisering<Rammevedtak> by lazy {
        value.filter { it.vedtaksType == Vedtakstype.INNVILGELSE }.toTidslinje()
    }

    val antallInnvilgelsesperioder: Int by lazy { innvilgelsesperioder.size }

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val førsteDagSomGirRett: LocalDate? by lazy {
        innvilgelsesperioder.minOfOrNull { it.fraOgMed }
    }

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val sisteDagSomGirRett: LocalDate? by lazy {
        innvilgelsesperioder.maxOfOrNull { it.tilOgMed }
    }

    /**
     * Perioden må være innenfor tidslinjen
     *
     * **/
    fun tidslinjeForPeriode(periode: Periode): Periodisering<Rammevedtak> {
        return tidslinje.krymp(periode)
    }

    /**
     * Innvilget->Stans eksempel:
     * Vedtak 1: 01.01.2021 - 04.01.2021 (oppfylt/innvilget)
     * Vedtak 2: 03.01.2021 - 04.01.2021 (ikke oppfylt/stans)
     * Utfallsperioder: 01.01.2021 - 02.01.2021 Oppfylt (fra vedtak 1)
     *                  03.01.2021 - 04.01.2021 Ikke Oppfylt (fra vedtak 2)
     */
    val utfallsperioder: Periodisering<Utfallsperiode> by lazy {
        tidslinje.perioderMedVerdi.flatMap { pmvVedtak ->
            pmvVedtak.verdi.utfallsperioder!!.perioderMedVerdi.mapNotNull {
                it.periode.overlappendePeriode(pmvVedtak.periode)?.let { overlappendePeriode ->
                    PeriodeMedVerdi(
                        periode = overlappendePeriode,
                        verdi = it.verdi,
                    )
                }
            }
        }.let { Periodisering(it) }
    }

    val saksopplysningerperiode: Periodisering<Saksopplysninger> by lazy {
        tidslinje.perioderMedVerdi.map { PeriodeMedVerdi(it.verdi.behandling.saksopplysninger, it.periode) }.let {
            Periodisering(it)
        }
    }

    private val tiltaksdeltakelsesperioderFraSaksopplysninger: Periodisering<Tiltaksdeltagelse> by lazy {
        saksopplysningerperiode.map { it.tiltaksdeltagelse.first() }
    }

    // TODO: Blir dette riktig for revurdering og evt meldekort?
    val valgteTiltaksdeltakelser: Periodisering<Tiltaksdeltagelse> by lazy {
        innvilgetTidslinje.perioderMedVerdi.filter { it.verdi.behandling.valgteTiltaksdeltakelser != null }
            .flatMap { it.verdi.behandling.valgteTiltaksdeltakelser!!.periodisering.krymp(it.periode).perioderMedVerdi }.let {
                Periodisering(it)
            }
    }

    fun valgteTiltaksdeltakelserForPeriode(periode: Periode): Periodisering<Tiltaksdeltagelse> {
        return valgteTiltaksdeltakelser.krymp(periode)
    }

    private val valgteTiltaksdeltakelserForForstegangsvedtak: Periodisering<Tiltaksdeltagelse> by lazy {
        innvilgetTidslinje.perioderMedVerdi.filter { it.verdi.behandling.valgteTiltaksdeltakelser != null && it.verdi.erFørstegangsvedtak }
            .flatMap { it.verdi.behandling.valgteTiltaksdeltakelser!!.periodisering.krymp(it.periode).perioderMedVerdi }.let {
                Periodisering(it)
            }
    }

    fun valgteTiltaksdeltakelserForForstegangsvedtakOverlapperMedPeriode(periode: Periode): List<Tiltaksdeltagelse> {
        return valgteTiltaksdeltakelserForForstegangsvedtak.perioderMedVerdi
            .filter { it.periode.overlapperMed(periode) }
            .map { it.verdi }
    }

    /** Tidslinje for antall barn. Første og siste periode vil være 1 eller flere. Kan inneholde hull med 0 barn. */
    val barnetilleggsperioder: Periodisering<AntallBarn> by lazy {
        tidslinje.perioderMedVerdi.flatMap { pmvVedtak ->
            (pmvVedtak.verdi.barnetillegg?.periodisering?.perioderMedVerdi ?: emptyList()).mapNotNull {
                it.periode.overlappendePeriode(pmvVedtak.periode)?.let { overlappendePeriode ->
                    PeriodeMedVerdi(
                        periode = overlappendePeriode,
                        verdi = it.verdi,
                    )
                }
            }
        }.let { Periodisering(it) }
    }

    val tiltakstypeperioder: Periodisering<TiltakstypeSomGirRett> by lazy {
        valgteTiltaksdeltakelser.map { it.typeKode }
    }

    fun antallBarnForDag(dag: LocalDate): AntallBarn {
        return barnetilleggsperioder.singleOrNullOrThrow { it.periode.inneholder(dag) }?.verdi ?: AntallBarn.ZERO
    }

    fun leggTilFørstegangsVedtak(vedtak: Rammevedtak): Vedtaksliste {
        when (vedtak.vedtaksType) {
            Vedtakstype.INNVILGELSE -> require(this.isEmpty()) { "Vedtaksliste must not be empty." }
            Vedtakstype.STANS -> Unit
        }
        return copy(value = listOf(vedtak))
    }

    fun hentTiltaksdataForPeriode(periode: Periode): List<Tiltaksdeltagelse> {
        return valgteTiltaksdeltakelserForPeriode(periode).perioderMedVerdi.map { it.verdi }
    }

    init {
        value.map { it.id }.let {
            require(it.size == it.distinct().size) { "Vedtakene må ha unike IDer men var: $it" }
        }
        value.zipWithNext().forEach {
            require(it.first.opprettet.isBefore(it.second.opprettet)) { "Vedtakene må være sortert på opprettet-tidspunkt, men var: ${value.map { it.opprettet }}" }
        }
    }

    companion object {
        fun empty() = Vedtaksliste(emptyList())
    }
}
