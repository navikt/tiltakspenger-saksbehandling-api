package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO

/**
 * @see [TiltakstypeSomGirRettDTO]
 */
internal enum class TiltakstypeSomGirRettDb {
    ARBEIDSFORBEREDENDE_TRENING,
    ARBEIDSMARKEDSOPPLAERING,
    ARBEIDSRETTET_REHABILITERING,
    ARBEIDSTRENING,
    AVKLARING,
    DIGITAL_JOBBKLUBB,
    ENKELTPLASS_AMO,
    ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG,
    FAG_OG_YRKESOPPLAERING,
    FORSØK_OPPLÆRING_LENGRE_VARIGHET,
    GRUPPE_AMO,
    GRUPPE_VGS_OG_HØYERE_YRKESFAG,
    HOYERE_YRKESFAGLIG_UTDANNING,
    HØYERE_UTDANNING,
    INDIVIDUELL_JOBBSTØTTE,
    INDIVIDUELL_KARRIERESTØTTE_UNG,
    JOBBKLUBB,
    NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
    OPPFØLGING,
    STUDIESPESIALISERING,
    UTVIDET_OPPFØLGING_I_NAV,
    UTVIDET_OPPFØLGING_I_OPPLÆRING,
    ;

    fun toDomain(): TiltakstypeSomGirRettDTO = TiltakstypeSomGirRettDTO.valueOf(this.name)
}

internal fun TiltakstypeSomGirRettDTO.toDb(): String = this.name

internal fun String.toTiltakstypeSomGirRett(): TiltakstypeSomGirRettDTO = TiltakstypeSomGirRettDb.valueOf(this).toDomain()
