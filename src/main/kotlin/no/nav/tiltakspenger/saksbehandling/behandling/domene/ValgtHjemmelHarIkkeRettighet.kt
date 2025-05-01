package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface ValgtHjemmelHarIkkeRettighet

enum class ValgtHjemmelType { STANS, AVSLAG }

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=679150248
 */
sealed interface ValgtHjemmelForStans : ValgtHjemmelHarIkkeRettighet {
    data object DeltarIkkePåArbeidsmarkedstiltak : ValgtHjemmelForStans
    data object Alder : ValgtHjemmelForStans
    data object Livsoppholdytelser : ValgtHjemmelForStans
    data object Kvalifiseringsprogrammet : ValgtHjemmelForStans
    data object Introduksjonsprogrammet : ValgtHjemmelForStans
    data object LønnFraTiltaksarrangør : ValgtHjemmelForStans
    data object LønnFraAndre : ValgtHjemmelForStans
    data object Institusjonsopphold : ValgtHjemmelForStans
}

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=679150248
 */
sealed interface ValgtHjemmelForAvslag : ValgtHjemmelHarIkkeRettighet {
    data object DeltarIkkePåArbeidsmarkedstiltak : ValgtHjemmelForAvslag
    data object Alder : ValgtHjemmelForAvslag
    data object Livsoppholdytelser : ValgtHjemmelForAvslag
    data object Kvalifiseringsprogrammet : ValgtHjemmelForAvslag
    data object Introduksjonsprogrammet : ValgtHjemmelForAvslag
    data object LønnFraTiltaksarrangør : ValgtHjemmelForAvslag
    data object LønnFraAndre : ValgtHjemmelForAvslag
    data object Institusjonsopphold : ValgtHjemmelForAvslag
}
