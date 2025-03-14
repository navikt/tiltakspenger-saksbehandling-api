package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

sealed interface ValgtHjemmelHarIkkeRettighet {
    val kode: String
    val type: ValgtHjemmelType
    val beskrivelse: String
}

enum class ValgtHjemmelType { STANS, AVSLAG }

sealed class ValgtHjemmelForStans(
    override val kode: String,
    override val beskrivelse: String,
) : ValgtHjemmelHarIkkeRettighet {
    override val type: ValgtHjemmelType = ValgtHjemmelType.STANS

    data object DeltarIkkePåArbeidsmarkedstiltak : ValgtHjemmelForStans(
        kode = "DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
        beskrivelse = "tiltakspengeforskriften § 2 - ingen deltagelse",
    )

    data object Alder : ValgtHjemmelForStans(
        kode = "ALDER",
        beskrivelse = "tiltakspengeforskriften § 3 - alder",
    )

    data object Livsoppholdytelser : ValgtHjemmelForStans(
        kode = "LIVSOPPHOLDYTELSER",
        beskrivelse = "tiltakspengeforskriften § 7, første ledd - andre livsoppholdsytelser",
    )

    data object Kvalifiseringsprogrammet : ValgtHjemmelForStans(
        kode = "KVALIFISERINGSPROGRAMMET",
        beskrivelse = "tiltakspengeforskriften § 7, tredje ledd - kvalifiseringsprogrammet",
    )

    data object Introduksjonsprogrammet : ValgtHjemmelForStans(
        kode = "INTRODUKSJONSPROGRAMMET",
        beskrivelse = "tiltakspengeforskriften § 7, tredje ledd - introduksjonsprogrammet",
    )

    data object LønnFraTiltaksarrangør : ValgtHjemmelForStans(
        kode = "LØNN_FRA_TILTAKSARRANGØR",
        beskrivelse = "tiltakspengeforskriften § 8 - lønn fra tiltaksarrangør",
    )

    data object LønnFraAndre : ValgtHjemmelForStans(
        kode = "LØNN_FRA_ANDRE",
        beskrivelse = "arbeidsmarkedsloven § 13 - lønn fra andre",
    )

    data object Institusjonsopphold : ValgtHjemmelForStans(
        kode = "INSTITUSJONSOPPHOLD",
        beskrivelse = "tiltakspengeforskriften § 9 - institusjonsopphold",
    )

    data object Annet : ValgtHjemmelForStans(
        kode = "ANNET",
        beskrivelse = "Annet",
    )
}

sealed class ValgtHjemmelForAvslag(
    override val kode: String,
    override val beskrivelse: String,
) : ValgtHjemmelHarIkkeRettighet {
    override val type = ValgtHjemmelType.AVSLAG

    data object DeltarIkkePåArbeidsmarkedstiltak : ValgtHjemmelForAvslag(
        kode = "DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
        beskrivelse = "tiltakspengeforskriften § 2 - ingen deltagelse",
    )

    data object Alder : ValgtHjemmelForAvslag(
        kode = "ALDER",
        beskrivelse = "tiltakspengeforskriften § 3 - alder",
    )

    data object Livsoppholdytelser : ValgtHjemmelForAvslag(
        kode = "LIVSOPPHOLDYTELSER",
        beskrivelse = "tiltakspengeforskriften § 7, første ledd - andre livsoppholdsytelser",
    )

    data object Kvalifiseringsprogrammet : ValgtHjemmelForAvslag(
        kode = "KVALIFISERINGSPROGRAMMET",
        beskrivelse = "tiltakspengeforskriften § 7, tredje ledd - kvalifiseringsprogrammet",
    )

    data object Introduksjonsprogrammet : ValgtHjemmelForAvslag(
        kode = "INTRODUKSJONSPROGRAMMET",
        beskrivelse = "tiltakspengeforskriften § 7, tredje ledd - introduksjonsprogrammet",
    )

    data object LønnFraTiltaksarrangør : ValgtHjemmelForAvslag(
        kode = "LØNN_FRA_TILTAKSARRANGØR",
        beskrivelse = "tiltakspengeforskriften § 8 - lønn fra tiltaksarrangør",
    )

    data object LønnFraAndre : ValgtHjemmelForAvslag(
        kode = "LØNN_FRA_ANDRE",
        beskrivelse = "arbeidsmarkedsloven § 13 - lønn fra andre",
    )

    data object Institusjonsopphold : ValgtHjemmelForAvslag(
        kode = "INSTITUSJONSOPPHOLD",
        beskrivelse = "tiltakspengeforskriften § 9 - institusjonsopphold",
    )

    data object Annet : ValgtHjemmelForAvslag(
        kode = "ANNET",
        beskrivelse = "Annet",
    )
}

fun toValgtHjemmelHarIkkeRettighet(type: ValgtHjemmelType, kode: String): ValgtHjemmelHarIkkeRettighet {
    return when (type) {
        ValgtHjemmelType.STANS -> ValgtHjemmelForStans::class.sealedSubclasses
        ValgtHjemmelType.AVSLAG -> ValgtHjemmelForAvslag::class.sealedSubclasses
    }.first { subclass -> subclass.objectInstance?.kode == kode }
        .objectInstance!!
}
