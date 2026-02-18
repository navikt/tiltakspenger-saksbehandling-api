package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import arrow.core.toNonEmptySetOrThrow
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Klagehjemmel
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagehjemler

enum class KlagehjemmelDb {
    ARBEIDSMARKEDSLOVEN_2,
    ARBEIDSMARKEDSLOVEN_13,
    ARBEIDSMARKEDSLOVEN_13_LØNN,
    ARBEIDSMARKEDSLOVEN_13_L4,
    ARBEIDSMARKEDSLOVEN_15,
    ARBEIDSMARKEDSLOVEN_17,
    ARBEIDSMARKEDSLOVEN_22,

    FOLKETRYGDLOVEN_22_15,
    FOLKETRYGDLOVEN_22_17_A,

    FORELDELSESLOVEN_10,
    FORELDELSESLOVEN_2_OG_3,

    FORVALTNINGSLOVEN_11,
    FORVALTNINGSLOVEN_17,
    FORVALTNINGSLOVEN_18_OG_19,
    FORVALTNINGSLOVEN_28,
    FORVALTNINGSLOVEN_30,
    FORVALTNINGSLOVEN_31,
    FORVALTNINGSLOVEN_32,
    FORVALTNINGSLOVEN_35,
    FORVALTNINGSLOVEN_41,
    FORVALTNINGSLOVEN_42,

    TILTAKSPENGEFORSKRIFTEN_2,
    TILTAKSPENGEFORSKRIFTEN_3,
    TILTAKSPENGEFORSKRIFTEN_5,
    TILTAKSPENGEFORSKRIFTEN_6,
    TILTAKSPENGEFORSKRIFTEN_7,
    TILTAKSPENGEFORSKRIFTEN_8,
    TILTAKSPENGEFORSKRIFTEN_9,
    TILTAKSPENGEFORSKRIFTEN_10,
    TILTAKSPENGEFORSKRIFTEN_11,
    ;

    fun toDomain(): Klagehjemmel = when (this) {
        ARBEIDSMARKEDSLOVEN_2 -> Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_2
        ARBEIDSMARKEDSLOVEN_13 -> Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13
        ARBEIDSMARKEDSLOVEN_13_LØNN -> Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_LØNN
        ARBEIDSMARKEDSLOVEN_13_L4 -> Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_L4
        ARBEIDSMARKEDSLOVEN_15 -> Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_15
        ARBEIDSMARKEDSLOVEN_17 -> Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_17
        ARBEIDSMARKEDSLOVEN_22 -> Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_22
        FOLKETRYGDLOVEN_22_15 -> Hjemmel.FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_15
        FOLKETRYGDLOVEN_22_17_A -> Hjemmel.FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_17_A
        FORELDELSESLOVEN_2_OG_3 -> Hjemmel.ForeldelseslovenHjemmel.FORELDELSESLOVEN_2_OG_3
        FORELDELSESLOVEN_10 -> Hjemmel.ForeldelseslovenHjemmel.FORELDELSESLOVEN_10
        FORVALTNINGSLOVEN_11 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_11
        FORVALTNINGSLOVEN_17 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_17
        FORVALTNINGSLOVEN_18_OG_19 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_18_OG_19
        FORVALTNINGSLOVEN_28 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_28
        FORVALTNINGSLOVEN_30 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_30
        FORVALTNINGSLOVEN_31 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_31
        FORVALTNINGSLOVEN_32 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_32
        FORVALTNINGSLOVEN_35 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_35
        FORVALTNINGSLOVEN_41 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_41
        FORVALTNINGSLOVEN_42 -> Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_42
        TILTAKSPENGEFORSKRIFTEN_2 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_2
        TILTAKSPENGEFORSKRIFTEN_3 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3
        TILTAKSPENGEFORSKRIFTEN_5 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_5
        TILTAKSPENGEFORSKRIFTEN_6 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_6
        TILTAKSPENGEFORSKRIFTEN_7 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_7
        TILTAKSPENGEFORSKRIFTEN_8 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_8
        TILTAKSPENGEFORSKRIFTEN_9 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_9
        TILTAKSPENGEFORSKRIFTEN_10 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_10
        TILTAKSPENGEFORSKRIFTEN_11 -> Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_11
    }

    companion object {
        fun Klagehjemmel.toDb(): KlagehjemmelDb = when (this) {
            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_2 -> ARBEIDSMARKEDSLOVEN_2
            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13 -> ARBEIDSMARKEDSLOVEN_13
            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_LØNN -> ARBEIDSMARKEDSLOVEN_13_LØNN
            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_L4 -> ARBEIDSMARKEDSLOVEN_13_L4
            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_15 -> ARBEIDSMARKEDSLOVEN_15
            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_17 -> ARBEIDSMARKEDSLOVEN_17
            Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_22 -> ARBEIDSMARKEDSLOVEN_22
            Hjemmel.FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_15 -> FOLKETRYGDLOVEN_22_15
            Hjemmel.FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_17_A -> FOLKETRYGDLOVEN_22_17_A
            Hjemmel.ForeldelseslovenHjemmel.FORELDELSESLOVEN_10 -> FORELDELSESLOVEN_10
            Hjemmel.ForeldelseslovenHjemmel.FORELDELSESLOVEN_2_OG_3 -> FORELDELSESLOVEN_2_OG_3
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_11 -> FORVALTNINGSLOVEN_11
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_17 -> FORVALTNINGSLOVEN_17
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_18_OG_19 -> FORVALTNINGSLOVEN_18_OG_19
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_28 -> FORVALTNINGSLOVEN_28
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_30 -> FORVALTNINGSLOVEN_30
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_31 -> FORVALTNINGSLOVEN_31
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_32 -> FORVALTNINGSLOVEN_32
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_35 -> FORVALTNINGSLOVEN_35
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_41 -> FORVALTNINGSLOVEN_41
            Hjemmel.ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_42 -> FORVALTNINGSLOVEN_42
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_2 -> TILTAKSPENGEFORSKRIFTEN_2
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3 -> TILTAKSPENGEFORSKRIFTEN_3
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_5 -> TILTAKSPENGEFORSKRIFTEN_5
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_6 -> TILTAKSPENGEFORSKRIFTEN_6
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_7 -> TILTAKSPENGEFORSKRIFTEN_7
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_8 -> TILTAKSPENGEFORSKRIFTEN_8
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_9 -> TILTAKSPENGEFORSKRIFTEN_9
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_10 -> TILTAKSPENGEFORSKRIFTEN_10
            Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_11 -> TILTAKSPENGEFORSKRIFTEN_11
        }

        fun List<Klagehjemmel>.toDb(): List<KlagehjemmelDb> = this.toList().map { it.toDb() }

        fun List<KlagehjemmelDb>.toDomain(): Klagehjemler =
            Klagehjemler(this.map { it.toDomain() }.toNonEmptySetOrThrow())
    }
}
