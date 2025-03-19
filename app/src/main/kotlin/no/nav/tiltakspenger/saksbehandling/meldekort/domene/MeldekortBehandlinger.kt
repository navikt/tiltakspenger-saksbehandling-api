package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.toNonEmptyListOrNull
import mu.KotlinLogging
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
import java.time.LocalDate

/**
 * Består av ingen, én eller flere [MeldeperiodeBeregning].
 * Vil være tom fram til første innvilgede førstegangsbehandling.
 * Kun den siste vil kunne være under behandling (åpen).
 */
data class MeldekortBehandlinger(
    val verdi: List<MeldekortBehandling>,
) : List<MeldekortBehandling> by verdi {

    val log = KotlinLogging.logger { }

    /**
     * @throws NullPointerException Dersom det ikke er noen meldekort-behandling som kan sendes til beslutter. Eller siste meldekort ikke er i tilstanden 'under behandling'.
     * @throws IllegalArgumentException Dersom innsendt meldekortid ikke samsvarer med siste meldekortperiode.
     */
    fun sendTilBeslutter(
        kommando: SendMeldekortTilBeslutningKommando,
        barnetilleggsPerioder: Periodisering<AntallBarn>,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett>,
    ): Either<KanIkkeSendeMeldekortTilBeslutning, Pair<MeldekortBehandlinger, MeldekortBehandlet>> {
        val meldekortId = kommando.meldekortId

        val meldekortUnderBehandling = hentMeldekortBehandling(meldekortId)

        requireNotNull(meldekortUnderBehandling) {
            "Fant ikke innsendt meldekort $meldekortId på saken"
        }
        require(meldekortUnderBehandling is MeldekortUnderBehandling) {
            "Innsendt meldekort $meldekortId er ikke under behandling"
        }

        if (kommando.periode != meldekortUnderBehandling.periode) {
            return InnsendteDagerMåMatcheMeldeperiode.left()
        }

        kommando.dager.dager.zip(meldekortUnderBehandling.beregning.dager).forEach { (dagA, dagB) ->
            if (dagA.status == Status.SPERRET && dagB !is MeldeperiodeBeregningDag.Utfylt.Sperret) {
                log.error { "Kan ikke endre dag til sperret. Nåværende tilstand: $utfylteDager. Innsendte dager: ${kommando.dager}" }
                return KanIkkeSendeMeldekortTilBeslutning.KanIkkeEndreDagTilSperret.left()
            }
            if (dagA.status != Status.SPERRET && dagB is MeldeperiodeBeregningDag.Utfylt.Sperret) {
                log.error { "Kan ikke endre dag fra sperret. Nåværende tilstand: $utfylteDager. Innsendte dager: ${kommando.dager}" }
                return KanIkkeSendeMeldekortTilBeslutning.KanIkkeEndreDagFraSperret.left()
            }
        }
        val meldekortdager = kommando.beregn(
            eksisterendeMeldekortBehandlinger = this,
            barnetilleggsPerioder = barnetilleggsPerioder,
            tiltakstypePerioder = tiltakstypePerioder,
        )
        val utfyltMeldeperiode = meldekortUnderBehandling.beregning.tilUtfyltMeldeperiode(meldekortdager).getOrElse {
            return it.left()
        }
        return meldekortUnderBehandling.sendTilBeslutter(utfyltMeldeperiode, kommando.saksbehandler)
            .map {
                Pair(
                    MeldekortBehandlinger(
                        verdi = (verdi.dropLast(1) + it).toNonEmptyListOrNull()!!,
                    ),
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
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett>,
    ): Pair<MeldekortBehandlinger, List<MeldekortBehandling>> {
        return verdi.filter { it.erÅpen() }
            .fold(Pair(this, emptyList())) { acc, meldekortBehandling ->
                val meldeperiode = oppdaterteKjeder.hentSisteMeldeperiodeForKjede(
                    kjedeId = meldekortBehandling.meldeperiode.kjedeId,
                )
                meldekortBehandling.oppdaterMeldeperiode(meldeperiode, tiltakstypePerioder)?.let {
                    Pair(
                        acc.first.oppdaterMeldekortbehandling(it),
                        acc.second + it,
                    )
                } ?: acc
            }
    }

    val periode: Periode by lazy { Periode(verdi.first().fraOgMed, verdi.last().tilOgMed) }

    private val behandledeMeldekort: List<MeldekortBehandlet> by lazy { verdi.filterIsInstance<MeldekortBehandlet>() }

    /** Under behandling er ikke-avsluttede meldekortbehandlinger som ikke er til beslutning. */
    val meldekortUnderBehandling: List<MeldekortUnderBehandling> by lazy {
        verdi.filterIsInstance<MeldekortUnderBehandling>()
    }

    val godkjenteMeldekort: List<MeldekortBehandlet> by lazy { behandledeMeldekort.filter { it.status == MeldekortBehandlingStatus.GODKJENT } }

    val sisteGodkjenteMeldekort: MeldekortBehandlet? by lazy { godkjenteMeldekort.lastOrNull() }

    @Suppress("unused")
    val sisteGodkjenteMeldekortDag: LocalDate? by lazy { sisteGodkjenteMeldekort?.periode?.tilOgMed }

    /** Merk at denne går helt tilbake til siste godkjente, utbetalte dag. Dette er ikke nødvendigvis den siste godkjente meldeperioden. */
    val sisteUtbetalteMeldekortDag: LocalDate? by lazy {
        godkjenteMeldekort.flatMap { it.beregning.dager }.lastOrNull { it.beløp > 0 }?.dato
    }

    /** Vil kun returnere hele meldekortperioder som er utfylt. Dersom siste meldekortperiode er delvis utfylt, vil ikke disse komme med. */
    val utfylteDager: List<MeldeperiodeBeregningDag.Utfylt> by lazy { behandledeMeldekort.flatMap { it.beregning.dager } }

    val sakId: SakId by lazy { verdi.first().sakId }

    /**
     * Erstatt eksisterende meldekortbehandling med ny meldekortbehandling.
     */
    private fun oppdaterMeldekortbehandling(meldekortBehandling: MeldekortBehandling): MeldekortBehandlinger {
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

    init {
        verdi.zipWithNext { a, b ->
            require(a.tilOgMed.plusDays(1) == b.fraOgMed) {
                "Meldekortperiodene må være sammenhengende og sortert, men var ${verdi.map { it.periode }}"
            }
        }
        require(verdi.dropLast(1).all { it is MeldekortBehandlet }) {
            "Kun det siste meldekortet kan være i tilstanden 'under behandling', de N første må være 'behandlet'."
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
