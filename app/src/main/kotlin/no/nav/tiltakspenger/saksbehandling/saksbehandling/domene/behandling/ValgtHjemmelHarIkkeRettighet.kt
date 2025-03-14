package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

sealed interface ValgtHjemmelHarIkkeRettighet {
    val kode: String
    val type: ValgtHjemmelType
}

enum class ValgtHjemmelType { STANS, AVSLAG }

sealed class ValgtHjemmelForStans(
    override val kode: String,
) : ValgtHjemmelHarIkkeRettighet {
    override val type: ValgtHjemmelType = ValgtHjemmelType.STANS

    data object DeltarIkkePåArbeidsmarkedstiltak : ValgtHjemmelForStans(
        kode = "DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
    )

    data object Alder : ValgtHjemmelForStans(
        kode = "ALDER",
    )

    data object Livsoppholdytelser : ValgtHjemmelForStans(
        kode = "LIVSOPPHOLDYTELSER",
    )

    data object Kvalifiseringsprogrammet : ValgtHjemmelForStans(
        kode = "KVALIFISERINGSPROGRAMMET",
    )

    data object Introduksjonsprogrammet : ValgtHjemmelForStans(
        kode = "INTRODUKSJONSPROGRAMMET",
    )

    data object LønnFraTiltaksarrangør : ValgtHjemmelForStans(
        kode = "LØNN_FRA_TILTAKSARRANGØR",
    )

    data object LønnFraAndre : ValgtHjemmelForStans(
        kode = "LØNN_FRA_ANDRE",
    )

    data object Institusjonsopphold : ValgtHjemmelForStans(
        kode = "INSTITUSJONSOPPHOLD",
    )
}

sealed class ValgtHjemmelForAvslag(
    override val kode: String,
) : ValgtHjemmelHarIkkeRettighet {
    override val type = ValgtHjemmelType.AVSLAG

    data object DeltarIkkePåArbeidsmarkedstiltak : ValgtHjemmelForAvslag(
        kode = "DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
    )

    data object Alder : ValgtHjemmelForAvslag(
        kode = "ALDER",
    )

    data object Livsoppholdytelser : ValgtHjemmelForAvslag(
        kode = "LIVSOPPHOLDYTELSER",
    )

    data object Kvalifiseringsprogrammet : ValgtHjemmelForAvslag(
        kode = "KVALIFISERINGSPROGRAMMET",
    )

    data object Introduksjonsprogrammet : ValgtHjemmelForAvslag(
        kode = "INTRODUKSJONSPROGRAMMET",
    )

    data object LønnFraTiltaksarrangør : ValgtHjemmelForAvslag(
        kode = "LØNN_FRA_TILTAKSARRANGØR",
    )

    data object LønnFraAndre : ValgtHjemmelForAvslag(
        kode = "LØNN_FRA_ANDRE",
    )

    data object Institusjonsopphold : ValgtHjemmelForAvslag(
        kode = "INSTITUSJONSOPPHOLD",
    )
}

fun toValgtHjemmelHarIkkeRettighet(type: ValgtHjemmelType, kode: String): ValgtHjemmelHarIkkeRettighet {
    return when (type) {
        ValgtHjemmelType.STANS -> ValgtHjemmelForStans::class.sealedSubclasses
        ValgtHjemmelType.AVSLAG -> ValgtHjemmelForAvslag::class.sealedSubclasses
    }.first { subclass -> subclass.objectInstance?.kode == kode }
        .objectInstance!!
}

fun toValgtHjemmelHarIkkeRettighet(type: ValgtHjemmelType, koder: List<String>): List<ValgtHjemmelHarIkkeRettighet> {
    return koder.map { toValgtHjemmelHarIkkeRettighet(type, it) }
}
