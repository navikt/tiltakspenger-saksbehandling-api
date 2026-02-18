@file:Suppress("unused", "ktlint")

package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Klagehjemmel.KlageArbeidsmarkedsloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Klagehjemmel.KlageForeldelsesloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Klagehjemmel.KlageForvaltningsloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Klagehjemmel.KlageTiltakspengeforskriften
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Arbeidsmarkedsloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Folketrygdloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Foreldelsesloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Forvaltningsloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Tiltakspengeforskriften

sealed interface Hjemmel {
    val paragraf: Paragraf
    val rettskilde: Rettskilde
    val ledd: Ledd? get() = null
    val punktum: Int? get() = null

    sealed interface ArbeidsmarkedslovenHjemmel : Hjemmel {
        override val rettskilde: Rettskilde get() = Arbeidsmarkedsloven

        data object ARBEIDSMARKEDSLOVEN_2 : ArbeidsmarkedslovenHjemmel, KlageArbeidsmarkedsloven {
            override val paragraf = Paragraf("2")
        }

        data object ARBEIDSMARKEDSLOVEN_12 : ArbeidsmarkedslovenHjemmel {
            override val paragraf = Paragraf("12")
        }

        data object ARBEIDSMARKEDSLOVEN_13 : ArbeidsmarkedslovenHjemmel, KlageArbeidsmarkedsloven {
            override val paragraf = Paragraf("13")
        }

        /** Henviser kun til første leddsetningen av 1. punktum i paragraf 13: Deltakere i arbeidsmarkedstiltak kan få tiltakspenger dersom de ikke mottar lønn fra tiltaksarrangør eller har rett til å få dekket utgifter til livsopphold på annen måte. */
        data object ARBEIDSMARKEDSLOVEN_13_LØNN : ArbeidsmarkedslovenHjemmel, KlageArbeidsmarkedsloven {
            override val paragraf = Paragraf("13")
            override val ledd = Ledd(1)
            override val punktum = 1
        }

        data object ARBEIDSMARKEDSLOVEN_13_L1 : ArbeidsmarkedslovenHjemmel {
            override val paragraf = Paragraf("13")
            override val ledd = Ledd(1)
        }

        data object ARBEIDSMARKEDSLOVEN_13_L4 : ArbeidsmarkedslovenHjemmel, KlageArbeidsmarkedsloven {
            override val paragraf = Paragraf("13")
            override val ledd = Ledd(4)
        }

        data object ARBEIDSMARKEDSLOVEN_15 : ArbeidsmarkedslovenHjemmel, KlageArbeidsmarkedsloven {
            override val paragraf = Paragraf("15")
        }

        data object ARBEIDSMARKEDSLOVEN_17 : ArbeidsmarkedslovenHjemmel, KlageArbeidsmarkedsloven {
            override val paragraf = Paragraf("17")
        }

        data object ARBEIDSMARKEDSLOVEN_19 : ArbeidsmarkedslovenHjemmel {
            override val paragraf = Paragraf("19")
        }

        data object ARBEIDSMARKEDSLOVEN_22 : ArbeidsmarkedslovenHjemmel, KlageArbeidsmarkedsloven {
            override val paragraf = Paragraf("22")
        }

        data object ARBEIDSMARKEDSLOVEN_28 : ArbeidsmarkedslovenHjemmel {
            override val paragraf = Paragraf("28")
        }
    }

    sealed interface FolketrygdlovenHjemmel : Hjemmel {
        override val rettskilde: Rettskilde get() = Folketrygdloven

        data object FOLKETRYGDLOVEN_22_15 : FolketrygdlovenHjemmel, Klagehjemmel.KlageFolketrygdloven {
            override val paragraf = Paragraf("22-15")
        }

        data object FOLKETRYGDLOVEN_22_15_A : FolketrygdlovenHjemmel {
            override val paragraf = Paragraf("22-15 a.")
        }

        data object FOLKETRYGDLOVEN_22_15_B : FolketrygdlovenHjemmel {
            override val paragraf = Paragraf("22-15 b.")
        }

        data object FOLKETRYGDLOVEN_22_15_C : FolketrygdlovenHjemmel {
            override val paragraf = Paragraf("22-15 c.")
        }

        data object FOLKETRYGDLOVEN_22_15_D : FolketrygdlovenHjemmel {
            override val paragraf = Paragraf("22-15 d.")
        }

        data object FOLKETRYGDLOVEN_22_15_E : FolketrygdlovenHjemmel {
            override val paragraf = Paragraf("22-15 e.")
        }

        data object FOLKETRYGDLOVEN_22_15_F : FolketrygdlovenHjemmel {
            override val paragraf = Paragraf("22-15 f.")
        }

        data object FOLKETRYGDLOVEN_22_17_A : FolketrygdlovenHjemmel, Klagehjemmel.KlageFolketrygdloven {
            override val paragraf = Paragraf("22-17 a.")
        }
    }

