package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import java.time.LocalDate

/**
 * Når en bruker er på tiltak kan hen være 1-10 av 14 dager på tiltak. Dvs. minst 2 dager per uke må være Sperret eller IkkeDeltatt.
 *
 * Vi vet at det på et tidspunkt kommer til å være mulig å fylle ut en meldekortdag for flere enn ett tiltak. Da vil man kunne rename Meldekortdag til MeldekortdagForTiltak og wrappe den i en Meldekortdag(List<MeldekortdagForTiltak>)
 */

sealed interface Meldekortdag {
    val dato: LocalDate
    val meldekortId: MeldekortId
    val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær?
    val tiltakstype: TiltakstypeSomGirRett
    val beregningsdag: Beregningsdag?

    val beløp: Int get() = beregningsdag?.beløp ?: 0
    val prosent: Int get() = beregningsdag?.prosent ?: 0

    /** Begrenses av maksDagerMedTiltakspengerForPeriode (1-10) per meldeperiode og SPERRET. */
    val harDeltattEllerFravær: Boolean

    data class IkkeUtfylt(
        override val meldekortId: MeldekortId,
        override val dato: LocalDate,
        override val tiltakstype: TiltakstypeSomGirRett,
    ) : Meldekortdag {
        override val harDeltattEllerFravær = false
        override val reduksjon = null
        override val beregningsdag = null
    }

    sealed interface Utfylt : Meldekortdag {
        override val tiltakstype: TiltakstypeSomGirRett
        override val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær

        /** Begrenses av maksDagerMedTiltakspengerForPeriode (1-10) per meldeperiode og SPERRET. */
        override val harDeltattEllerFravær: Boolean

        sealed interface Deltatt : Utfylt {
            override val harDeltattEllerFravær get() = true

            data class DeltattUtenLønnITiltaket private constructor(
                override val meldekortId: MeldekortId,
                override val dato: LocalDate,
                override val tiltakstype: TiltakstypeSomGirRett,
                override val beregningsdag: Beregningsdag,
            ) : Deltatt {
                override val reduksjon = IngenReduksjon

                companion object {
                    fun create(
                        meldekortId: MeldekortId,
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                    ) = DeltattUtenLønnITiltaket(meldekortId, dato, tiltakstype, beregnDag(dato, IngenReduksjon))

                    fun fromDb(
                        meldekortId: MeldekortId,
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                        beregningsdag: Beregningsdag,
                    ) = DeltattUtenLønnITiltaket(meldekortId, dato, tiltakstype, beregningsdag)
                }
            }

            data class DeltattMedLønnITiltaket private constructor(
                override val meldekortId: MeldekortId,
                override val dato: LocalDate,
                override val tiltakstype: TiltakstypeSomGirRett,
                override val beregningsdag: Beregningsdag,
            ) : Deltatt {
                override val reduksjon = YtelsenFallerBort

                companion object {
                    fun create(
                        meldekortId: MeldekortId,
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                    ) = DeltattMedLønnITiltaket(meldekortId, dato, tiltakstype, beregnDag(dato, YtelsenFallerBort))

                    fun fromDb(
                        meldekortId: MeldekortId,
                        dato: LocalDate,
                        tiltakstype: TiltakstypeSomGirRett,
                        beregningsdag: Beregningsdag,
                    ) = DeltattMedLønnITiltaket(meldekortId, dato, tiltakstype, beregningsdag)
                }
            }
        }

        data class IkkeDeltatt private constructor(
            override val meldekortId: MeldekortId,
            override val dato: LocalDate,
            // TODO post-mvp: Siden vi bare støtter ett tiltak i MVP kan vi implisitt fylle ut denne. Dersom vi har flere tiltak, må bruker i så fall velge tiltaket han ikke har deltatt på og det er ikke sikkert det gir mening og vi tar bort feltet.
            override val tiltakstype: TiltakstypeSomGirRett,
            override val beregningsdag: Beregningsdag,
        ) : Utfylt {
            override val reduksjon = YtelsenFallerBort
            override val harDeltattEllerFravær = false

            companion object {
                fun create(
                    meldekortId: MeldekortId,
                    dato: LocalDate,
                    tiltakstype: TiltakstypeSomGirRett,
                ) = IkkeDeltatt(meldekortId, dato, tiltakstype, beregnDag(dato, YtelsenFallerBort))

                fun fromDb(
                    meldekortId: MeldekortId,
                    dato: LocalDate,
                    tiltakstype: TiltakstypeSomGirRett,
                    beregningsdag: Beregningsdag,
                ) = IkkeDeltatt(meldekortId, dato, tiltakstype, beregningsdag)
            }
        }

