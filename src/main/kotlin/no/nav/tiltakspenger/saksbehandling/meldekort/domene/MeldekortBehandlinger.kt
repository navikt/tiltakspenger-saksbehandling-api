package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeSendeMeldekortTilBeslutning.InnsendteDagerMåMatcheMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling.MeldekortBehandlet
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status
import java.time.Clock
import java.time.LocalDate

/**
 * Består av ingen, én eller flere [MeldekortBeregning].
 * Vil være tom fram til første innvilgede førstegangsbehandling.
 * Kun den siste vil kunne være under behandling (åpen).
 */
data class MeldekortBehandlinger(
    val verdi: List<MeldekortBehandling>,
) : List<MeldekortBehandling> by verdi {

    val log = KotlinLogging.logger { }

    val periode: Periode by lazy { Periode(verdi.first().fraOgMed, verdi.last().tilOgMed) }
    val sakId: SakId by lazy { verdi.first().sakId }

    private val behandledeMeldekort: List<MeldekortBehandlet> by lazy { verdi.filterIsInstance<MeldekortBehandlet>() }

    val behandledeMeldekortPerKjede: Map<MeldeperiodeKjedeId, List<MeldekortBehandlet>> by lazy {
        behandledeMeldekort
            .sortedBy { it.opprettet }
            .groupBy { it.kjedeId }
    }

    val sisteBehandledeMeldekortPerKjede: List<MeldekortBehandlet> by lazy {
        behandledeMeldekortPerKjede.values.map { it.last() }
    }

    /** Under behandling er ikke-avsluttede meldekortbehandlinger som ikke er til beslutning. */
    val meldekortUnderBehandling: MeldekortUnderBehandling? by lazy {
        verdi.filterIsInstance<MeldekortUnderBehandling>().singleOrNullOrThrow()
    }

    private val meldekortUnderBeslutning: MeldekortBehandlet? by lazy {
        behandledeMeldekort.filter { it.status == MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING }.singleOrNullOrThrow()
    }

    val godkjenteMeldekort: List<MeldekortBehandlet> by lazy { behandledeMeldekort.filter { it.status == MeldekortBehandlingStatus.GODKJENT } }

    val sisteGodkjenteMeldekort: MeldekortBehandlet? by lazy { godkjenteMeldekort.lastOrNull() }

    @Suppress("unused")
    val sisteGodkjenteMeldekortDag: LocalDate? by lazy { sisteGodkjenteMeldekort?.tilOgMed }

    /** Merk at denne går helt tilbake til siste godkjente, utbetalte dag. Dette er ikke nødvendigvis den siste godkjente meldeperioden. */
    val sisteUtbetalteMeldekortDag: LocalDate? by lazy {
        godkjenteMeldekort.flatMap { it.beregning.dager }.lastOrNull { it.beløp > 0 }?.dato
    }

    /**
     *  Vil kun returnere hele meldekortperioder som er utfylt. Dersom siste meldekortperiode er delvis utfylt, vil ikke disse komme med.
     *
     *  Obs!! Med korrigeringer kan vi ikke stole på beregninger fra disse dagene, ettersom korrigering kan påvirke dager etter det korrigerte meldekortet
     *  TODO: Bør løses når vi splitter meldekort-utfylling og meldekort-beregning
     *  */
    val utfylteDager: List<MeldeperiodeBeregningDag.Utfylt> by lazy { sisteBehandledeMeldekortPerKjede.flatMap { it.beregning.dager } }

    /** Meldekort som er under behandling eller venter på beslutning */
    val åpenMeldekortBehandling: MeldekortBehandling? by lazy { meldekortUnderBehandling ?: meldekortUnderBeslutning }
    val finnesÅpenMeldekortBehandling: Boolean by lazy { åpenMeldekortBehandling != null }

    /**
     * Tar alle behandlinger frem til behandling vi ønsker å sammenligne med. Alle beregningene for behandlingene
     * legges flatt og grupperes på meldeperiode. Deretter hentes den nyeste beregningen i hver gruppering.
     *
     * TODO Henrik endrer dette når han forstår eksempelet bedre
     * John: Et eksempel fordi dette er en veldig tilspisset løsning:
     *
     *           U1-B1                    U2-B1
     * |-----------------------|-----------------------|
     *           U3-B1                    U3-B2
     * |-----------------------|-----------------------|
     *           U4-B1                    U4-B2
     * |-----------------------|-----------------------| (nåtilstand)
     *
     * U4 er nåtilstand, U1, U2, U3 er forrige utbetalinger
     * Liste med U1, U2, U3 og U4
     * Godkjente meldekort, hente behandlingen per utbetaling
     * - takeWhile() fjerner U4
     * - plukker ut beregningene fra behandlingene og står igjen med U1-B1, U2-B1, U3-B1 og U3-B2
     * - grupperer på meldeperiode, gir to grupperinger. (U1-B1 og U3-B1) og (U2-B1 og U3-B2)
     * - maxBy henter den nyeste innad i hver gruppering
     *  står igjen med beregningstidslinja U3-B1 og U3-B2
     */
    fun beregningFremTilBehandling(
        periode: Periode,
        behandlingId: MeldekortId,
    ): MeldekortBeregning.MeldeperiodeBeregnet? {
        return godkjenteMeldekort
            .takeWhile { behandletMeldekort -> behandletMeldekort.id != behandlingId }
            .flatMap { it.beregning.beregninger }
            .groupBy { it.periode }
            .map { it.value.maxBy { meldeperiodeBeregnet -> meldeperiodeBeregnet.opprettet } }
            .singleOrNull { it.periode == periode }
    }

    /**
     * @throws NullPointerException Dersom det ikke er noen meldekort-behandling som kan sendes til beslutter. Eller siste meldekort ikke er i tilstanden 'under behandling'.
     * @throws IllegalArgumentException Dersom innsendt meldekortid ikke samsvarer med siste meldekortperiode.
     */
    fun sendTilBeslutter(
        kommando: SendMeldekortTilBeslutningKommando,
        barnetilleggsPerioder: Periodisering<AntallBarn?>,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortTilBeslutning, Pair<MeldekortBehandlinger, MeldekortBehandlet>> {
        val meldekortId = kommando.meldekortId
        val meldekort = meldekortUnderBehandling

        requireNotNull(meldekort) {
            "Fant ingen meldekort under behandling på saken"
        }

        require(meldekort.id == meldekortId) {
            "MeldekortId i kommando ($meldekortId) samsvarer ikke med meldekortet som er under behandling (${meldekort.id})"
        }

        if (kommando.periode != meldekort.periode) {
            return InnsendteDagerMåMatcheMeldeperiode.left()
        }

        kommando.dager.dager.zip(meldekort.beregning.dager).forEach { (dagA, dagB) ->
            if (dagA.status == Status.SPERRET && dagB !is MeldeperiodeBeregningDag.Utfylt.Sperret) {
                log.error { "Kan ikke endre dag til sperret. Nåværende tilstand: $utfylteDager. Innsendte dager: ${kommando.dager}" }
                return KanIkkeSendeMeldekortTilBeslutning.KanIkkeEndreDagTilSperret.left()
            }
            if (dagA.status != Status.SPERRET && dagB is MeldeperiodeBeregningDag.Utfylt.Sperret) {
                log.error { "Kan ikke endre dag fra sperret. Nåværende tilstand: $utfylteDager. Innsendte dager: ${kommando.dager}" }
                return KanIkkeSendeMeldekortTilBeslutning.KanIkkeEndreDagFraSperret.left()
            }
        }

        val beregnedeDager: NonEmptyList<MeldekortBeregning.MeldeperiodeBeregnet> = kommando.beregn(
            eksisterendeMeldekortBehandlinger = this,
            barnetilleggsPerioder = barnetilleggsPerioder,
            tiltakstypePerioder = tiltakstypePerioder,
        )

        val beregning = MeldekortBeregning.UtfyltMeldeperiode(
            sakId = sakId,
            maksDagerMedTiltakspengerForPeriode = meldekortUnderBehandling!!.beregning.maksDagerMedTiltakspengerForPeriode,
            beregninger = beregnedeDager,
        )

        return meldekort.sendTilBeslutter(
            beregning,
            kommando.meldekortbehandlingBegrunnelse,
            kommando.saksbehandler,
            clock,
        )
            .map {
                Pair(
                    oppdaterMeldekortbehandling(it),
                    it,
                )
            }
    }

    fun hentMeldekortBehandling(meldekortId: MeldekortId): MeldekortBehandling? {
        return verdi.singleOrNullOrThrow { it.id == meldekortId }
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingerForMeldeperiode(id: MeldeperiodeId): List<MeldekortBehandling> {
        return this.filter { it.meldeperiode.id == id }
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingerForKjede(kjedeId: MeldeperiodeKjedeId): List<MeldekortBehandling> {
        return verdi.filter { it.kjedeId == kjedeId }
    }

    fun hentSisteMeldekortBehandlingForKjede(kjedeId: MeldeperiodeKjedeId): MeldekortBehandling? {
        return hentMeldekortBehandlingerForKjede(kjedeId).maxByOrNull { it.opprettet }
    }

    /**
     * Løper igjennom alle ikke-avsluttede meldekortbehandlinger (også de som er sendt til beslutter), setter tilstanden til under behandling, oppdaterer meldeperioden og resetter utfyllinga.
     */
    fun oppdaterMedNyeKjeder(
        oppdaterteKjeder: MeldeperiodeKjeder,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
        clock: Clock,
    ): Pair<MeldekortBehandlinger, List<MeldekortBehandling>> {
        return verdi.filter { it.erÅpen() }
            .fold(Pair(this, emptyList())) { acc, meldekortBehandling ->
                val meldeperiode = oppdaterteKjeder.hentSisteMeldeperiodeForKjede(
                    kjedeId = meldekortBehandling.meldeperiode.kjedeId,
                )
                meldekortBehandling.oppdaterMeldeperiode(meldeperiode, tiltakstypePerioder, clock)?.let {
                    Pair(
                        acc.first.oppdaterMeldekortbehandling(it),
                        acc.second + it,
                    )
                } ?: acc
            }
    }

    /**
     * Erstatt eksisterende meldekortbehandling med ny meldekortbehandling.
     */
    fun oppdaterMeldekortbehandling(meldekortBehandling: MeldekortBehandling): MeldekortBehandlinger {
        return MeldekortBehandlinger(
            verdi = verdi.map {
                if (it.id == meldekortBehandling.id) {
                    meldekortBehandling
                } else {
                    it
                }
            },
        )
    }

    fun leggTil(behandling: MeldekortUnderBehandling): MeldekortBehandlinger {
        return MeldekortBehandlinger(
            verdi = verdi.plus(behandling).sortedBy { it.fraOgMed },
        )
    }

    init {
        verdi.zipWithNext { a, b ->
            require(a.kjedeId == b.kjedeId || a.tilOgMed < b.fraOgMed) {
                "Meldekortperiodene må være sammenhengende og sortert, men var ${verdi.map { it.periode }}"
            }
        }
        require(verdi.count { it is MeldekortUnderBehandling } <= 1) {
            "Kun ett meldekort på saken kan være i tilstanden 'under behandling'"
        }
        require(verdi.map { it.sakId }.distinct().size <= 1) {
            "Alle meldekortperioder må tilhøre samme sak."
        }
        verdi.map { it.id }.also {
            require(it.size == it.distinct().size) {
                "Meldekort må ha unik id"
            }
        }
    }

    companion object {
        fun empty(): MeldekortBehandlinger {
            return MeldekortBehandlinger(
                verdi = emptyList(),
            )
        }
    }
}

fun NonEmptyList<MeldekortBehandling>.tilMeldekortperioder(): MeldekortBehandlinger {
    return MeldekortBehandlinger(
        verdi = this,
    )
}
