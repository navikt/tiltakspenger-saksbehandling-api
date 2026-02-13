@file:Suppress("unused")

package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Arbeidsmarkedsloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Folketrygdloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Foreldelsesloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Forvaltningsloven
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde.Tiltakspengeforskriften

enum class Hjemmel(
    val paragraf: Paragraf,
    val rettskilde: Rettskilde,
    val ledd: Ledd? = null,
) {
    ARBEIDSMARKEDSLOVEN_2("2", Arbeidsmarkedsloven),
    ARBEIDSMARKEDSLOVEN_12("12", Arbeidsmarkedsloven),
    ARBEIDSMARKEDSLOVEN_13("13", Arbeidsmarkedsloven),
    ARBEIDSMARKEDSLOVEN_13_L1("13", Arbeidsmarkedsloven, Ledd(1)),
    ARBEIDSMARKEDSLOVEN_13_L4("13", Arbeidsmarkedsloven, Ledd(4)),
    ARBEIDSMARKEDSLOVEN_15("15", Arbeidsmarkedsloven),
    ARBEIDSMARKEDSLOVEN_17("17", Arbeidsmarkedsloven),
    ARBEIDSMARKEDSLOVEN_19("19", Arbeidsmarkedsloven),
    ARBEIDSMARKEDSLOVEN_22("22", Arbeidsmarkedsloven),
    ARBEIDSMARKEDSLOVEN_28("28", Arbeidsmarkedsloven),

    FOLKETRYGDLOVEN_22_15("22-15", Folketrygdloven),
    FOLKETRYGDLOVEN_22_15_A("22-15 a.", Folketrygdloven),
    FOLKETRYGDLOVEN_22_15_B("22-15 b.", Folketrygdloven),
    FOLKETRYGDLOVEN_22_15_C("22-15 c.", Folketrygdloven),
    FOLKETRYGDLOVEN_22_15_D("22-15 d.", Folketrygdloven),
    FOLKETRYGDLOVEN_22_15_E("22-15 e.", Folketrygdloven),
    FOLKETRYGDLOVEN_22_15_F("22-15 f.", Folketrygdloven),
    FOLKETRYGDLOVEN_22_17_A("22-17 a.", Folketrygdloven),

    FORELDELSESLOVEN_2("2", Foreldelsesloven),
    FORELDELSESLOVEN_3("3", Foreldelsesloven),
    FORELDELSESLOVEN_10("10", Foreldelsesloven),
    FORELDELSESLOVEN_16("16", Foreldelsesloven),
    FORELDELSESLOVEN_17("17", Foreldelsesloven),
    FORELDELSESLOVEN_21("21", Foreldelsesloven),
    FORELDELSESLOVEN_28("28", Foreldelsesloven),

    FORVALTNINGSLOVEN_11("11", Forvaltningsloven),
    FORVALTNINGSLOVEN_12("12", Forvaltningsloven),
    FORVALTNINGSLOVEN_14("14", Forvaltningsloven),
    FORVALTNINGSLOVEN_16("16", Forvaltningsloven),
    FORVALTNINGSLOVEN_17("17", Forvaltningsloven),
    FORVALTNINGSLOVEN_18("18", Forvaltningsloven),
    FORVALTNINGSLOVEN_19("19", Forvaltningsloven),
    FORVALTNINGSLOVEN_21("21", Forvaltningsloven),
    FORVALTNINGSLOVEN_24("24", Forvaltningsloven),
    FORVALTNINGSLOVEN_25("25", Forvaltningsloven),
    FORVALTNINGSLOVEN_28("28", Forvaltningsloven),
    FORVALTNINGSLOVEN_29("29", Forvaltningsloven),
    FORVALTNINGSLOVEN_30("30", Forvaltningsloven),
    FORVALTNINGSLOVEN_31("31", Forvaltningsloven),
    FORVALTNINGSLOVEN_32("32", Forvaltningsloven),
    FORVALTNINGSLOVEN_33("33", Forvaltningsloven),
    FORVALTNINGSLOVEN_35("35", Forvaltningsloven),
    FORVALTNINGSLOVEN_41("41", Forvaltningsloven),
    FORVALTNINGSLOVEN_42("42", Forvaltningsloven),

    TILTAKSPENGEFORSKRIFTEN_2("2", Tiltakspengeforskriften),
    TILTAKSPENGEFORSKRIFTEN_3("3", Tiltakspengeforskriften),
    TILTAKSPENGEFORSKRIFTEN_5("5", Tiltakspengeforskriften),
    TILTAKSPENGEFORSKRIFTEN_6("6", Tiltakspengeforskriften),
    TILTAKSPENGEFORSKRIFTEN_7("7", Tiltakspengeforskriften),
    TILTAKSPENGEFORSKRIFTEN_7_L1("7", Tiltakspengeforskriften, Ledd(1)),
    TILTAKSPENGEFORSKRIFTEN_7_L3("7", Tiltakspengeforskriften, Ledd(3)),
    TILTAKSPENGEFORSKRIFTEN_8("8", Tiltakspengeforskriften),
    TILTAKSPENGEFORSKRIFTEN_8_L2("8", Tiltakspengeforskriften, Ledd(2)),
    TILTAKSPENGEFORSKRIFTEN_9("9", Tiltakspengeforskriften),
    TILTAKSPENGEFORSKRIFTEN_10("10", Tiltakspengeforskriften),
    TILTAKSPENGEFORSKRIFTEN_11("11", Tiltakspengeforskriften),
    ;

    // dummy-parameteren er en liten hack for å unngå Platform declaration clash
    constructor(paragraf: String, forskrift: Rettskilde, ledd: Ledd? = null, dummy: Nothing? = null) : this(Paragraf(paragraf), forskrift, ledd)
}
