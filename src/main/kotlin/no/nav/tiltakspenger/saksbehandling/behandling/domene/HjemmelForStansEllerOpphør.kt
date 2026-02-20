package no.nav.tiltakspenger.saksbehandling.behandling.domene

/**
 * https://confluence.adeo.no/pages/viewpage.action?pageId=679150248
 *
 * TODO abn: split til separate enums for stans og opphør
 */
enum class HjemmelForStansEllerOpphør {
    DeltarIkkePåArbeidsmarkedstiltak,
    Alder,
    Livsoppholdytelser,
    Kvalifiseringsprogrammet,
    Introduksjonsprogrammet,
    LønnFraTiltaksarrangør,
    LønnFraAndre,
    Institusjonsopphold,
    IkkeLovligOpphold,
    FremmetForSent,
}
