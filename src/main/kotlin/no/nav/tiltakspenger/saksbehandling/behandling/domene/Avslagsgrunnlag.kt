package no.nav.tiltakspenger.saksbehandling.behandling.domene

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=679150248
 */
enum class Avslagsgrunnlag(val hjemler: List<Hjemmel>) {
    DeltarIkkePåArbeidsmarkedstiltak(
        listOf(
            Hjemmel(
                paragraf = Paragraf(2),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
            Hjemmel(
                paragraf = Paragraf(13),
                forskrift = Forskrift.Arbeidsmarkedsloven,
            ),
        ),
    ),
    Alder(
        listOf(
            Hjemmel(
                paragraf = Paragraf(3),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
        ),
    ),
    Livsoppholdytelser(
        listOf(
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
        ),
    ),
    Kvalifiseringsprogrammet(
        listOf(
            Hjemmel(
                paragraf = Paragraf(7),
                forskrift = Forskrift.Tiltakspengeforskriften,
                ledd = Ledd(3),
            ),
        ),
    ),
    Introduksjonsprogrammet(
        listOf(
            Hjemmel(
                paragraf = Paragraf(7),
                forskrift = Forskrift.Tiltakspengeforskriften,
                ledd = Ledd(3),
            ),
        ),
    ),
    LønnFraTiltaksarrangør(
        listOf(
            Hjemmel(
                paragraf = Paragraf(8),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
        ),
    ),
    LønnFraAndre(
        listOf(
            Hjemmel(
                paragraf = Paragraf(13),
                forskrift = Forskrift.Arbeidsmarkedsloven,
            ),
            Hjemmel(
                paragraf = Paragraf(8),
                forskrift = Forskrift.Tiltakspengeforskriften,
                ledd = Ledd(2),
            ),
        ),
    ),
    Institusjonsopphold(
        listOf(
            Hjemmel(
                paragraf = Paragraf(9),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
        ),
    ),
    FremmetForSent(
        listOf(
            Hjemmel(
                paragraf = Paragraf(11),
                forskrift = Forskrift.Tiltakspengeforskriften,
            ),
            Hjemmel(
                paragraf = Paragraf(15),
                forskrift = Forskrift.Arbeidsmarkedsloven,
            ),
        ),
    ),
}
