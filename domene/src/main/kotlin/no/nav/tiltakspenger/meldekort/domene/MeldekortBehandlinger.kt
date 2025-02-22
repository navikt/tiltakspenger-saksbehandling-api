package no.nav.tiltakspenger.meldekort.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling.MeldekortBehandlet
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling.MeldekortUnderBehandling
import java.time.LocalDate

/**
 * Består av ingen, én eller flere [MeldeperiodeBeregning].
 * Vil være tom fram til første innvilgede førstegangsbehandling.
 * Kun den siste vil kunne være under behandling (åpen).
 */
data class MeldekortBehandlinger(
    val verdi: List<MeldekortBehandling>,
) : List<MeldekortBehandling> by verdi {
    /**
     * @throws NullPointerException Dersom det ikke er noen meldekort-behandling som kan sendes til beslutter. Eller siste meldekort ikke er i tilstanden 'under behandling'.
     * @throws IllegalArgumentException Dersom innsendt meldekortid ikke samsvarer med siste meldekortperiode.
     */
    fun sendTilBeslutter(
        kommando: SendMeldekortTilBeslutningKommando,
    ): Either<KanIkkeSendeMeldekortTilBeslutning, Pair<MeldekortBehandlinger, MeldekortBehandlet>> {
        val meldekortUnderBehandling = this.meldekortUnderBehandling!!

        require(meldekortUnderBehandling.id == kommando.meldekortId) {
            "MeldekortId i kommando (${kommando.meldekortId}) samsvarer ikke med siste meldekortperiode (${meldekortUnderBehandling.id})"
        }

        val meldekortdager = kommando.beregn(eksisterendeMeldekortBehandlinger = this)
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

    fun hentMeldekortBehandlingForMeldekortBehandlingId(meldekortId: MeldekortId): MeldekortBehandling? {
        return verdi.singleOrNullOrThrow { it.id == meldekortId }
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingForMeldeperiodeId(id: HendelseId): List<MeldekortBehandling> {
        return this.filter { it.meldeperiode.id == id }
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingForMeldeperiodeKjedeId(meldeperiodeKjedeId: MeldeperiodeKjedeId): List<MeldekortBehandling> {
        return verdi.filter { it.meldeperiodeKjedeId == meldeperiodeKjedeId }
    }

    /**
     * Løper igjennom alle ikke-avsluttede meldekortbehandlinger (også de som er sendt til beslutter), setter tilstanden til under behandling, oppdaterer meldeperioden og resetter utfyllinga.
     */
    fun oppdaterMedNyeKjeder(oppdaterteKjeder: MeldeperiodeKjeder): Pair<MeldekortBehandlinger, List<MeldekortBehandling>> {
        return verdi.filter { it.erÅpen() }
            .fold(Pair(this, emptyList())) { acc, meldekortBehandling ->
                val meldeperiode = oppdaterteKjeder.hentSisteMeldeperiodeForKjedeId(
                    kjedeId = meldekortBehandling.meldeperiode.meldeperiodeKjedeId,
                )
                meldekortBehandling.oppdaterMeldeperiode(meldeperiode)?.let {
                    Pair(
                        acc.first.oppdaterMeldekortbehandling(it),
                        acc.second + it,
                    )
                } ?: acc
            }
    }

    val periode: Periode by lazy { Periode(verdi.first().fraOgMed, verdi.last().tilOgMed) }

    val behandledeMeldekort: List<MeldekortBehandlet> by lazy { verdi.filterIsInstance<MeldekortBehandlet>() }

    val godkjenteMeldekort: List<MeldekortBehandlet> by lazy { behandledeMeldekort.filter { it.status == MeldekortBehandlingStatus.GODKJENT } }

    val sisteGodkjenteMeldekort: MeldekortBehandlet? by lazy { godkjenteMeldekort.lastOrNull() }

    val sisteGodkjenteMeldekortDag: LocalDate? by lazy { sisteGodkjenteMeldekort?.periode?.tilOgMed }

    /** Merk at denne går helt tilbake til siste godkjente, utbetalte dag. Dette er ikke nødvendigvis den siste godkjente meldeperioden. */
    val sisteUtbetalteMeldekortDag: LocalDate? by lazy {
        godkjenteMeldekort.flatMap { it.beregning.dager }.lastOrNull { it.beløp > 0 }?.dato
    }

    /** Vil kun returnere hele meldekortperioder som er utfylt. Dersom siste meldekortperiode er delvis utfylt, vil ikke disse komme med. */
    val utfylteDager: List<MeldeperiodeBeregningDag.Utfylt> by lazy { behandledeMeldekort.flatMap { it.beregning.dager } }

    /** Under behandling er ikke-avsluttede meldekortbehandlinger som ikke er til beslutning. */
    val meldekortUnderBehandling: MeldekortUnderBehandling? by lazy {
        verdi.filterIsInstance<MeldekortUnderBehandling>().singleOrNullOrThrow()
    }

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
        require(verdi.distinctBy { it.tiltakstype }.size <= 1) {
            "Alle meldekortperioder må ha samme tiltakstype. Meldekortperioders tiltakstyper=${
                verdi.map {
                    it.tiltakstype
                }
            }"
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
