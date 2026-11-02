package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett

fun TiltakstypeSomGirRett.tilTiltakstypeDTO(): TiltakResponsDTO.TiltakTypeDTO {
    return when (this) {
        TiltakstypeSomGirRett.ARBEIDSFORBEREDENDE_TRENING -> TiltakResponsDTO.TiltakTypeDTO.ARBFORB
        TiltakstypeSomGirRett.ARBEIDSRETTET_REHABILITERING -> TiltakResponsDTO.TiltakTypeDTO.ARBRRHDAG
        TiltakstypeSomGirRett.ARBEIDSTRENING -> TiltakResponsDTO.TiltakTypeDTO.ARBTREN
        TiltakstypeSomGirRett.AVKLARING -> TiltakResponsDTO.TiltakTypeDTO.AVKLARAG
        TiltakstypeSomGirRett.DIGITAL_JOBBKLUBB -> TiltakResponsDTO.TiltakTypeDTO.DIGIOPPARB
        TiltakstypeSomGirRett.ENKELTPLASS_AMO -> TiltakResponsDTO.TiltakTypeDTO.ENKELAMO
        TiltakstypeSomGirRett.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakTypeDTO.ENKFAGYRKE
        TiltakstypeSomGirRett.FORSØK_OPPLÆRING_LENGRE_VARIGHET -> TiltakResponsDTO.TiltakTypeDTO.FORSOPPLEV
        TiltakstypeSomGirRett.GRUPPE_AMO -> TiltakResponsDTO.TiltakTypeDTO.GRUPPEAMO
        TiltakstypeSomGirRett.GRUPPE_VGS_OG_HØYERE_YRKESFAG -> TiltakResponsDTO.TiltakTypeDTO.GRUFAGYRKE
        TiltakstypeSomGirRett.HØYERE_UTDANNING -> TiltakResponsDTO.TiltakTypeDTO.HOYEREUTD
        TiltakstypeSomGirRett.INDIVIDUELL_JOBBSTØTTE -> TiltakResponsDTO.TiltakTypeDTO.INDJOBSTOT
        TiltakstypeSomGirRett.INDIVIDUELL_KARRIERESTØTTE_UNG -> TiltakResponsDTO.TiltakTypeDTO.IPSUNG
        TiltakstypeSomGirRett.JOBBKLUBB -> TiltakResponsDTO.TiltakTypeDTO.JOBBK
        TiltakstypeSomGirRett.OPPFØLGING -> TiltakResponsDTO.TiltakTypeDTO.INDOPPFAG
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_NAV -> TiltakResponsDTO.TiltakTypeDTO.UTVAOONAV
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_OPPLÆRING -> TiltakResponsDTO.TiltakTypeDTO.UTVOPPFOPL
        TiltakstypeSomGirRett.ARBEIDSMARKEDSOPPLAERING -> TiltakResponsDTO.TiltakTypeDTO.ARBEIDSMARKEDSOPPLAERING
        TiltakstypeSomGirRett.FAG_OG_YRKESOPPLAERING -> TiltakResponsDTO.TiltakTypeDTO.FAG_OG_YRKESOPPLAERING
        TiltakstypeSomGirRett.HOYERE_YRKESFAGLIG_UTDANNING -> TiltakResponsDTO.TiltakTypeDTO.HOYERE_YRKESFAGLIG_UTDANNING
        TiltakstypeSomGirRett.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> TiltakResponsDTO.TiltakTypeDTO.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV
        TiltakstypeSomGirRett.STUDIESPESIALISERING -> TiltakResponsDTO.TiltakTypeDTO.STUDIESPESIALISERING
    }
}
