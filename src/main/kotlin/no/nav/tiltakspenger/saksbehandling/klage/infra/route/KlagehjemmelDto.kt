package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ArbeidsmarkedslovenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.FolketrygdlovenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ForeldelseslovenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.ForvaltningslovenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel.TiltakspengeforskriftenHjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Klagehjemmel

enum class KlagehjemmelDto {
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

    fun toKlagehjemmel(): Klagehjemmel = when (this) {
        ARBEIDSMARKEDSLOVEN_2 -> ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_2
        ARBEIDSMARKEDSLOVEN_13 -> ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13
        ARBEIDSMARKEDSLOVEN_13_LØNN -> ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_LØNN
        ARBEIDSMARKEDSLOVEN_13_L4 -> ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_L4
        ARBEIDSMARKEDSLOVEN_15 -> ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_15
        ARBEIDSMARKEDSLOVEN_17 -> ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_17
        ARBEIDSMARKEDSLOVEN_22 -> ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_22
        FOLKETRYGDLOVEN_22_15 -> FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_15
        FOLKETRYGDLOVEN_22_17_A -> FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_17_A
        FORELDELSESLOVEN_10 -> ForeldelseslovenHjemmel.FORELDELSESLOVEN_10
        FORELDELSESLOVEN_2_OG_3 -> ForeldelseslovenHjemmel.FORELDELSESLOVEN_2_OG_3
        FORVALTNINGSLOVEN_11 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_11
        FORVALTNINGSLOVEN_17 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_17
        FORVALTNINGSLOVEN_18_OG_19 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_18_OG_19
        FORVALTNINGSLOVEN_28 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_28
        FORVALTNINGSLOVEN_30 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_30
        FORVALTNINGSLOVEN_31 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_31
        FORVALTNINGSLOVEN_32 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_32
        FORVALTNINGSLOVEN_35 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_35
        FORVALTNINGSLOVEN_41 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_41
        FORVALTNINGSLOVEN_42 -> ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_42
        TILTAKSPENGEFORSKRIFTEN_2 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_2
        TILTAKSPENGEFORSKRIFTEN_3 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3
        TILTAKSPENGEFORSKRIFTEN_5 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_5
        TILTAKSPENGEFORSKRIFTEN_6 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_6
        TILTAKSPENGEFORSKRIFTEN_7 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_7
        TILTAKSPENGEFORSKRIFTEN_8 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_8
        TILTAKSPENGEFORSKRIFTEN_9 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_9
        TILTAKSPENGEFORSKRIFTEN_10 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_10
        TILTAKSPENGEFORSKRIFTEN_11 -> TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_11
    }

    companion object {
        fun Klagehjemmel.toKlagehjemmelDto(): KlagehjemmelDto = when (this) {
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13 -> ARBEIDSMARKEDSLOVEN_13
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_LØNN -> ARBEIDSMARKEDSLOVEN_13_LØNN
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13_L4 -> ARBEIDSMARKEDSLOVEN_13_L4
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_15 -> ARBEIDSMARKEDSLOVEN_15
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_17 -> ARBEIDSMARKEDSLOVEN_17
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_2 -> ARBEIDSMARKEDSLOVEN_2
            ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_22 -> ARBEIDSMARKEDSLOVEN_22
            FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_15 -> FOLKETRYGDLOVEN_22_15
            FolketrygdlovenHjemmel.FOLKETRYGDLOVEN_22_17_A -> FOLKETRYGDLOVEN_22_17_A
            ForeldelseslovenHjemmel.FORELDELSESLOVEN_10 -> FORELDELSESLOVEN_10
            ForeldelseslovenHjemmel.FORELDELSESLOVEN_2_OG_3 -> FORELDELSESLOVEN_2_OG_3
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_11 -> FORVALTNINGSLOVEN_11
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_17 -> FORVALTNINGSLOVEN_17
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_18_OG_19 -> FORVALTNINGSLOVEN_18_OG_19
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_28 -> FORVALTNINGSLOVEN_28
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_30 -> FORVALTNINGSLOVEN_30
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_31 -> FORVALTNINGSLOVEN_31
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_32 -> FORVALTNINGSLOVEN_32
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_35 -> FORVALTNINGSLOVEN_35
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_41 -> FORVALTNINGSLOVEN_41
            ForvaltningslovenHjemmel.FORVALTNINGSLOVEN_42 -> FORVALTNINGSLOVEN_42
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_2 -> TILTAKSPENGEFORSKRIFTEN_2
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3 -> TILTAKSPENGEFORSKRIFTEN_3
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_5 -> TILTAKSPENGEFORSKRIFTEN_5
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_6 -> TILTAKSPENGEFORSKRIFTEN_6
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_7 -> TILTAKSPENGEFORSKRIFTEN_7
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_8 -> TILTAKSPENGEFORSKRIFTEN_8
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_9 -> TILTAKSPENGEFORSKRIFTEN_9
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_10 -> TILTAKSPENGEFORSKRIFTEN_10
            TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_11 -> TILTAKSPENGEFORSKRIFTEN_11
        }

        fun Set<Klagehjemmel>.toKlagehjemmelDto(): List<KlagehjemmelDto> = this.map { it.toKlagehjemmelDto() }
    }
}
