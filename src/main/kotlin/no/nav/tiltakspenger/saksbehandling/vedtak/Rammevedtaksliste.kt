package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
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
    val tidslinje: Periodisering<Rammevedtak> by lazy {
        verdi.filter {
            // Fjerner alle vedtak som er omgjort av et annet vedtak.
            it.omgjortAvRammevedtakId == null
        }.filter {
            when (it.resultat) {
                is BehandlingResultat.Innvilgelse,
                is RevurderingResultat.Stans,
                -> true

                is SøknadsbehandlingResultat.Avslag -> false
            }
        }.toTidslinje()
    }

    val erInnvilgelseSammenhengende by lazy { innvilgetTidslinje.erSammenhengende }

    /** Nåtilstand. Sakens totale vedtaksperioder. Vil kunne ha hull dersom det f.eks. er opphold mellom 2 tiltaksdeltagelsesperioder. Avslag og delvis avslag vil ikke være med her. */
    val vedtaksperioder: List<Periode> by lazy { tidslinje.perioder }

    /** Nåtilstand. De periodene som gir rett til tiltakspenger. Kan ha hull. */
    val innvilgelsesperioder: List<Periode> by lazy {
        innvilgetTidslinje.perioder
    }

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
    val førsteDagSomGirRett: LocalDate? by lazy {
        innvilgelsesperioder.minOfOrNull { it.fraOgMed }
    }

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val sisteDagSomGirRett: LocalDate? by lazy {
        innvilgelsesperioder.maxOfOrNull { it.tilOgMed }
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

    fun leggTil(vedtak: Rammevedtak): Rammevedtaksliste = copy(verdi = this.verdi.plus(vedtak))

    fun hentRammevedtakForId(rammevedtakId: VedtakId): Rammevedtak {
        return verdi.single { it.id == rammevedtakId }
    }

    fun finnRammevedtakForBehandling(id: BehandlingId): Rammevedtak? {
        return this.singleOrNullOrThrow { vedtak -> vedtak.behandling.id == id }
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
    }

    companion object {
        fun empty() = Rammevedtaksliste(emptyList())
    }
}
