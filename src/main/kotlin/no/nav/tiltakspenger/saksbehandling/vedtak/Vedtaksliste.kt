package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinjeMedHull
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
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
        innvilgelsesperioder.ifEmpty { null }
            ?.let { perioder ->
                Periode(perioder.minOf { it.fraOgMed }, perioder.maxOf { it.tilOgMed })
            }
    }

    /** Nåtilstand. Sakens totale vedtaksperioder. Vil kunne ha hull dersom det f.eks. er opphold mellom 2 tiltaksdeltagelsesperioder. Avslag og delvis avslag vil ikke være med her. */
    val vedtaksperioder: List<Periode> by lazy { tidslinje.perioder }

    /** Nåtilstand. De periodene som gir rett til tiltakspenger. Vil kunne være hull. */
    val innvilgelsesperioder: List<Periode> by lazy {
        tidslinje.filter { it.verdi?.vedtaksType == Vedtakstype.INNVILGELSE }.map { it.periode }
    }

    val innvilgetTidslinje: Periodisering<Rammevedtak?> by lazy {
        tidslinje.filter { verdi, _ -> verdi?.vedtaksType == Vedtakstype.INNVILGELSE }
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
        tidslinje.flatMap {
            it.verdi?.utfallsperioder?.krymp(it.periode)?.perioderMedVerdi as? List<PeriodeMedVerdi<Utfallsperiode?>>
                ?: listOf(PeriodeMedVerdi(null, it.periode))
        }
    }

    fun utfallForPeriode(periode: Periode): Periodisering<Utfallsperiode?> {
        return utfallsperioder.overlapperMed(periode).utvid(Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER, periode)
    }

    fun vedtakForPeriode(periode: Periode): Periodisering<VedtakId?> = tidslinje
        .map { verdi, _ -> verdi?.id }
        .nyPeriode(periode, null)

    // Denne fungerer bare for førstegangsvedtak der man har valgte tiltaksdeltakelser
    @Suppress("UNCHECKED_CAST")
    val valgteTiltaksdeltakelser: Periodisering<Tiltaksdeltagelse?> by lazy {
        innvilgetTidslinje.flatMap { (verdi, periode) ->
            if (verdi == null) {
                listOf(PeriodeMedVerdi(null, periode))
            } else {
                // TODO John og Anders: Fix for revurdering
                require(verdi.behandling is Søknadsbehandling)
                verdi.behandling.valgteTiltaksdeltakelser!!.periodisering.krymp(periode).perioderMedVerdi as List<PeriodeMedVerdi<Tiltaksdeltagelse?>>
            }
        }
    }

    fun valgteTiltaksdeltakelserForPeriode(periode: Periode): Periodisering<Tiltaksdeltagelse?> {
        return valgteTiltaksdeltakelser.overlapperMed(periode)
    }

    fun harInnvilgetTiltakspengerPaDato(dato: LocalDate): Boolean {
        return innvilgelsesperioder.any { it.inneholder(dato) }
    }

    fun harInnvilgetTiltakspengerEtterDato(dato: LocalDate): Boolean {
        return innvilgelsesperioder.any { it.starterEtter(dato) }
    }

    /** Tidslinje for antall barn. Første og siste periode vil være 1 eller flere. Kan inneholde hull med 0 barn. */
    @Suppress("UNCHECKED_CAST")
    val barnetilleggsperioder: Periodisering<AntallBarn?> by lazy {
        tidslinje.flatMap {
            it.verdi?.barnetillegg?.periodisering?.krymp(it.periode)?.perioderMedVerdi?.let {
                it as List<PeriodeMedVerdi<AntallBarn?>>
            } ?: listOf(PeriodeMedVerdi(null, it.periode))
        }
    }

    val tiltakstypeperioder: Periodisering<TiltakstypeSomGirRett?> by lazy {
        valgteTiltaksdeltakelser.mapVerdi { verdi, _ -> verdi?.typeKode }
    }

    fun leggTilFørstegangsVedtak(vedtak: Rammevedtak): Vedtaksliste = copy(value = this.value.plus(vedtak))

    fun hentTiltaksdataForPeriode(periode: Periode): List<Tiltaksdeltagelse?> {
        return valgteTiltaksdeltakelserForPeriode(periode).verdier
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
