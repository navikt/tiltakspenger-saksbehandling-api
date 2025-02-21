package no.nav.tiltakspenger.saksbehandling.domene.vedtak

import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.saksbehandling.domene.stønadsdager.Stønadsdager
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkårssett
import java.time.LocalDate

data class Vedtaksliste(
    val value: List<Rammevedtak>,
) : List<Vedtak> by value {
    constructor(value: Rammevedtak) : this(listOf(value))

    val tidslinje: Periodisering<Rammevedtak> by lazy { value.toTidslinje() }

    val førstegangsvedtak: Rammevedtak? by lazy { value.singleOrNullOrThrow { it.erFørstegangsvedtak } }

    /** Nåtilstand. Dette er sakens totale vedtaksperiode. Vær veldig obs når du bruker denne, fordi den sier ikke noe om antall perioder, om de gir rett eller ikke. */
    val vedtaksperiode: Periode? by lazy { tidslinje.ifEmpty { null }?.totalePeriode }

    /** Nåtilstand. Sakens totale vedtaksperioder. Vil kunne ha hull dersom det f.eks. er opphold mellom 2 tiltaksdeltagelsesperioder. Avslag og delvis avslag vil ikke være med her. */
    val vedtaksperioder: List<Periode> by lazy { tidslinje.perioder }

    /** Nåtilstand. De periodene som gir rett til tiltakspenger. Vil kunne være hull. */
    val innvilgelsesperioder: List<Periode> by lazy {
        tidslinje.filter { it.verdi.vedtaksType == Vedtakstype.INNVILGELSE }.map { it.periode }
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

    // TODO pre-revurdering-av-revurdering jah: Det gir egentlig ikke mening og ha periodiserte vilkårssett. Her bør man heller slå sammen vilkårssettene. Men det krever at hvert vilkår kan periodiseres.
    // TODO John + Anders: Vi får muligens en tilsvarende på Saksopplysninger? Vi må ha denne for bakoverkompatibilitet.
    val vilkårssett: Periodisering<Vilkårssett> by lazy {
        tidslinje.perioderMedVerdi.map {
            PeriodeMedVerdi(
                it.verdi.krymp(it.periode).behandling.vilkårssett!!,
                it.periode,
            )
        }.let { Periodisering(it) }
    }

    /**
     * Perioden må være innenfor tidslinjen
     *
     * **/
    fun tidslinjeForPeriode(periode: Periode): Periodisering<Rammevedtak> {
        return tidslinje.krymp(periode)
    }

    fun krympVilkårssett(nyPeriode: Periode): Periodisering<Vilkårssett> {
        if (nyPeriode == vedtaksperiode) return vilkårssett
        return vilkårssett.krymp(nyPeriode).map { it.krymp(nyPeriode) }
    }

    /**
     * Innvilget->Stans eksempel:
     * Vedtak 1: 01.01.2021 - 04.01.2021 (oppfylt/innvilget)
     * Vedtak 2: 03.01.2021 - 04.01.2021 (ikke oppfylt/stans)
     * Utfallsperioder: 01.01.2021 - 02.01.2021 Oppfylt (fra vedtak 1)
     *                  03.01.2021 - 04.01.2021 Ikke Oppfylt (fra vedtak 2)
     */
    val utfallsperioder: Periodisering<AvklartUtfallForPeriode> by lazy {
        tidslinje.perioderMedVerdi.flatMap { pmvVedtak ->
            pmvVedtak.verdi.utfallsperioder.perioderMedVerdi.mapNotNull {
                it.periode.overlappendePeriode(pmvVedtak.periode)?.let { overlappendePeriode ->
                    PeriodeMedVerdi(
                        periode = overlappendePeriode,
                        verdi = it.verdi,
                    )
                }
            }
        }.let { Periodisering(it) }
    }

    fun leggTilFørstegangsVedtak(vedtak: Rammevedtak): Vedtaksliste {
        when (vedtak.vedtaksType) {
            Vedtakstype.INNVILGELSE -> require(this.isEmpty()) { "Vedtaksliste must not be empty." }
            Vedtakstype.STANS -> Unit
        }
        return copy(value = listOf(vedtak))
    }

    fun krympUtfallsperioder(nyPeriode: Periode): Periodisering<AvklartUtfallForPeriode> {
        if (nyPeriode == vedtaksperiode) return utfallsperioder
        return utfallsperioder.krymp(nyPeriode)
    }

    // TODO pre-revurdering-av-revurdering jah: Det gir egentlig ikke mening og ha periodiserte stønadsdager. Her bør man heller slå sammen stønadsdagene. Men det krever at Stønadsdager er periodisert på innsiden.
    // TODO John + Anders: Vi får muligens en tilsvarende på Saksopplysninger? Vi må ha denne for bakoverkompatibilitet.
    val stønadsdager: Periodisering<Stønadsdager> by lazy {
        tidslinje.perioderMedVerdi.map {
            PeriodeMedVerdi(
                it.verdi.krymp(it.periode).behandling.stønadsdager!!,
                it.periode,
            )
        }.let { Periodisering(it) }
    }

    fun krympStønadsdager(nyPeriode: Periode): Periodisering<Stønadsdager> {
        if (nyPeriode == vedtaksperiode) return stønadsdager
        return stønadsdager.krymp(nyPeriode).map { it.krymp(nyPeriode) }
    }

    /**
     * @return null dersom vi ikke har noen rammevedtak, eller vi ikke har vedtak som overlapper med [periode].
     * @throws NoSuchElementException eller [IllegalArgumentException] dersom mer enn 1 tiltak er gjeldende for perioden.
     */
    fun hentTiltaksdataForPeriode(periode: Periode): TiltaksdataForJournalføring? {
        if (value.isEmpty()) return null
        val tidslinje = Periodisering(
            tidslinje.perioderMedVerdi.map {
                PeriodeMedVerdi(
                    it.verdi.krymp(it.periode).behandling,
                    it.periode,
                )
            },
        )
        val overlappendePeriode = periode.overlappendePeriode(tidslinje.totalePeriode) ?: return null
        return tidslinje.krymp(overlappendePeriode).map {
            TiltaksdataForJournalføring(
                tiltaksnavn = it.tiltaksnavn,
                eksternGjennomføringId = it.gjennomføringId,
                eksternDeltagelseId = it.tiltaksid,
            )
        }.single().verdi
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
