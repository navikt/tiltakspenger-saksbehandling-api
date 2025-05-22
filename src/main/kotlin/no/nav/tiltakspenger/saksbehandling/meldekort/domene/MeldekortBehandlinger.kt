package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDate

/**
 * Består av ingen, én eller flere [MeldekortBehandling].
 * Vil være tom fram til første innvilgede søknadsbehandling.
 * Kun en behandling kan være under behandling (åpen) til enhver tid.
 * Merk at [verdi] inneholder alle meldekortbehandlinger, inkludert de som er avbrutt, og bør ikke brukes direkte!
 */
data class MeldekortBehandlinger(
    val verdi: List<MeldekortBehandling>,
) : List<MeldekortBehandling> by verdi {

    val log = KotlinLogging.logger { }

    val ikkeAvbrutteMeldekortBehandlinger: List<MeldekortBehandling> by lazy {
        verdi.filter { it !is AvbruttMeldekortBehandling }
    }

    val avbrutteMeldekortBehandlinger: List<AvbruttMeldekortBehandling> by lazy {
        verdi.filterIsInstance<AvbruttMeldekortBehandling>()
    }

    val meldeperiodeBeregninger by lazy { MeldeperiodeBeregninger(this) }

    private val behandledeMeldekort: List<MeldekortBehandling.Behandlet> by lazy {
        verdi.filterIsInstance<MeldekortBehandling.Behandlet>()
    }

    val behandledeMeldekortPerKjede: Map<MeldeperiodeKjedeId, List<MeldekortBehandling.Behandlet>> by lazy {
        behandledeMeldekort
            .sortedBy { it.opprettet }
            .groupBy { it.kjedeId }
    }

    val sisteBehandledeMeldekortPerKjede: List<MeldekortBehandling.Behandlet> by lazy {
        behandledeMeldekortPerKjede.values.map { it.last() }
    }

    /** Under behandling er ikke-avsluttede meldekortbehandlinger som ikke er til beslutning. */
    val meldekortUnderBehandling: MeldekortUnderBehandling? by lazy {
        verdi.filterIsInstance<MeldekortUnderBehandling>().singleOrNullOrThrow()
    }

    private val meldekortUnderBeslutning: MeldekortBehandletManuelt? by lazy {
        behandledeMeldekort.filter {
            it.status == MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
        }.singleOrNullOrThrow() as MeldekortBehandletManuelt?
    }

    val godkjenteMeldekort: List<MeldekortBehandling.Behandlet> by lazy {
        behandledeMeldekort.filter {
            it.status == MeldekortBehandlingStatus.GODKJENT || it.status == MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
        }
    }

    val sisteGodkjenteMeldekort: MeldekortBehandling.Behandlet? by lazy { godkjenteMeldekort.lastOrNull() }

    @Suppress("unused")
    val sisteGodkjenteMeldekortDag: LocalDate? by lazy { sisteGodkjenteMeldekort?.tilOgMed }

    /** Merk at denne går helt tilbake til siste godkjente, utbetalte dag. Dette er ikke nødvendigvis den siste godkjente meldeperioden. */
    val sisteUtbetalteMeldekortDag: LocalDate? by lazy {
        godkjenteMeldekort.flatMap { it.beregning.first().dager }.lastOrNull { it.beløp > 0 }?.dato
    }

    /** Vil kun returnere hele meldekortperioder som er utfylt og godkjent */
    val utfylteDager: List<MeldeperiodeBeregningDag> by lazy {
        meldeperiodeBeregninger.sisteBeregningForKjede.values.flatMap { it.dager }
    }

    /** Meldekort som er under behandling eller venter på beslutning */
    val åpenMeldekortBehandling: MeldekortBehandling? by lazy { meldekortUnderBehandling ?: meldekortUnderBeslutning }

    suspend fun oppdaterMeldekort(
        kommando: OppdaterMeldekortKommando,
        beregn: (meldeperiode: Meldeperiode) -> NonEmptyList<MeldeperiodeBeregning>,
        simuler: (suspend (MeldekortBehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>),
    ): Either<KanIkkeOppdatereMeldekort, Triple<MeldekortBehandlinger, MeldekortUnderBehandling, SimuleringMedMetadata?>> {
        val meldekort = hentMeldekortBehandling(kommando.meldekortId) as MeldekortUnderBehandling
        return meldekort.oppdater(
            kommando = kommando,
            beregn = beregn,
            simuler = simuler,
        ).map { Triple(oppdaterMeldekortbehandling(it.first), it.first, it.second) }
    }

    suspend fun sendTilBeslutter(
        kommando: SendMeldekortTilBeslutterKommando,
        beregn: (meldeperiode: Meldeperiode) -> NonEmptyList<MeldeperiodeBeregning>,
        simuler: (suspend (MeldekortBehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>),
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortTilBeslutter, Triple<MeldekortBehandlinger, MeldekortBehandletManuelt, SimuleringMedMetadata?>> {
        val meldekort = hentMeldekortBehandling(kommando.meldekortId) as MeldekortUnderBehandling
        return meldekort.sendTilBeslutter(
            kommando = kommando,
            beregn = beregn,
            clock = clock,
            simuler = simuler,
        ).map { Triple(oppdaterMeldekortbehandling(it.first), it.first, it.second) }
    }

    fun hentMeldekortBehandling(meldekortId: MeldekortId): MeldekortBehandling? {
        return verdi.singleOrNullOrThrow { it.id == meldekortId }
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingerForKjede(kjedeId: MeldeperiodeKjedeId): List<MeldekortBehandling> {
        return ikkeAvbrutteMeldekortBehandlinger.filter { it.kjedeId == kjedeId }
    }

    fun hentAvbrutteMeldekortBehandlingerForKjede(kjedeId: MeldeperiodeKjedeId): List<MeldekortBehandling> {
        return avbrutteMeldekortBehandlinger.filter { it.kjedeId == kjedeId }
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

    fun leggTil(behandling: MeldekortBehandletAutomatisk): MeldekortBehandlinger {
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
        require(meldekortUnderBehandling == null || meldekortUnderBeslutning == null) {
            "Kan ikke ha meldekort både under behandling og under beslutning"
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
