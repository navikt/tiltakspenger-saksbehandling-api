package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinjeMedHull
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import java.time.LocalDate

data class Vedtaksliste(
    val value: List<Rammevedtak>,
) : List<Vedtak> by value {
    @Suppress("unused")
    constructor(value: Rammevedtak) : this(listOf(value))

    val fnr = value.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow()
    val sakId = value.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow()
    val saksnummer = value.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()

    val tidslinje: Periodisering<Rammevedtak?> by lazy { value.toTidslinjeMedHull() }

    val innvilgelsesperiode: Periode? by lazy {
        innvilgelsesperioder.ifEmpty { null }?.let { Periode(it.minOf { it.fraOgMed }, it.maxOf { it.tilOgMed }) }
    }

    /** Nåtilstand. Sakens totale vedtaksperioder. Vil kunne ha hull dersom det f.eks. er opphold mellom 2 tiltaksdeltagelsesperioder. Avslag og delvis avslag vil ikke være med her. */
    val vedtaksperioder: List<Periode> by lazy { tidslinje.perioder }

    /** Nåtilstand. De periodene som gir rett til tiltakspenger. Vil kunne være hull. */
    val innvilgelsesperioder: List<Periode> by lazy {
        tidslinje.filter { it.verdi?.vedtaksType == Vedtakstype.INNVILGELSE }.map { it.periode }
    }

    val innvilgetTidslinje: Periodisering<Rammevedtak?> by lazy {
        tidslinje.perioderMedVerdi.filter { it.verdi == null || it.verdi?.vedtaksType == Vedtakstype.INNVILGELSE }.map {
            PeriodeMedVerdi(
                periode = it.periode,
                verdi = it.verdi,
            )
        }.let { Periodisering(it) }
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
     * Innvilget->Stans eksempel:
     * Vedtak 1: 01.01.2021 - 04.01.2021 (oppfylt/innvilget)
     * Vedtak 2: 03.01.2021 - 04.01.2021 (ikke oppfylt/stans)
     * Utfallsperioder: 01.01.2021 - 02.01.2021 Oppfylt (fra vedtak 1)
     *                  03.01.2021 - 04.01.2021 Ikke Oppfylt (fra vedtak 2)
     */
    @Suppress("UNCHECKED_CAST")
    val utfallsperioder: Periodisering<Utfallsperiode?> by lazy {
        tidslinje.perioderMedVerdi.flatMap {
            if (it.verdi == null) {
                listOf(PeriodeMedVerdi<Utfallsperiode?>(null, it.periode))
            } else {
                it.verdi!!.utfallsperioder!!.krymp(it.periode).perioderMedVerdi as List<PeriodeMedVerdi<Utfallsperiode?>>
            }
        }.let { Periodisering(it) }
    }

    fun utfallForPeriode(periode: Periode): Periodisering<Utfallsperiode?> {
        return utfallsperioder.overlapperMed(periode).utvid(Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER, periode)
    }

    // Denne fungerer bare for førstegangsvedtak der man har valgte tiltaksdeltakelser
    @Suppress("UNCHECKED_CAST")
    val valgteTiltaksdeltakelser: Periodisering<Tiltaksdeltagelse?> by lazy {
        innvilgetTidslinje.perioderMedVerdi
            .flatMap {
                if (it.verdi == null) {
                    listOf(PeriodeMedVerdi<Tiltaksdeltagelse?>(null, it.periode))
                } else {
                    it.verdi!!.behandling.valgteTiltaksdeltakelser!!.periodisering.krymp(it.periode).perioderMedVerdi as List<PeriodeMedVerdi<Tiltaksdeltagelse?>>
                }
            }
            .let {
                Periodisering(it)
            }
    }

    fun valgteTiltaksdeltakelserForPeriode(periode: Periode): Periodisering<Tiltaksdeltagelse?> {
        return valgteTiltaksdeltakelser.overlapperMed(periode)
    }

    /** Tidslinje for antall barn. Første og siste periode vil være 1 eller flere. Kan inneholde hull med 0 barn. */
    @Suppress("UNCHECKED_CAST")
    val barnetilleggsperioder: Periodisering<AntallBarn?> by lazy {
        tidslinje.perioderMedVerdi.flatMap {
            if (it.verdi == null) {
                listOf(PeriodeMedVerdi<AntallBarn?>(null, it.periode))
            } else {
                it.verdi?.barnetillegg?.periodisering?.krymp(it.periode)?.perioderMedVerdi?.let {
                    it as List<PeriodeMedVerdi<AntallBarn?>>
                } ?: listOf(PeriodeMedVerdi<AntallBarn?>(null, it.periode))
            }
        }.let { Periodisering(it) }
    }

    val tiltakstypeperioder: Periodisering<TiltakstypeSomGirRett?> by lazy {
        valgteTiltaksdeltakelser.map { it?.typeKode }
    }

    fun leggTilFørstegangsVedtak(vedtak: Rammevedtak): Vedtaksliste = copy(value = this.value.plus(vedtak))

    fun hentTiltaksdataForPeriode(periode: Periode): List<Tiltaksdeltagelse?> {
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
