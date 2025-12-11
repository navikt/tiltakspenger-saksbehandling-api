package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.overlappendePerioder
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.finnAntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.LocalDate

data class Rammevedtaksliste(
    val verdi: List<Rammevedtak>,
) : List<Rammevedtak> by verdi {
    constructor(value: Rammevedtak) : this(listOf(value))

    val fnr = verdi.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow()
    val sakId = verdi.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow()
    val saksnummer = verdi.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()

    /** Et førstegangsvedtak defineres som den første søknadsbehandlingen som førte til innvilgelse. */
    val harFørstegangsvedtak: Boolean by lazy {
        verdi.any { it.behandling is Søknadsbehandling && it.resultat is BehandlingResultat.Innvilgelse }
    }

    val utbetalinger: List<VedtattUtbetaling> by lazy {
        verdi.mapNotNull { it.utbetaling }
    }

    val avslagsvedtak: List<Rammevedtak> by lazy {
        verdi.filter { it.resultat is SøknadsbehandlingResultat.Avslag }
    }

    val vedtakUtenAvslag: List<Rammevedtak> by lazy {
        verdi.filter {
            when (it.resultat) {
                is BehandlingResultat.Innvilgelse,
                is RevurderingResultat.Stans,
                -> true

                is SøknadsbehandlingResultat.Avslag -> false
            }
        }
    }

    /**
     * Vedtakstidslinjen tar kun for seg vedtak som kan påvirke en utbetaling ([BehandlingResultat.Innvilgelse] og [RevurderingResultat.Stans]) og skal aldri inkludere [SøknadsbehandlingResultat.Avslag].
     * Dersom man ønsker å opphøre en tidligere innvilget periode, skal man bruke stans, aldri [SøknadsbehandlingResultat.Avslag].
     *
     * Et tenkt eksempel: Bruker søker på 2 tiltak, som har lik periode. Det første gir rett til tiltakspenger, det andre ikke.
     * Dersom man innvilger tiltak 1 og avslår tiltak 2 i den rekkefølgen, hvis man inkluderte avslag i tidslinjen, ville avslaget opphørt innvilgelsen; som den absolutt ikke må gjøre i dette tilfellet.
     * Men det er fremdeles riktig og gi bruker avslag på tiltak 2.
     *
     * Vil være [no.nav.tiltakspenger.libs.periodisering.TomPeriodisering] før vi har noen vedtak.
     * [no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering] ved etter første innvilgelse.
     * Og [no.nav.tiltakspenger.libs.periodisering.IkkesammenhengendePeriodisering] dersom vi får en ny innvilgelse som ikke overlapper med den første.
     */
    val tidslinje: Periodisering<Rammevedtak> by lazy { vedtakUtenAvslag.toTidslinje() }

    /** Nåtilstand. Sakens totale vedtaksperioder. Vil kunne ha hull dersom det f.eks. er opphold mellom 2 tiltaksdeltakelsesperioder. Avslag og delvis avslag vil ikke være med her. */
    val vedtaksperioder: List<Periode> by lazy { tidslinje.perioder }

    /** Nåtilstand. De periodene som gir rett til tiltakspenger. Kan ha hull. */
    val innvilgelsesperioder: List<Periode> by lazy { innvilgetTidslinje.perioder }

    val innvilgetTidslinje: Periodisering<Rammevedtak> by lazy {
        tidslinje.filter {
            it.verdi.resultat is BehandlingResultat.Innvilgelse
        }.perioderMedVerdi.mapNotNull { (vedtak, gjeldendePeriode) ->
            gjeldendePeriode.overlappendePeriode(vedtak.innvilgelsesperiode!!)?.let { overlappendePeriode ->
                PeriodeMedVerdi(
                    vedtak,
                    overlappendePeriode,
                )
            }
        }.tilPeriodisering()
    }

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den første innvilget dagen. */
    val førsteDagSomGirRett: LocalDate? by lazy { innvilgelsesperioder.minOfOrNull { it.fraOgMed } }

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val sisteDagSomGirRett: LocalDate? by lazy { innvilgelsesperioder.maxOfOrNull { it.tilOgMed } }

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

    val valgteTiltaksdeltakelser: Periodisering<Tiltaksdeltakelse> by lazy {
        innvilgetTidslinje.flatMapPeriodisering { it.verdi.behandling.valgteTiltaksdeltakelser!!.periodisering }
    }

    fun valgteTiltaksdeltakelserForPeriode(periode: Periode): Periodisering<Tiltaksdeltakelse> {
        return valgteTiltaksdeltakelser.overlappendePeriode(periode)
    }

    fun harInnvilgetTiltakspengerPåDato(dato: LocalDate): Boolean {
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

    /**
     * Legger til et rammevedtak i vedtaklisten og oppdaterer omgjortAvRammevedtak per vedtak
     */
    fun leggTil(vedtak: Rammevedtak): Rammevedtaksliste {
        if (vedtak.resultat is SøknadsbehandlingResultat.Avslag) {
            // Avslag omgjør aldri noe
            return copy(verdi = this.verdi.plus(vedtak))
        }
        val vedtaksliste = this.verdi
        val oppdatertVedtaksliste = vedtaksliste.map {
            it.oppdaterOmgjortAvRammevedtak(vedtak)
        }
        return copy(verdi = oppdatertVedtaksliste.plus(vedtak))
    }

    fun hentRammevedtakForId(rammevedtakId: VedtakId): Rammevedtak {
        return verdi.single { it.id == rammevedtakId }
    }

    fun finnRammevedtakForBehandling(id: BehandlingId): Rammevedtak? {
        return this.singleOrNullOrThrow { vedtak -> vedtak.behandling.id == id }
    }

    /**
     * Tenkt kalt under behandlingen for å avgjøre hvilke rammevedtak som vil bli omgjort.
     * Obs: Merk at en annen behandling kan ha omgjort det samme/de samme vedtakene etter at denne metoden er kalt, men før denne behandlingen iverksettes.
     * @param virkningsperiode vurderingsperioden/vedtaksperioden. Kan være en ren innvilgelse, et rent opphør eller en blanding.
     */
    fun finnVedtakSomOmgjøres(
        virkningsperiode: Periode,
    ): OmgjørRammevedtak {
        return OmgjørRammevedtak(
            this.flatMap {
                it.finnPerioderSomOmgjøres(virkningsperiode)
            }.sortedBy { it.periode.fraOgMed },
        )
    }

    init {
        verdi.map { it.id }.let {
            require(it.size == it.distinct().size) { "Vedtakene må ha unike IDer men var: $it" }
        }
        verdi.map { it.behandling.id }.let {
            require(it.size == it.distinct().size) { "Behandlingene må ha unike IDer men var: $it" }
        }
        verdi.zipWithNext().forEach {
            require(it.first.opprettet.isBefore(it.second.opprettet)) { "Vedtakene må være sortert på opprettet-tidspunkt, men var: ${verdi.map { it.opprettet }}" }
        }
        require(tidslinjeFraGjeldendeVedtak() == this.tidslinje) {
            "Ugyldig gjeldende tidslinje. For vedtaksliste med vedtak ${this.map { it.id }}, forventet gjeldende tidslinje: ${this.tidslinje},"
        }
        validerOmgjøringer()
    }

    companion object {
        fun empty() = Rammevedtaksliste(emptyList())
    }

    private fun validerOmgjøringer() {
        vedtakUtenAvslag.forEachIndexed { index, vedtak ->
            val tidslinjeFørDetteVedtaket = vedtakUtenAvslag.take(index).toTidslinje()
            val overlapp = tidslinjeFørDetteVedtaket.overlappendePeriode(vedtak.periode)
            val omgjør = OmgjørRammevedtak(
                overlapp.perioderMedVerdi.map {
                    Omgjøringsperiode(
                        rammevedtakId = it.verdi.id,
                        periode = it.periode,
                        omgjøringsgrad = if (it.verdi.periode == it.periode) Omgjøringsgrad.HELT else Omgjøringsgrad.DELVIS,
                    )
                },
            )
            require(vedtak.omgjørRammevedtak == omgjør) {
                "Ugyldig [omgjørRammevedtak] på vedtak ${vedtak.id}. Forventet omgjøringsdata: $omgjør, men fant: ${vedtak.omgjørRammevedtak}"
            }

            val alleSenereVedtak = vedtakUtenAvslag.drop(index + 1)

            val omgjortAvRammevedtak = alleSenereVedtak.fold(OmgjortAvRammevedtak.empty) { acc, senereVedtak ->
                val perioderSomOmgjøres = senereVedtak.periode
                    .trekkFra(acc.perioder)
                    .overlappendePerioder(listOf(vedtak.periode))

                perioderSomOmgjøres.map {
                    Omgjøringsperiode(
                        rammevedtakId = senereVedtak.id,
                        periode = it,
                        omgjøringsgrad = if (vedtak.periode == it) Omgjøringsgrad.HELT else Omgjøringsgrad.DELVIS,
                    )
                }.let { acc.leggTil(it) }
            }

            require(vedtak.omgjortAvRammevedtak == omgjortAvRammevedtak) {
                "Ugyldig [omgjortAvRammevedtak] på vedtak ${vedtak.id}. Forventet: $omgjortAvRammevedtak, men fant: ${vedtak.omgjortAvRammevedtak}"
            }
        }
    }

    private fun tidslinjeFraGjeldendeVedtak(): Periodisering<Rammevedtak> {
        return vedtakUtenAvslag.flatMap { vedtak ->
            vedtak.gjeldendePerioder.map {
                PeriodeMedVerdi(vedtak, it)
            }
        }.sortedBy { it.periode.fraOgMed }.tilPeriodisering()
    }

    fun hentVedtakForBehandlingId(behandlingId: BehandlingId): Rammevedtak {
        return this.single { it.behandling.id == behandlingId }
    }
}
