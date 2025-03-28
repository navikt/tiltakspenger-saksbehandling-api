package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface ValgtHjemmelHarIkkeRettighet {
    companion object {
        fun toValgtHjemmelHarIkkeRettighet(
            type: ValgtHjemmelType,
            valgtHjemmel: String,
        ): ValgtHjemmelHarIkkeRettighet {
            return when (type) {
                ValgtHjemmelType.STANS -> when (valgtHjemmel) {
                    "DeltarIkkePåArbeidsmarkedstiltak" -> ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak
                    "Alder" -> ValgtHjemmelForStans.Alder
                    "Livsoppholdytelser" -> ValgtHjemmelForStans.Livsoppholdytelser
                    "Kvalifiseringsprogrammet" -> ValgtHjemmelForStans.Kvalifiseringsprogrammet
                    "Introduksjonsprogrammet" -> ValgtHjemmelForStans.Introduksjonsprogrammet
                    "LønnFraTiltaksarrangør" -> ValgtHjemmelForStans.LønnFraTiltaksarrangør
                    "LønnFraAndre" -> ValgtHjemmelForStans.LønnFraAndre
                    "Institusjonsopphold" -> ValgtHjemmelForStans.Institusjonsopphold
                    else -> throw IllegalArgumentException("Ukjent kode for ValgtHjemmelForStans: $valgtHjemmel")
                }

                ValgtHjemmelType.AVSLAG -> when (valgtHjemmel) {
                    "DeltarIkkePåArbeidsmarkedstiltak" -> ValgtHjemmelForAvslag.DeltarIkkePåArbeidsmarkedstiltak
                    "Alder" -> ValgtHjemmelForAvslag.Alder
                    "Livsoppholdytelser" -> ValgtHjemmelForAvslag.Livsoppholdytelser
                    "Kvalifiseringsprogrammet" -> ValgtHjemmelForAvslag.Kvalifiseringsprogrammet
                    "Introduksjonsprogrammet" -> ValgtHjemmelForAvslag.Introduksjonsprogrammet
                    "LønnFraTiltaksarrangør" -> ValgtHjemmelForAvslag.LønnFraTiltaksarrangør
                    "LønnFraAndre" -> ValgtHjemmelForAvslag.LønnFraAndre
                    "Institusjonsopphold" -> ValgtHjemmelForAvslag.Institusjonsopphold
                    else -> throw IllegalArgumentException("Ukjent kode for ValgtHjemmelForAvslag: $valgtHjemmel")
                }
            }
        }
    }
}

enum class ValgtHjemmelType { STANS, AVSLAG }

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
