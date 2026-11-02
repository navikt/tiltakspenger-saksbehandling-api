package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO

fun TiltakstypeSomGirRettDTO.tilTiltakstypeDTO(): TiltakResponsDTO.TiltakTypeDTO {
    return when (this) {
        TiltakstypeSomGirRettDTO.ARBEIDSFORBEREDENDE_TRENING -> TiltakResponsDTO.TiltakTypeDTO.ARBFORB
        TiltakstypeSomGirRettDTO.ARBEIDSRETTET_REHABILITERING -> TiltakResponsDTO.TiltakTypeDTO.ARBRRHDAG
        TiltakstypeSomGirRettDTO.ARBEIDSTRENING -> TiltakResponsDTO.TiltakTypeDTO.ARBTREN
        TiltakstypeSomGirRettDTO.AVKLARING -> TiltakResponsDTO.TiltakTypeDTO.AVKLARAG
        TiltakstypeSomGirRettDTO.DIGITAL_JOBBKLUBB -> TiltakResponsDTO.TiltakTypeDTO.DIGIOPPARB
        TiltakstypeSomGirRettDTO.ENKELTPLASS_AMO -> TiltakResponsDTO.TiltakTypeDTO.ENKELAMO
        TiltakstypeSomGirRettDTO.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakTypeDTO.ENKFAGYRKE
        TiltakstypeSomGirRettDTO.FORSØK_OPPLÆRING_LENGRE_VARIGHET -> TiltakResponsDTO.TiltakTypeDTO.FORSOPPLEV
        TiltakstypeSomGirRettDTO.GRUPPE_AMO -> TiltakResponsDTO.TiltakTypeDTO.GRUPPEAMO
        TiltakstypeSomGirRettDTO.GRUPPE_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakTypeDTO.GRUFAGYRKE
        TiltakstypeSomGirRettDTO.HØYERE_UTDANNING -> TiltakResponsDTO.TiltakTypeDTO.HOYEREUTD
        TiltakstypeSomGirRettDTO.INDIVIDUELL_JOBBSTØTTE -> TiltakResponsDTO.TiltakTypeDTO.INDJOBSTOT
        TiltakstypeSomGirRettDTO.INDIVIDUELL_KARRIERESTØTTE_UNG -> TiltakResponsDTO.TiltakTypeDTO.IPSUNG
        TiltakstypeSomGirRettDTO.JOBBKLUBB -> TiltakResponsDTO.TiltakTypeDTO.JOBBK
        TiltakstypeSomGirRettDTO.OPPFØLGING -> TiltakResponsDTO.TiltakTypeDTO.INDOPPFAG
        TiltakstypeSomGirRettDTO.UTVIDET_OPPFØLGING_I_NAV -> TiltakResponsDTO.TiltakTypeDTO.UTVAOONAV
        TiltakstypeSomGirRettDTO.UTVIDET_OPPFØLGING_I_OPPLÆRING -> TiltakResponsDTO.TiltakTypeDTO.UTVOPPFOPL
        TiltakstypeSomGirRettDTO.ARBEIDSMARKEDSOPPLAERING -> TiltakResponsDTO.TiltakTypeDTO.ARBEIDSMARKEDSOPPLAERING
        TiltakstypeSomGirRettDTO.FAG_OG_YRKESOPPLAERING -> TiltakResponsDTO.TiltakTypeDTO.FAG_OG_YRKESOPPLAERING
        TiltakstypeSomGirRettDTO.HOYERE_YRKESFAGLIG_UTDANNING -> TiltakResponsDTO.TiltakTypeDTO.HOYERE_YRKESFAGLIG_UTDANNING
        TiltakstypeSomGirRettDTO.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> TiltakResponsDTO.TiltakTypeDTO.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV
        TiltakstypeSomGirRettDTO.STUDIESPESIALISERING -> TiltakResponsDTO.TiltakTypeDTO.STUDIESPESIALISERING
    }
}
