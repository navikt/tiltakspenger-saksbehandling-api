package no.nav.tiltakspenger.saksbehandling.repository.tiltak

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.ARBEIDSFORBEREDENDE_TRENING
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.ARBEIDSRETTET_REHABILITERING
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.ARBEIDSTRENING
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.AVKLARING
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.DIGITAL_JOBBKLUBB
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.ENKELTPLASS_AMO
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.FORSØK_OPPLÆRING_LENGRE_VARIGHET
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.GRUPPE_AMO
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.GRUPPE_VGS_OG_HØYERE_YRKESFAG
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.HØYERE_UTDANNING
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.INDIVIDUELL_JOBBSTØTTE
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.INDIVIDUELL_KARRIERESTØTTE_UNG
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.JOBBKLUBB
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.OPPFØLGING
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.UTVIDET_OPPFØLGING_I_NAV
import no.nav.tiltakspenger.saksbehandling.repository.tiltak.TiltakstypeSomGirRettDb.UTVIDET_OPPFØLGING_I_OPPLÆRING

/**
 * @see [TiltakstypeSomGirRett]
 */
internal enum class TiltakstypeSomGirRettDb {
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
    ;

    fun toDomain(): TiltakstypeSomGirRett =
        when (this) {
            ARBEIDSFORBEREDENDE_TRENING -> TiltakstypeSomGirRett.ARBEIDSFORBEREDENDE_TRENING
            ARBEIDSRETTET_REHABILITERING -> TiltakstypeSomGirRett.ARBEIDSRETTET_REHABILITERING
            ARBEIDSTRENING -> TiltakstypeSomGirRett.ARBEIDSTRENING
            AVKLARING -> TiltakstypeSomGirRett.AVKLARING
            DIGITAL_JOBBKLUBB -> TiltakstypeSomGirRett.DIGITAL_JOBBKLUBB
            ENKELTPLASS_AMO -> TiltakstypeSomGirRett.ENKELTPLASS_AMO
            ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> TiltakstypeSomGirRett.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG
            FORSØK_OPPLÆRING_LENGRE_VARIGHET -> TiltakstypeSomGirRett.FORSØK_OPPLÆRING_LENGRE_VARIGHET
            GRUPPE_AMO -> TiltakstypeSomGirRett.GRUPPE_AMO
            GRUPPE_VGS_OG_HØYERE_YRKESFAG -> TiltakstypeSomGirRett.GRUPPE_VGS_OG_HØYERE_YRKESFAG
            HØYERE_UTDANNING -> TiltakstypeSomGirRett.HØYERE_UTDANNING
            INDIVIDUELL_JOBBSTØTTE -> TiltakstypeSomGirRett.INDIVIDUELL_JOBBSTØTTE
            INDIVIDUELL_KARRIERESTØTTE_UNG -> TiltakstypeSomGirRett.INDIVIDUELL_KARRIERESTØTTE_UNG
            JOBBKLUBB -> TiltakstypeSomGirRett.JOBBKLUBB
            OPPFØLGING -> TiltakstypeSomGirRett.OPPFØLGING
            UTVIDET_OPPFØLGING_I_NAV -> TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_NAV
            UTVIDET_OPPFØLGING_I_OPPLÆRING -> TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_OPPLÆRING
        }
}

internal fun TiltakstypeSomGirRett.toDb(): String =
    when (this) {
        TiltakstypeSomGirRett.ARBEIDSFORBEREDENDE_TRENING -> ARBEIDSFORBEREDENDE_TRENING.name
        TiltakstypeSomGirRett.ARBEIDSRETTET_REHABILITERING -> ARBEIDSRETTET_REHABILITERING.name
        TiltakstypeSomGirRett.ARBEIDSTRENING -> ARBEIDSTRENING.name
        TiltakstypeSomGirRett.AVKLARING -> AVKLARING.name
        TiltakstypeSomGirRett.DIGITAL_JOBBKLUBB -> DIGITAL_JOBBKLUBB.name
        TiltakstypeSomGirRett.ENKELTPLASS_AMO -> ENKELTPLASS_AMO.name
        TiltakstypeSomGirRett.ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG -> ENKELTPLASS_VGS_OG_HØYERE_YRKESFAG.name
        TiltakstypeSomGirRett.FORSØK_OPPLÆRING_LENGRE_VARIGHET -> FORSØK_OPPLÆRING_LENGRE_VARIGHET.name
        TiltakstypeSomGirRett.GRUPPE_AMO -> GRUPPE_AMO.name
        TiltakstypeSomGirRett.GRUPPE_VGS_OG_HØYERE_YRKESFAG -> GRUPPE_VGS_OG_HØYERE_YRKESFAG.name
        TiltakstypeSomGirRett.HØYERE_UTDANNING -> HØYERE_UTDANNING.name
        TiltakstypeSomGirRett.INDIVIDUELL_JOBBSTØTTE -> INDIVIDUELL_JOBBSTØTTE.name
        TiltakstypeSomGirRett.INDIVIDUELL_KARRIERESTØTTE_UNG -> INDIVIDUELL_KARRIERESTØTTE_UNG.name
        TiltakstypeSomGirRett.JOBBKLUBB -> JOBBKLUBB.name
        TiltakstypeSomGirRett.OPPFØLGING -> OPPFØLGING.name
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_NAV -> UTVIDET_OPPFØLGING_I_NAV.name
        TiltakstypeSomGirRett.UTVIDET_OPPFØLGING_I_OPPLÆRING -> UTVIDET_OPPFØLGING_I_OPPLÆRING.name
    }

internal fun String.toTiltakstypeSomGirRett(): TiltakstypeSomGirRett = TiltakstypeSomGirRettDb.valueOf(this).toDomain()
