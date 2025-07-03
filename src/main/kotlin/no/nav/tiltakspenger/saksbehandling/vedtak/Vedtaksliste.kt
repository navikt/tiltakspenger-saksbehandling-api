package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.finnAntallDagerForMeldeperiode
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

    /**
     * Tar med [Vedtakstype.INNVILGELSE] og [Vedtakstype.STANS], men ignorerer [Vedtakstype.AVSLAG]
     *
     * Vil være [no.nav.tiltakspenger.libs.periodisering.TomPeriodisering] før vi har noen vedtak.
     * [no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering] ved etter første innvilgelse.
     * Og [no.nav.tiltakspenger.libs.periodisering.IkkesammenhengendePeriodisering] dersom vi får en ny innvilgelse som ikke overlapper med den første.
     */
    val tidslinje: Periodisering<Rammevedtak> by lazy {
        value.filter {
            when (it.vedtakstype) {
                Vedtakstype.INNVILGELSE,
                Vedtakstype.STANS,
                -> true

                Vedtakstype.AVSLAG -> false
            }
        }.toTidslinje()
    }

    /**
     * En tidslinje som inneholder alle vedtak
     */
    val vedtakshistorikk: Periodisering<Rammevedtak> by lazy { value.toTidslinje() }

    val erInnvilgelseSammenhengende by lazy { innvilgetTidslinje.erSammenhengende }

    /** Nåtilstand. Sakens totale vedtaksperioder. Vil kunne ha hull dersom det f.eks. er opphold mellom 2 tiltaksdeltagelsesperioder. Avslag og delvis avslag vil ikke være med her. */
    val vedtaksperioder: List<Periode> by lazy { tidslinje.perioder }

    /** Nåtilstand. De periodene som gir rett til tiltakspenger. Kan ha hull. */
    val innvilgelsesperioder: List<Periode> by lazy {
        innvilgetTidslinje.perioder
    }

    val innvilgetTidslinje: Periodisering<Rammevedtak> by lazy {
        tidslinje.filter { it.verdi.vedtakstype == Vedtakstype.INNVILGELSE }
    }

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
    val utfallsperioder: Periodisering<Utfallsperiode> by lazy {
        tidslinje.flatMapPeriodisering { it.verdi.utfallsperioder }
    }

    /** Dersom vi ikke har en verdi for deler av [periode], fyller vi den ut med [Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER] */
    fun utfallForPeriode(periode: Periode): Periodisering<Utfallsperiode> {
        return utfallsperioder.overlappendePeriode(periode).utvid(Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER, periode)
    }

    /**
     * Tar utgangspunkt i [innvilgetTidslinje]
     */
    val antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode> by lazy {
        innvilgetTidslinje.flatMapPeriodisering { it.verdi.behandling.antallDagerPerMeldeperiode!! }
    }

    fun antallDagerForMeldeperiode(periode: Periode): AntallDagerForMeldeperiode? {
        return innvilgetTidslinje
            .overlappendePeriode(periode)
            .mapNotNull { it.verdi.behandling.antallDagerPerMeldeperiode?.finnAntallDagerForMeldeperiode(periode) }
            .maxOfOrNull { it }
    }

    fun vedtakForPeriode(periode: Periode): Periodisering<VedtakId> {
        return tidslinje.map { verdi, _ -> verdi.id }.krymp(periode)
    }

    val valgteTiltaksdeltakelser: Periodisering<Tiltaksdeltagelse> by lazy {
        innvilgetTidslinje.flatMapPeriodisering { it.verdi.behandling.valgteTiltaksdeltakelser!!.periodisering }
    }

    fun valgteTiltaksdeltakelserForPeriode(periode: Periode): Periodisering<Tiltaksdeltagelse> {
        return valgteTiltaksdeltakelser.overlappendePeriode(periode)
    }

    fun harInnvilgetTiltakspengerPaDato(dato: LocalDate): Boolean {
        return innvilgelsesperioder.any { it.inneholder(dato) }
    }

    fun harInnvilgetTiltakspengerEtterDato(dato: LocalDate): Boolean {
        return innvilgelsesperioder.any { it.starterEtter(dato) }
    }

    /**
     * Tidslinje for antall barn. Første og siste periode vil være 1 eller flere. Kan inneholde hull med 0 barn.
     * Tar utgangspunkt i [innvilgetTidslinje]
     */
    val barnetilleggsperioder: Periodisering<AntallBarn> by lazy {
        innvilgetTidslinje.flatMapPeriodisering {
            it.verdi.barnetillegg?.periodisering ?: Periodisering(
                AntallBarn(0),
                it.periode,
            )
        }
    }

    val tiltakstypeperioder: Periodisering<TiltakstypeSomGirRett> by lazy {
        valgteTiltaksdeltakelser.mapVerdi { verdi, _ -> verdi.typeKode }
    }

    fun leggTilVedtak(vedtak: Rammevedtak): Vedtaksliste = copy(value = this.value.plus(vedtak))

    fun hentTiltaksdataForPeriode(periode: Periode): List<Tiltaksdeltagelse> {
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
