package no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett

enum class TiltakstypeSomGirRettDTO {
    ARBEIDSFORBEREDENDE_TRENING,
    ARBEIDSRETTET_REHABILITERING,
    ARBEIDSTRENING,
    AVKLARING,
    DIGITAL_JOBBKLUBB,
    ENKELTPLASS_AMO,
    ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG,
    FORSØK_OPPLÆRING_LENGRE_VARIGHET,
    GRUPPE_AMO,
    GRUPPE_VGS_OG_HØYERE_YRKESFAG,
    HØYERE_UTDANNING,
    INDIVIDUELL_JOBBSTØTTE,
    INDIVIDUELL_KARRIERESTØTTE_UNG,
    JOBBKLUBB,
    OPPFØLGING,
    UTVIDET_OPPFØLGING_I_NAV,
    UTVIDET_OPPFØLGING_I_OPPLÆRING,
}

fun TiltakstypeSomGirRett.toDTO(): TiltakstypeSomGirRettDTO =
    when (this) {
        TiltakstypeSomGirRett.ARBEIDSFORBEREDENDE_TRENING -> TiltakstypeSomGirRettDTO.ARBEIDSFORBEREDENDE_TRENING
        TiltakstypeSomGirRett.ARBEIDSRETTET_REHABILITERING -> TiltakstypeSomGirRettDTO.ARBEIDSRETTET_REHABILITERING
        TiltakstypeSomGirRett.ARBEIDSTRENING -> TiltakstypeSomGirRettDTO.ARBEIDSTRENING
        TiltakstypeSomGirRett.AVKLARING -> TiltakstypeSomGirRettDTO.AVKLARING
        TiltakstypeSomGirRett.DIGITAL_JOBBKLUBB -> TiltakstypeSomGirRettDTO.DIGITAL_JOBBKLUBB
        TiltakstypeSomGirRett.ENKELTPLASS_AMO -> TiltakstypeSomGirRettDTO.ENKELTPLASS_AMO
        TiltakstypeSomGirRett.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> TiltakstypeSomGirRettDTO.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG
        TiltakstypeSomGirRett.FORSØK_OPPLÆRING_LENGRE_VARIGHET -> TiltakstypeSomGirRettDTO.FORSØK_OPPLÆRING_LENGRE_VARIGHET
        TiltakstypeSomGirRett.GRUPPE_AMO -> TiltakstypeSomGirRettDTO.GRUPPE_AMO
        TiltakstypeSomGirRett.GRUPPE_VGS_OG_HØYERE_YRKESFAG -> TiltakstypeSomGirRettDTO.GRUPPE_VGS_OG_HØYERE_YRKESFAG
        TiltakstypeSomGirRett.HØYERE_UTDANNING -> TiltakstypeSomGirRettDTO.HØYERE_UTDANNING
        TiltakstypeSomGirRett.INDIVIDUELL_JOBBSTØTTE -> TiltakstypeSomGirRettDTO.INDIVIDUELL_JOBBSTØTTE
        TiltakstypeSomGirRett.INDIVIDUELL_KARRIERESTØTTE_UNG -> TiltakstypeSomGirRettDTO.INDIVIDUELL_KARRIERESTØTTE_UNG
        TiltakstypeSomGirRett.JOBBKLUBB -> TiltakstypeSomGirRettDTO.JOBBKLUBB
        TiltakstypeSomGirRett.OPPFØLGING -> TiltakstypeSomGirRettDTO.OPPFØLGING
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_NAV -> TiltakstypeSomGirRettDTO.UTVIDET_OPPFØLGING_I_NAV
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_OPPLÆRING -> TiltakstypeSomGirRettDTO.UTVIDET_OPPFØLGING_I_OPPLÆRING
    }
