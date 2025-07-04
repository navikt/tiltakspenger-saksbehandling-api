package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import java.time.LocalDate

/**
 * Beregning av en enkelt dag i en meldeperiode.
 * Merk at en dag i meldeperiode kan være på utsiden av et innvilgelsesvedtak, eller dagen kan ha vært innvilget og senere stanset.
 * Da bruker vi statusen IKKE_RETT_TIL_TILTAKSPENGER
 *
 * Vi vet at det på et tidspunkt kommer til å være mulig å fylle ut en meldekortdag for flere enn ett tiltak.
 * Da vil man kunne rename Meldekortdag til MeldekortdagForTiltak og wrappe den i en Meldekortdag(List<MeldekortdagForTiltak>)
 */
sealed interface MeldeperiodeBeregningDag {
    val dato: LocalDate
    val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær
    val tiltakstype: TiltakstypeSomGirRett?
    val beregningsdag: Beregningsdag?

    val beløp: Int get() = beregningsdag?.beløp ?: 0
    val beløpBarnetillegg: Int get() = beregningsdag?.beløpBarnetillegg ?: 0
    val prosent: Int get() = beregningsdag?.prosent ?: 0

    /** Begrenses av maksDagerMedTiltakspengerForPeriode (1-14) per meldeperiode og IKKE_RETT_TIL_TILTAKSPENGER. */
    val harDeltattEllerFravær: Boolean

    sealed interface Deltatt : MeldeperiodeBeregningDag {
        override val harDeltattEllerFravær get() = true

        data class DeltattUtenLønnITiltaket private constructor(
            override val dato: LocalDate,
            override val tiltakstype: TiltakstypeSomGirRett,
            override val beregningsdag: Beregningsdag,
        ) : Deltatt {
            override val reduksjon = IngenReduksjon

            companion object {
                fun create(
                    dato: LocalDate,
                    tiltakstype: TiltakstypeSomGirRett,
                    antallBarn: AntallBarn,
                ) = DeltattUtenLønnITiltaket(
                    dato,
                    tiltakstype,
                    beregnDag(dato, IngenReduksjon, antallBarn),
                )

                fun fromDb(
                    dato: LocalDate,
                    tiltakstype: TiltakstypeSomGirRett,
                    beregningsdag: Beregningsdag,
                ) = DeltattUtenLønnITiltaket(dato, tiltakstype, beregningsdag)
            }
        }

        data class DeltattMedLønnITiltaket private constructor(
            override val dato: LocalDate,
            override val tiltakstype: TiltakstypeSomGirRett,
            override val beregningsdag: Beregningsdag,
        ) : Deltatt {
            override val reduksjon = YtelsenFallerBort

            companion object {
                fun create(
                    dato: LocalDate,
                    tiltakstype: TiltakstypeSomGirRett,
                    antallBarn: AntallBarn,
                ) = DeltattMedLønnITiltaket(
                    dato,
                    tiltakstype,
                    beregnDag(dato, YtelsenFallerBort, antallBarn),
                )

                fun fromDb(
                    dato: LocalDate,
                    tiltakstype: TiltakstypeSomGirRett,
                    beregningsdag: Beregningsdag,
                ) = DeltattMedLønnITiltaket(dato, tiltakstype, beregningsdag)
            }
        }
    }

    data class IkkeDeltatt private constructor(
        override val dato: LocalDate,
        override val tiltakstype: TiltakstypeSomGirRett,
        override val beregningsdag: Beregningsdag,
    ) : MeldeperiodeBeregningDag {
        override val reduksjon = YtelsenFallerBort
        override val harDeltattEllerFravær = false

        companion object {
            fun create(
                dato: LocalDate,
                tiltakstype: TiltakstypeSomGirRett,
                antallBarn: AntallBarn,
            ) = IkkeDeltatt(dato, tiltakstype, beregnDag(dato, YtelsenFallerBort, antallBarn))

            fun fromDb(
                dato: LocalDate,
                tiltakstype: TiltakstypeSomGirRett,
                beregningsdag: Beregningsdag,
            ) = IkkeDeltatt(dato, tiltakstype, beregningsdag)
        }
    }

