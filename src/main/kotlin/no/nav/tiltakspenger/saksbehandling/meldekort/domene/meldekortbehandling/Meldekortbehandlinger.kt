package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.KanIkkeSendeMeldekortbehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.SendMeldekortbehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import java.time.Clock
import java.time.LocalDateTime

/**
 * Består av ingen, én eller flere [Meldekortbehandling].
 * Vil være tom fram til første innvilgede søknadsbehandling.
 * Merk at [verdi] inneholder alle meldekortbehandlinger, inkludert de som er avbrutt, og bør ikke brukes direkte!
 * [verdi] må være sortert i [behandlingsrekkefølge].
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

    /** Behandlingene av hver kjede, sortert i [behandlingsrekkefølge]. */
    val behandledeMeldekortPerKjede: Map<MeldeperiodeKjedeId, List<Meldekortbehandling.Behandlet>> by lazy {
        behandledeMeldekort
            .flatMap { behandling -> behandling.kjedeIder.map { kjedeId -> kjedeId to behandling } }
            .groupBy({ it.first }, { it.second })
    }

    val sisteBehandledeMeldekortPerKjede: List<Meldekortbehandling.Behandlet> by lazy {
        behandledeMeldekortPerKjede.values.map { it.last() }
    }

    /** meldekort med status UNDER_BEHANDLING */
    val meldekortbehandlingerUnderBehandling: List<MeldekortUnderBehandling> by lazy {
        verdi.filterIsInstance<MeldekortUnderBehandling>()
            .filter { it.status == MeldekortbehandlingStatus.UNDER_BEHANDLING }
    }

    val godkjenteMeldekort: List<Meldekortbehandling.Behandlet> by lazy {
        behandledeMeldekort.filter {
            it.status == MeldekortbehandlingStatus.GODKJENT || it.status == MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET
        }
    }

    val sisteGodkjenteMeldekort: Meldekortbehandling.Behandlet? by lazy { godkjenteMeldekort.lastOrNull() }

    /** Meldekort som er under behandling eller venter på beslutning */
    val åpneMeldekortbehandlinger: List<Meldekortbehandling> = verdi.filter { it.erÅpen() }
    val harÅpenBehandling: Boolean by lazy { åpneMeldekortbehandlinger.isNotEmpty() }

    fun hentÅpenBehandlingForKjede(kjedeId: MeldeperiodeKjedeId): Meldekortbehandling? {
        return åpneMeldekortbehandlinger.find { it.kjedeIder.contains(kjedeId) }
    }

    /**
     * Finner de av [kjedeIder] som allerede er omfattet av en åpen meldekortbehandling.
     * To åpne meldekortbehandlinger kan ikke omfatte samme meldeperiodekjede, siden det gir konflikter på blant annet [MeldeperiodebehandlingType].
     * Behandlingen [ekskluderBehandlingId] holdes utenfor, slik at en åpen behandling kan oppdateres med sine egne kjeder.
     */
    fun kjederMedAnnenÅpenBehandling(
        kjedeIder: Collection<MeldeperiodeKjedeId>,
        ekskluderBehandlingId: MeldekortId? = null,
    ): Set<MeldeperiodeKjedeId> {
        val opptatteKjeder = åpneMeldekortbehandlinger
            .filterNot { it.id == ekskluderBehandlingId }
            .flatMap { it.kjedeIder }
            .toSet()

        return kjedeIder.filterTo(mutableSetOf()) { it in opptatteKjeder }
    }

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
    fun hentIkkeAvbrutteBehandlingerForKjede(kjedeId: MeldeperiodeKjedeId): List<Meldekortbehandling> {
        return ikkeAvbrutteMeldekortbehandlinger.filter { it.kjedeIder.contains(kjedeId) }
    }

    fun hentAvbrutteBehandlingerForKjede(kjedeId: MeldeperiodeKjedeId): List<Meldekortbehandling> {
        return avbrutteMeldekortbehandlinger.filter { it.kjedeIder.contains(kjedeId) }
    }

    fun hentSisteMeldekortbehandlingForKjede(kjedeId: MeldeperiodeKjedeId): Meldekortbehandling? {
        return verdi.filter { it.kjedeIder.contains(kjedeId) }.maxByOrNull { it.sistEndret }
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
            }.sortedWith(behandlingsrekkefølge),
        )
    }

    fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Meldekortbehandlinger {
        return Meldekortbehandlinger(
            verdi = verdi.map {
                if (it.klagebehandling?.id == klagebehandling.id) {
                    it.oppdaterKlagebehandling(klagebehandling)
                } else {
                    it
                }
            }.sortedWith(behandlingsrekkefølge),
        )
    }

    fun leggTil(behandling: MeldekortUnderBehandling): Meldekortbehandlinger {
        return Meldekortbehandlinger(
            verdi = verdi.plus(behandling).sortedWith(behandlingsrekkefølge),
        )
    }

    fun leggTil(behandling: MeldekortBehandletAutomatisk): Meldekortbehandlinger {
        return Meldekortbehandlinger(
            verdi = verdi.plus(behandling).sortedWith(behandlingsrekkefølge),
        )
    }

    fun åpneBehandlingerMedKlagebehandlingId(klagebehandlingId: KlagebehandlingId): List<Meldekortbehandling> {
        return this.filter { it.erÅpen() && it.klagebehandling?.id == klagebehandlingId }
    }

    init {
        verdi.zipWithNext { a, b ->
            require(behandlingsrekkefølge.compare(a, b) < 0) {
                "Meldekortbehandlingene må være sortert på iverksatt-tidspunkt (ikke-iverksatte sist), deretter opprettet-tidspunkt. ${a.id}(iverksatt ${a.iverksattTidspunkt}, opprettet ${a.opprettet}) er ikke før ${b.id}(iverksatt ${b.iverksattTidspunkt}, opprettet ${b.opprettet})"
            }
        }

        verdi.map { it.id }.also {
            require(it.size == it.distinct().size) {
                "Meldekort må ha unik id"
            }
        }

        val samtidigBehandledeKjeder = åpneMeldekortbehandlinger
            .flatMap { behandling -> behandling.kjedeIder }
            .nonDistinctBy { it }

        require(samtidigBehandledeKjeder.isEmpty()) {
            "To åpne meldekortbehandlinger kan ikke omfatte samme meldeperiodekjede. Samtidig behandlede kjeder: $samtidigBehandledeKjeder - sak $sakId"
        }

        ikkeAvbrutteMeldekortbehandlinger
            .flatMap { behandling -> behandling.meldeperioder }
            .groupBy { it.kjedeId }
            .forEach { (kjedeId, meldeperiodebehandlinger) ->
                val faktiskeTyper = meldeperiodebehandlinger.map { it.type }
                val forventedeTyper = faktiskeTyper.mapIndexed { index, _ ->
                    if (index == 0) {
                        MeldeperiodebehandlingType.FØRSTE_BEHANDLING
                    } else {
                        MeldeperiodebehandlingType.KORRIGERING
                    }
                }

                require(faktiskeTyper == forventedeTyper) {
                    "Den første behandlingen av en meldeperiodekjede må ha type FØRSTE_BEHANDLING, og de påfølgende KORRIGERING. Kjede $kjedeId har typene $faktiskeTyper - sak $sakId"
                }
            }
    }

    companion object {
        /**
         * Rekkefølgen behandlingene får virkning i, og som [Meldekortbehandlinger] er sortert i.
         * Iverksatte behandlinger sorteres på iverksettelsestidspunkt, og behandlinger som ikke er iverksatt (åpne og avbrutte) kommer sist.
         * Flere behandlinger kan være åpne samtidig, så opprettet-tidspunktet sier ikke noe om rekkefølgen behandlingene iverksettes i.
         * En åpen behandling kan for eksempel utvides med en kjede som en nyere behandling har iverksatt i mellomtiden.
         * Opprettet-tidspunktet brukes derfor bare til å skille behandlinger med likt iverksettelsestidspunkt.
         */
        val behandlingsrekkefølge: Comparator<Meldekortbehandling> =
            compareBy<Meldekortbehandling, LocalDateTime?>(nullsLast()) { it.iverksattTidspunkt }
                .thenBy { it.opprettet }

        fun empty(): Meldekortbehandlinger {
            return Meldekortbehandlinger(
                verdi = emptyList(),
            )
        }
    }
}
