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

// TODO raq - flytt disse
@JvmInline
value class Paragraf(val nummer: Int)

@JvmInline
value class Ledd(val nummer: Int)

data class Hjemmel(
    val paragraf: Paragraf,
    val forskrift: Forskrift,
    val ledd: Ledd? = null,
)

enum class Forskrift {
    Tiltakspengeforskriften,
    Arbeidsmarkedsloven,
}

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=679150248
 */
// TODO raq - test
sealed interface Avslagsgrunnlag : ValgtHjemmelHarIkkeRettighet {
    val hjemler: List<Hjemmel>

    data object DeltarIkkePåArbeidsmarkedstiltak : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(2),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
            Hjemmel(
                paragraf = Paragraf(13),
                forskrift = Forskrift.Arbeidsmarkedsloven,
            ),
        )
    }

    data object Alder : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(3),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
        )
    }

    data object Livsoppholdytelser : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(7),
                forskrift = Forskrift.Tiltakspengeforskriften,
                ledd = Ledd(1),
            ),
            Hjemmel(
                paragraf = Paragraf(13),
                forskrift = Forskrift.Arbeidsmarkedsloven,
                ledd = Ledd(1),
            ),
        )
    }

    data object Kvalifiseringsprogrammet : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(7),
                forskrift = Forskrift.Tiltakspengeforskriften,
                ledd = Ledd(3),
            ),
        )
    }

    data object Introduksjonsprogrammet : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(7),
                forskrift = Forskrift.Tiltakspengeforskriften,
                ledd = Ledd(3),
            ),
        )
    }

    data object LønnFraTiltaksarrangør : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(8),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
        )
    }

    data object LønnFraAndre : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(13),
                forskrift = Forskrift.Arbeidsmarkedsloven,
            ),
            Hjemmel(
                paragraf = Paragraf(8),
                forskrift = Forskrift.Tiltakspengeforskriften,
                ledd = Ledd(2),
            ),
        )
    }

    data object Institusjonsopphold : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(9),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
        )
    }

    data object FremmetForSent : Avslagsgrunnlag {
        override val hjemler = listOf(
            Hjemmel(
                paragraf = Paragraf(11),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
            Hjemmel(
                paragraf = Paragraf(15),
                forskrift = Forskrift.Arbeidsmarkedsloven,
            ),
        )
    }
}