    data class IkkeBesvart private constructor(
        override val dato: LocalDate,
        override val tiltakstype: TiltakstypeSomGirRett,
        override val beregningsdag: Beregningsdag,
    ) : MeldeperiodeBeregningDag {
        override val reduksjon = YtelsenFallerBort
        override val harDeltattEllerFravær = false

        companion object {
            fun create(
                dato: LocalDate,
                tiltakstype: TiltakstypeSomGirRett,
                antallBarn: AntallBarn,
            ) = IkkeBesvart(dato, tiltakstype, beregnDag(dato, YtelsenFallerBort, antallBarn))

            fun fromDb(
                dato: LocalDate,
                tiltakstype: TiltakstypeSomGirRett,
                beregningsdag: Beregningsdag,
            ) = IkkeBesvart(dato, tiltakstype, beregningsdag)
        }
    }

    sealed interface Fravær : MeldeperiodeBeregningDag {
        override val harDeltattEllerFravær get() = true

        /**
         * @property reduksjon I tilfellet syk bruker/barn, gjøres det en utregning på tvers av meldekort basert på egenmeldingsdager, sykedager og karantene.
         */
        sealed interface Syk : Fravær {
            override val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær
            override val beregningsdag: Beregningsdag

            data class SykBruker private constructor(
                override val dato: LocalDate,
                override val tiltakstype: TiltakstypeSomGirRett,
                override val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                override val beregningsdag: Beregningsdag,
            ) : Syk {
                companion object {
                    fun create(
                        dato: LocalDate,
                        reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                        tiltakstype: TiltakstypeSomGirRett,
                        antallBarn: AntallBarn,
                    ) = SykBruker(
                        dato,
                        tiltakstype,
                        reduksjon,
                        beregnDag(dato, reduksjon, antallBarn),
                    )

                    fun fromDb(
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                        reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                        beregningsdag: Beregningsdag,
                    ) = SykBruker(dato, tiltakstype, reduksjon, beregningsdag)
                }
            }

            data class SyktBarn private constructor(
                override val dato: LocalDate,
                override val tiltakstype: TiltakstypeSomGirRett,
                override val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                override val beregningsdag: Beregningsdag,

            ) : Syk {
                companion object {
                    fun create(
                        dag: LocalDate,
                        reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                        tiltakstype: TiltakstypeSomGirRett,
                        antallBarn: AntallBarn,
                    ) = SyktBarn(
                        dag,
                        tiltakstype,
                        reduksjon,
                        beregnDag(dag, reduksjon, antallBarn),
                    )

                    fun fromDb(
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                        reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                        beregningsdag: Beregningsdag,
                    ) = SyktBarn(dato, tiltakstype, reduksjon, beregningsdag)
                }
            }
        }

        sealed interface Velferd : Fravær {
            data class FraværGodkjentAvNav private constructor(
                override val dato: LocalDate,
                override val tiltakstype: TiltakstypeSomGirRett,
                override val beregningsdag: Beregningsdag,
            ) : Velferd {
                override val reduksjon = IngenReduksjon

                companion object {
                    fun create(
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                        antallBarn: AntallBarn,
                    ) = FraværGodkjentAvNav(
                        dato,
                        tiltakstype,
                        beregnDag(dato, IngenReduksjon, antallBarn),
                    )

                    fun fromDb(
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                        beregningsdag: Beregningsdag,
                    ) = FraværGodkjentAvNav(dato, tiltakstype, beregningsdag)
                }
            }

            data class FraværAnnet private constructor(
                override val dato: LocalDate,
                override val tiltakstype: TiltakstypeSomGirRett,
                override val beregningsdag: Beregningsdag,
            ) : Velferd {
                override val reduksjon = YtelsenFallerBort

                companion object {
                    fun create(
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                        antallBarn: AntallBarn,
                    ) = FraværAnnet(
                        dato,
                        tiltakstype,
                        beregnDag(dato, YtelsenFallerBort, antallBarn),
                    )

                    fun fromDb(
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                        beregningsdag: Beregningsdag,
                    ) = FraværAnnet(dato, tiltakstype, beregningsdag)
                }
            }
        }
    }

    /**
     * En meldekortdag bruker ikke får mulighet til å fylle ut.
     * Gjelder for disse tilfellene:
     * 1. Første del av første meldekort i en sak.
     * 1. Siste del av siste meldekort i en sak.
     * 1. Andre dager bruker ikke får melde pga. vilkårsvurderingen. Delvis innvilget. Dette vil ikke gjelde MVP.
     */
    data class IkkeRettTilTiltakspenger(override val dato: LocalDate) : MeldeperiodeBeregningDag {
        override val tiltakstype = null
        override val reduksjon = YtelsenFallerBort
        override val harDeltattEllerFravær = false
        override val beregningsdag = null
    }
}
