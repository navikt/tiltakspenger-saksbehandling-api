package no.nav.tiltakspenger.saksbehandling.behandling.domene

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=679150248
 */
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