    sealed interface ForeldelseslovenHjemmel : Hjemmel {
        override val rettskilde: Rettskilde get() = Foreldelsesloven

        data object FORELDELSESLOVEN_2 : ForeldelseslovenHjemmel {
            override val paragraf = Paragraf("2")
        }

        /** Disse kommer i følge Kabalteamet alltid i par. Vi modellerer det inn mtp. klagebehandling */
        data object FORELDELSESLOVEN_2_OG_3 : ForeldelseslovenHjemmel, KlageForeldelsesloven {
            override val paragraf = Paragraf("2 og 3")
        }

        data object FORELDELSESLOVEN_3 : ForeldelseslovenHjemmel {
            override val paragraf = Paragraf("3")
        }

        data object FORELDELSESLOVEN_10 : ForeldelseslovenHjemmel, KlageForeldelsesloven {
            override val paragraf = Paragraf("10")
        }

        data object FORELDELSESLOVEN_16 : ForeldelseslovenHjemmel {
            override val paragraf = Paragraf("16")
        }

        data object FORELDELSESLOVEN_17 : ForeldelseslovenHjemmel {
            override val paragraf = Paragraf("17")
        }

        data object FORELDELSESLOVEN_21 : ForeldelseslovenHjemmel {
            override val paragraf = Paragraf("21")
        }

        data object FORELDELSESLOVEN_28 : ForeldelseslovenHjemmel {
            override val paragraf = Paragraf("28")
        }
    }

    sealed interface ForvaltningslovenHjemmel : Hjemmel {
        override val rettskilde: Rettskilde get() = Forvaltningsloven

        data object FORVALTNINGSLOVEN_11 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("11")
        }

        data object FORVALTNINGSLOVEN_12 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("12")
        }

        data object FORVALTNINGSLOVEN_14 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("14")
        }

        data object FORVALTNINGSLOVEN_16 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("16")
        }

        data object FORVALTNINGSLOVEN_17 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("17")
        }

        data object FORVALTNINGSLOVEN_18 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("18")
        }

        /** Disse kommer i følge Kabalteamet alltid i par. Vi modellerer det inn mtp. klagebehandling */
        data object FORVALTNINGSLOVEN_18_OG_19 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("18 og 19")
        }

        data object FORVALTNINGSLOVEN_19 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("19")
        }

        data object FORVALTNINGSLOVEN_21 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("21")
        }

        data object FORVALTNINGSLOVEN_24 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("24")
        }

        data object FORVALTNINGSLOVEN_25 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("25")
        }

        data object FORVALTNINGSLOVEN_28 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("28")
        }

        data object FORVALTNINGSLOVEN_29 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("29")
        }

        data object FORVALTNINGSLOVEN_30 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("30")
        }

        data object FORVALTNINGSLOVEN_31 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("31")
        }

        data object FORVALTNINGSLOVEN_32 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("32")
        }

        data object FORVALTNINGSLOVEN_33 : ForvaltningslovenHjemmel {
            override val paragraf = Paragraf("33")
        }

        data object FORVALTNINGSLOVEN_35 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("35")
        }

        data object FORVALTNINGSLOVEN_41 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("41")
        }

        data object FORVALTNINGSLOVEN_42 : ForvaltningslovenHjemmel, KlageForvaltningsloven {
            override val paragraf = Paragraf("42")
        }
    }

    sealed interface TiltakspengeforskriftenHjemmel : Hjemmel {
        override val rettskilde: Rettskilde get() = Tiltakspengeforskriften

        data object TILTAKSPENGEFORSKRIFTEN_2 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("2")
        }

        data object TILTAKSPENGEFORSKRIFTEN_3 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("3")
        }

        data object TILTAKSPENGEFORSKRIFTEN_5 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("5")
        }

        data object TILTAKSPENGEFORSKRIFTEN_6 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("6")
        }

        data object TILTAKSPENGEFORSKRIFTEN_7 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("7")
        }

        data object TILTAKSPENGEFORSKRIFTEN_7_L1 : TiltakspengeforskriftenHjemmel {
            override val paragraf = Paragraf("7")
            override val ledd = Ledd(1)
        }

        data object TILTAKSPENGEFORSKRIFTEN_7_L3 : TiltakspengeforskriftenHjemmel {
            override val paragraf = Paragraf("7")
            override val ledd = Ledd(3)
        }

        data object TILTAKSPENGEFORSKRIFTEN_8 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("8")
        }

        data object TILTAKSPENGEFORSKRIFTEN_8_L2 : TiltakspengeforskriftenHjemmel {
            override val paragraf = Paragraf("8")
            override val ledd = Ledd(2)
        }

        data object TILTAKSPENGEFORSKRIFTEN_9 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("9")
        }

        data object TILTAKSPENGEFORSKRIFTEN_10 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("10")
        }

        data object TILTAKSPENGEFORSKRIFTEN_11 : TiltakspengeforskriftenHjemmel, KlageTiltakspengeforskriften {
            override val paragraf = Paragraf("11")
        }
    }

}
