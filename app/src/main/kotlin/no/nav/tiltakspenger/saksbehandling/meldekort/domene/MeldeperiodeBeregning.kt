package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Fra paragraf 5: Enhver som mottar tiltakspenger, må som hovedregel melde seg til Arbeids- og velferdsetaten hver fjortende dag (meldeperioden)
 *
 * @property maksDagerMedTiltakspengerForPeriode Maks antall dager bruker kan få tiltakspenger i meldeperioden. 100% vil tilsvare 5 dager i uken.
 */
sealed interface MeldeperiodeBeregning : List<MeldeperiodeBeregningDag> {
    val fraOgMed: LocalDate get() = this.first().dato
    val tilOgMed: LocalDate get() = this.last().dato
    val periode: Periode get() = Periode(fraOgMed, tilOgMed)
    val sakId: SakId
    val meldekortId: MeldekortId
    val maksDagerMedTiltakspengerForPeriode: Int
    val dager: NonEmptyList<MeldeperiodeBeregningDag>
    val antallDagerMedDeltattEllerFravær: Int get() = dager.count { it.harDeltattEllerFravær }

    data class UtfyltMeldeperiode(
        override val sakId: SakId,
        override val maksDagerMedTiltakspengerForPeriode: Int,
        override val dager: NonEmptyList<MeldeperiodeBeregningDag.Utfylt>,
    ) : MeldeperiodeBeregning,
        List<MeldeperiodeBeregningDag> by dager {
        override val meldekortId = dager.first().meldekortId

        init {
            require(dager.size == 14) { "En meldekortperiode må være 14 dager, men var ${dager.size}" }
            require(dager.first().dato.dayOfWeek == DayOfWeek.MONDAY) { "Utbetalingsperioden må starte på en mandag" }
            require(dager.last().dato.dayOfWeek == DayOfWeek.SUNDAY) { "Utbetalingsperioden må slutte på en søndag" }
            dager.forEachIndexed { index, dag ->
                require(dager.first().dato.plusDays(index.toLong()) == dag.dato) {
                    "Datoene må være sammenhengende og sortert, men var ${dager.map { it.dato }}"
                }
            }

            require(
                dager.all { it.meldekortId == meldekortId },
            ) { "Alle dager må tilhøre samme meldekort, men var: ${dager.map { it.meldekortId }}" }

            validerAntallDager().onLeft {
                throw IllegalArgumentException(
                    "For mange dager utfylt - ${it.antallDagerUtfylt} var utfylt, maks antall for perioden er ${it.maksDagerMedTiltakspengerForPeriode}",
                )
            }
        }

        /**
         * Ordinær stønad, ikke med barnetillegg
         */
        fun beregnTotalOrdinærBeløp(): Int = dager.sumOf { it.beregningsdag?.beløp ?: 0 }

        /**
         * Barnetillegg uten ordinær stønad
         */
        fun beregnTotalBarnetiillegg(): Int = dager.sumOf { it.beregningsdag?.beløpBarnetillegg ?: 0 }

        /**
         * Ordinær stønad + barnetillegg
         */
        fun beregnTotaltBeløp(): Int = beregnTotalOrdinærBeløp() + beregnTotalBarnetiillegg()
    }

