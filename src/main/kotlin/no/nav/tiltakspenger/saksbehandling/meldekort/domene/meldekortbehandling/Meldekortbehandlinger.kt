package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.KanIkkeSendeMeldekortbehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.SendMeldekortbehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import java.time.Clock

/**
 * Består av ingen, én eller flere [Meldekortbehandling].
 * Vil være tom fram til første innvilgede søknadsbehandling.
 * Kun en behandling kan være under behandling (åpen) til enhver tid.
 * Merk at [verdi] inneholder alle meldekortbehandlinger, inkludert de som er avbrutt, og bør ikke brukes direkte!
 */
data class Meldekortbehandlinger(
    private val verdi: List<Meldekortbehandling>,
) : List<Meldekortbehandling> by verdi {

    val log = KotlinLogging.logger { }

    val fnr: Fnr? by lazy { verdi.distinctBy { it.fnr }.map { it.fnr }.singleOrNullOrThrow() }
    val sakId: SakId? by lazy { verdi.distinctBy { it.sakId }.map { it.sakId }.singleOrNullOrThrow() }
    val saksnummer: Saksnummer? by lazy {
        verdi.distinctBy { it.saksnummer }.map { it.saksnummer }.singleOrNullOrThrow()
    }

    val ikkeAvbrutteMeldekortbehandlinger: List<Meldekortbehandling> by lazy {
        verdi.filter { it !is MeldekortbehandlingAvbrutt }
    }

    val avbrutteMeldekortbehandlinger: List<MeldekortbehandlingAvbrutt> by lazy {
        verdi.filterIsInstance<MeldekortbehandlingAvbrutt>()
    }

    private val behandledeMeldekort: List<Meldekortbehandling.Behandlet> by lazy {
        verdi.filterIsInstance<Meldekortbehandling.Behandlet>()
    }

    val behandledeMeldekortPerKjede: Map<MeldeperiodeKjedeId, List<Meldekortbehandling.Behandlet>> by lazy {
        behandledeMeldekort
            .sortedBy { it.opprettet }
            .groupBy { it.kjedeIdLegacy }
    }

    val sisteBehandledeMeldekortPerKjede: List<Meldekortbehandling.Behandlet> by lazy {
        behandledeMeldekortPerKjede.values.map { it.last() }
    }

    /** meldekort med status UNDER_BEHANDLING */
    val meldekortUnderBehandling: MeldekortUnderBehandling? by lazy {
        verdi.singleOrNullOrThrow { it.status == MeldekortbehandlingStatus.UNDER_BEHANDLING } as MeldekortUnderBehandling?
    }

    val godkjenteMeldekort: List<Meldekortbehandling.Behandlet> by lazy {
        behandledeMeldekort.filter {
            it.status == MeldekortbehandlingStatus.GODKJENT || it.status == MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET
        }
    }

    val sisteGodkjenteMeldekort: Meldekortbehandling.Behandlet? by lazy { godkjenteMeldekort.maxByOrNull { it.opprettet } }

    /** Meldekort som er under behandling eller venter på beslutning */
    val åpenMeldekortbehandling: Meldekortbehandling? by lazy { this.singleOrNullOrThrow { it.erÅpen() } }

    val harÅpenBehandling: Boolean by lazy { åpenMeldekortbehandling != null }

    fun sendTilBeslutter(
        kommando: SendMeldekortbehandlingTilBeslutterKommando,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortbehandlingTilBeslutter, Pair<Meldekortbehandlinger, MeldekortbehandlingManuell>> {
        val meldekort = hentMeldekortbehandling(kommando.meldekortId) as MeldekortUnderBehandling
        return meldekort.sendTilBeslutter(
            kommando = kommando,
            clock = clock,
        ).map { Pair(oppdaterMeldekortbehandling(it), it) }
    }

    fun hentMeldekortbehandling(meldekortId: MeldekortId): Meldekortbehandling? {
        return verdi.singleOrNullOrThrow { it.id == meldekortId }
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortbehandlingerForKjede(kjedeId: MeldeperiodeKjedeId): List<Meldekortbehandling> {
        return ikkeAvbrutteMeldekortbehandlinger.filter { it.kjedeIdLegacy == kjedeId }
    }

    fun hentAvbrutteMeldekortbehandlingerForKjede(kjedeId: MeldeperiodeKjedeId): List<Meldekortbehandling> {
        return avbrutteMeldekortbehandlinger.filter { it.kjedeIdLegacy == kjedeId }
    }

    fun hentSisteMeldekortbehandlingForKjede(kjedeId: MeldeperiodeKjedeId): Meldekortbehandling? {
        return verdi.filter { it.kjedeIdLegacy == kjedeId }.maxByOrNull { it.opprettet }
    }

    /**
     * Løper igjennom alle ikke-avsluttede meldekortbehandlinger (også de som er sendt til beslutter), setter tilstanden til under behandling, oppdaterer meldeperioden og resetter utfyllinga.
     */
    fun oppdaterMedNyeKjeder(
        oppdaterteKjeder: MeldeperiodeKjeder,
        clock: Clock,
    ): Pair<Meldekortbehandlinger, List<Meldekortbehandling>> {
        return verdi
            .filter { it.erÅpen() }
            .fold(Pair(this, emptyList())) { acc, meldekortbehandling ->
                meldekortbehandling.oppdaterMeldeperioder(oppdaterteKjeder, clock)?.let {
                    Pair(
                        acc.first.oppdaterMeldekortbehandling(it),
                        acc.second + it,
                    )
                } ?: acc
            }
    }

    /** Erstatt eksisterende meldekortbehandling med ny meldekortbehandling. */
    fun oppdaterMeldekortbehandling(meldekortbehandling: Meldekortbehandling): Meldekortbehandlinger {
        return Meldekortbehandlinger(
            verdi = verdi.map {
                if (it.id == meldekortbehandling.id) {
                    meldekortbehandling
                } else {
                    it
                }
            },
        )
    }

    fun leggTil(behandling: MeldekortUnderBehandling): Meldekortbehandlinger {
        return Meldekortbehandlinger(
            verdi = verdi.plus(behandling).sortedBy { it.opprettet },
        )
    }

    fun leggTil(behandling: MeldekortBehandletAutomatisk): Meldekortbehandlinger {
        return Meldekortbehandlinger(
            verdi = verdi.plus(behandling).sortedBy { it.opprettet },
        )
    }

    init {
        verdi.zipWithNext { a, b ->
            require(a.opprettet < b.opprettet) {
                "Meldekortperiodene må være sortert på opprettet-tidspunkt. ${a.opprettet}(${a.id}) er ikke før ${b.opprettet}(${b.id})"
            }
        }

        require(verdi.count { it.erÅpen() } <= 1) {
            "Kun ett meldekort på saken kan være åpen om gangen"
        }

        verdi.map { it.id }.also {
            require(it.size == it.distinct().size) {
                "Meldekort må ha unik id"
            }
        }
    }

    companion object {
        fun empty(): Meldekortbehandlinger {
            return Meldekortbehandlinger(
                verdi = emptyList(),
            )
        }
    }
}