        sealed interface Fravær : Utfylt {
            override val harDeltattEllerFravær get() = true

            /**
             * @property reduksjon I tilfellet syk bruker/barn, gjøres det en utregning på tvers av meldekort basert på egenmeldingsdager, sykedager og karantene.
             */
            sealed interface Syk : Fravær {
                override val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær
                override val beregningsdag: Beregningsdag

                data class SykBruker private constructor(
                    override val meldekortId: MeldekortId,
                    override val dato: LocalDate,
                    override val tiltakstype: TiltakstypeSomGirRett,
                    override val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                    override val beregningsdag: Beregningsdag,
                ) : Syk {
                    companion object {
                        fun create(
                            meldekortId: MeldekortId,
                            dato: LocalDate,
                            reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                            tiltakstype: TiltakstypeSomGirRett,
                        ) = SykBruker(meldekortId, dato, tiltakstype, reduksjon, beregnDag(dato, reduksjon))

                        fun fromDb(
                            meldekortId: MeldekortId,
                            dato: LocalDate,
                            tiltakstype: TiltakstypeSomGirRett,
                            reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                            beregningsdag: Beregningsdag,
                        ) = SykBruker(meldekortId, dato, tiltakstype, reduksjon, beregningsdag)
                    }
                }

                data class SyktBarn private constructor(
                    override val meldekortId: MeldekortId,
                    override val dato: LocalDate,
                    override val tiltakstype: TiltakstypeSomGirRett,
                    override val reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                    override val beregningsdag: Beregningsdag,
                ) : Syk {
                    companion object {
                        fun create(
                            meldekortId: MeldekortId,
                            dato: LocalDate,
                            reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                            tiltakstype: TiltakstypeSomGirRett,
                        ) = SyktBarn(meldekortId, dato, tiltakstype, reduksjon, beregnDag(dato, reduksjon))

                        fun fromDb(
                            meldekortId: MeldekortId,
                            dato: LocalDate,
                            tiltakstype: TiltakstypeSomGirRett,
                            reduksjon: ReduksjonAvYtelsePåGrunnAvFravær,
                            beregningsdag: Beregningsdag,
                        ) = SyktBarn(meldekortId, dato, tiltakstype, reduksjon, beregningsdag)
                    }
                }
            }

            sealed interface Velferd : Fravær {
                data class VelferdGodkjentAvNav private constructor(
                    override val meldekortId: MeldekortId,
                    override val dato: LocalDate,
                    override val tiltakstype: TiltakstypeSomGirRett,
                    override val beregningsdag: Beregningsdag,
                ) : Velferd {
                    override val reduksjon = IngenReduksjon

                    companion object {
                        fun create(
                            meldekortId: MeldekortId,
                            dato: LocalDate,
                            tiltakstype: TiltakstypeSomGirRett,
                        ) = VelferdGodkjentAvNav(meldekortId, dato, tiltakstype, beregnDag(dato, IngenReduksjon))

                        fun fromDb(
                            meldekortId: MeldekortId,
                            dato: LocalDate,
                            tiltakstype: TiltakstypeSomGirRett,
                            beregningsdag: Beregningsdag,
                        ) = VelferdGodkjentAvNav(meldekortId, dato, tiltakstype, beregningsdag)
                    }
                }

                data class VelferdIkkeGodkjentAvNav private constructor(
                    override val meldekortId: MeldekortId,
                    override val dato: LocalDate,
                    override val tiltakstype: TiltakstypeSomGirRett,
                    override val beregningsdag: Beregningsdag,
                ) : Velferd {
                    override val reduksjon = YtelsenFallerBort

                    companion object {
                        fun create(
                            meldekortId: MeldekortId,
                            dato: LocalDate,
                            tiltakstype: TiltakstypeSomGirRett,
                        ) = VelferdIkkeGodkjentAvNav(meldekortId, dato, tiltakstype, beregnDag(dato, YtelsenFallerBort))

                        fun fromDb(
                            meldekortId: MeldekortId,
                            dato: LocalDate,
                            tiltakstype: TiltakstypeSomGirRett,
                            beregningsdag: Beregningsdag,
                        ) = VelferdIkkeGodkjentAvNav(meldekortId, dato, tiltakstype, beregningsdag)
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
        data class Sperret(
            override val meldekortId: MeldekortId,
            override val dato: LocalDate,
            override val tiltakstype: TiltakstypeSomGirRett,
        ) : Utfylt {
            override val reduksjon = YtelsenFallerBort
            override val harDeltattEllerFravær = false
            override val beregningsdag = null
        }
    }
}