    /**
     * Merk at ikke utfylt betyr at ikke alle dager utfylt. Noen dager kan være Sperret, og de anses som utfylt.
     */
    data class IkkeUtfyltMeldeperiode(
        override val sakId: SakId,
        override val maksDagerMedTiltakspengerForPeriode: Int,
        override val dager: NonEmptyList<MeldeperiodeBeregningDag>,
    ) : MeldeperiodeBeregning,
        List<MeldeperiodeBeregningDag> by dager {

        override val meldekortId = dager.first().meldekortId

        private val log = mu.KotlinLogging.logger {}

        fun settPeriodeTilSperret(periode: Periode): IkkeUtfyltMeldeperiode {
            return this.copy(
                dager = this.dager.map {
                    if (periode.inneholder(it.dato)) {
                        MeldeperiodeBeregningDag.Utfylt.Sperret(
                            dato = it.dato,
                            meldekortId = it.meldekortId,
                        )
                    } else {
                        it
                    }
                }.toNonEmptyListOrNull()!!,
            )
        }

        /**
         * Brukes når et nytt vedtak sperrer hele denne meldeperioden.
         */
        fun settAlleDagerTilSperret(): IkkeUtfyltMeldeperiode {
            return this.copy(
                maksDagerMedTiltakspengerForPeriode = 0,
                dager = dager.map {
                    MeldeperiodeBeregningDag.Utfylt.Sperret(
                        dato = it.dato,
                        meldekortId = it.meldekortId,
                    )
                }.toNonEmptyListOrNull()!!,
            )
        }

        companion object {
            /**
             * @param meldeperiode Perioden meldekortet skal gjelde for. Må være 14 dager, starte på en mandag og slutte på en søndag.
             * @return Meldekortperiode som er utfylt.
             * @throws IllegalStateException Dersom alle dagene i en meldekortperiode er SPERRET er den per definisjon utfylt. Dette har vi ikke støtte for i MVP.
             */
            fun fraPeriode(
                meldeperiode: Meldeperiode,
                meldekortId: MeldekortId,
                sakId: SakId,
                maksDagerMedTiltakspengerForPeriode: Int,
                tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
            ): IkkeUtfyltMeldeperiode {
                val dager =
                    meldeperiode.periode.tilDager().map { dag ->
                        if (meldeperiode.girRett[dag] == true) {
                            MeldeperiodeBeregningDag.IkkeUtfylt(
                                dato = dag,
                                meldekortId = meldekortId,
                                tiltakstype = tiltakstypePerioder.hentVerdiForDag(dag)!!,
                            )
                        } else {
                            MeldeperiodeBeregningDag.Utfylt.Sperret(
                                dato = dag,
                                meldekortId = meldekortId,
                            )
                        }
                    }
                return if (dager.any { it is MeldeperiodeBeregningDag.IkkeUtfylt }) {
                    IkkeUtfyltMeldeperiode(sakId, maksDagerMedTiltakspengerForPeriode, dager.toNonEmptyListOrNull()!!)
                } else {
                    throw IllegalStateException("Alle dagene i en meldekortperiode er SPERRET. Dette har vi ikke støtte for i MVP.")
                }
            }
        }

        fun tilUtfyltMeldeperiode(
            utfylteDager: NonEmptyList<MeldeperiodeBeregningDag.Utfylt>,
        ): Either<KanIkkeSendeMeldekortTilBeslutning, UtfyltMeldeperiode> {
            return validerAntallDager().map {
                UtfyltMeldeperiode(
                    sakId = sakId,
                    maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
                    dager = utfylteDager,
                )
            }
        }

        init {
            require(dager.size == 14) { "En meldekortperiode må være 14 dager, men var ${dager.size}" }
            require(dager.first().dato.dayOfWeek == DayOfWeek.MONDAY) { "Utbetalingsperioden må starte på en mandag" }
            require(dager.last().dato.dayOfWeek == DayOfWeek.SUNDAY) { "Utbetalingsperioden må slutte på en søndag" }
            dager.forEachIndexed { index, dag ->
                require(dager.first().dato.plusDays(index.toLong()) == dag.dato) {
                    "Datoene må være sammenhengende og sortert, men var ${dager.map { it.dato }}"
                }
            }
            require(
                dager.all { it.meldekortId == meldekortId },
            ) { "Alle dager må tilhøre samme meldekort, men var: ${dager.map { it.meldekortId }}" }
            require(
                dager.all { it is MeldeperiodeBeregningDag.IkkeUtfylt || it is MeldeperiodeBeregningDag.Utfylt.Sperret },
            ) { "Alle dagene må være av typen Ikke Utfylt eller Sperret." }
        }
    }
}

/** Denne skal ikke kalles utenfra */
private fun MeldeperiodeBeregning.validerAntallDager(): Either<KanIkkeSendeMeldekortTilBeslutning.ForMangeDagerUtfylt, Unit> {
    return if (antallDagerMedDeltattEllerFravær > this.maksDagerMedTiltakspengerForPeriode) {
        return KanIkkeSendeMeldekortTilBeslutning.ForMangeDagerUtfylt(
            maksDagerMedTiltakspengerForPeriode = this.maksDagerMedTiltakspengerForPeriode,
            antallDagerUtfylt = antallDagerMedDeltattEllerFravær,
        ).left()
    } else {
        Unit.right()
    }
}

private fun NonEmptyList<MeldeperiodeBeregningDag>.periode() = Periode(this.first().dato, this.last().dato)
