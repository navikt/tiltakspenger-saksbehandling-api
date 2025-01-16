package no.nav.tiltakspenger.meldekort.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling.IkkeUtfyltMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling.UtfyltMeldekort
import java.time.LocalDate

/**
 * Består av ingen, én eller flere [MeldeperiodeBeregning].
 * Vil være tom fram til første innvilgede førstegangsbehandling.
 * Kun den siste vil kunne være ikke-utfylt (åpen).
 * @param tiltakstype I MVP støtter vi kun ett tiltak, men på sikt kan vi ikke garantere at det er én til én mellom meldekortperioder og tiltakstype.
 */
data class MeldekortBehandlinger(
    val tiltakstype: TiltakstypeSomGirRett,
    val verdi: List<MeldekortBehandling>,
) : List<MeldekortBehandling> by verdi {

    /**
     * @throws NullPointerException Dersom det ikke er noen meldekort som kan sendes til beslutter. Eller siste meldekort ikke er i tilstanden 'ikke utfylt'.
     * @throws IllegalArgumentException Dersom innsendt meldekortid ikke samsvarer med siste meldekortperiode.
     */
    fun sendTilBeslutter(
        kommando: SendMeldekortTilBeslutterKommando,
    ): Either<KanIkkeSendeMeldekortTilBeslutter, Pair<MeldekortBehandlinger, UtfyltMeldekort>> {
        val ikkeUtfyltMeldekort = this.ikkeUtfyltMeldekort!!

        require(ikkeUtfyltMeldekort.id == kommando.meldekortId) {
            "MeldekortId i kommando (${kommando.meldekortId}) samsvarer ikke med siste meldekortperiode (${ikkeUtfyltMeldekort.id})"
        }
        val meldekortdager = kommando.beregn(eksisterendeMeldekort = this)
        val utfyltMeldeperiode = ikkeUtfyltMeldekort.beregning.tilUtfyltMeldeperiode(meldekortdager).getOrElse {
            return it.left()
        }
        return ikkeUtfyltMeldekort.sendTilBeslutter(utfyltMeldeperiode, kommando.saksbehandler, kommando.navkontor)
            .map {
                Pair(
                    MeldekortBehandlinger(
                        tiltakstype = tiltakstype,
                        verdi = (verdi.dropLast(1) + it).toNonEmptyListOrNull()!!,
                    ),
                    it,
                )
            }
    }

    fun hentMeldekortForId(meldekortId: MeldekortId): MeldekortBehandling? {
        return verdi.find { it.id == meldekortId }
    }

    fun hentMeldekortForKjedeId(meldeperiodeId: MeldeperiodeId): MeldekortBehandling? {
        return verdi.find { it.meldeperiodeId == meldeperiodeId }
    }

    val periode: Periode by lazy { Periode(verdi.first().fraOgMed, verdi.last().tilOgMed) }

    val utfylteMeldekort: List<UtfyltMeldekort> by lazy { verdi.filterIsInstance<UtfyltMeldekort>() }

    val godkjenteMeldekort: List<UtfyltMeldekort> by lazy { utfylteMeldekort.filter { it.status == MeldekortBehandlingStatus.GODKJENT } }

    val sisteGodkjenteMeldekort: UtfyltMeldekort? by lazy { godkjenteMeldekort.lastOrNull() }

    val sisteGodkjenteMeldekortDag: LocalDate? by lazy { sisteGodkjenteMeldekort?.periode?.tilOgMed }

    /** Merk at denne går helt tilbake til siste godkjente, utbetalte dag. Dette er ikke nødvendigvis den siste godkjente meldeperioden. */
    val sisteUtbetalteMeldekortDag: LocalDate? by lazy {
        godkjenteMeldekort.flatMap { it.beregning.dager }.lastOrNull { it.beløp > 0 }?.dato
    }

    /** Vil kun returnere hele meldekortperioder som er utfylt. Dersom siste meldekortperiode er delvis utfylt, vil ikke disse komme med. */
    val utfylteDager: List<MeldeperiodeBeregningDag.Utfylt> by lazy { utfylteMeldekort.flatMap { it.beregning.dager } }

    /** Så lenge saken er aktiv, vil det siste meldekortet være i tilstanden ikke utfylt. Vil også være null fram til første innvilgelse. */
    val ikkeUtfyltMeldekort: IkkeUtfyltMeldekort? by lazy {
        verdi.filterIsInstance<IkkeUtfyltMeldekort>().singleOrNullOrThrow()
    }

    val sakId: SakId by lazy { verdi.first().sakId }

    init {
        verdi.zipWithNext { a, b ->
            require(a.tilOgMed.plusDays(1) == b.fraOgMed) {
                "Meldekortperiodene må være sammenhengende og sortert, men var ${verdi.map { it.periode }}"
            }
        }
        verdi.zipWithNext { a, b ->
            require(a.id == b.forrigeMeldekortId) {
                "Neste meldekort ${b.id} må peke på forrige meldekort ${a.id}, men peker på ${b.forrigeMeldekortId}"
            }
        }
        require(
            verdi.all {
                it.tiltakstype == tiltakstype
            },
        ) {
            "Alle meldekortperioder må ha samme tiltakstype. Meldekortperioder.tiltakstype=$tiltakstype, meldekortperioders tiltakstyper=${
                verdi.map {
                    it.tiltakstype
                }
            }"
        }
        require(verdi.dropLast(1).all { it is UtfyltMeldekort }) {
            "Kun det siste meldekortet kan være i tilstanden 'ikke utfylt', de N første må være 'utfylt'."
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
        fun empty(tiltakstype: TiltakstypeSomGirRett): MeldekortBehandlinger {
            return MeldekortBehandlinger(
                tiltakstype = tiltakstype,
                verdi = emptyList(),
            )
        }
    }
}

fun NonEmptyList<MeldekortBehandling>.tilMeldekortperioder(): MeldekortBehandlinger {
    val tiltakstype = first().tiltakstype
    return MeldekortBehandlinger(
        tiltakstype = tiltakstype,
        verdi = this,
    )
}
