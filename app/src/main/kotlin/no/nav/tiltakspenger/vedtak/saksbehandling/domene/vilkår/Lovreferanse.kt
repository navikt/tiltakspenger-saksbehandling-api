package no.nav.tiltakspenger.vedtak.saksbehandling.domene.vilkår

// i stor grad hentet fra https://lovdata.no/pro/#document/SF/forskrift/2013-11-04-1286
enum class Lovreferanse(
    val lovverk: String,
    val paragraf: String,
    val ledd: String,
    val beskrivelse: String,
) {
    ALDER("Tiltakspengeforskriften", "§3", "1", "Tiltakspenger og barnetillegg"),
    INTROPROGRAMMET("Tiltakspengeforskriften", "§7", "3", "Forholdet til andre ytelser"),
    KVP("Tiltakspengeforskriften", "§7", "3", "Forholdet til andre ytelser"),
    INSTITUSJONSOPPHOLD("Tiltakspengeforskriften", "§9", "1", "Opphold i institusjon, fengsel mv."),
    TILTAKSDELTAGELSE("Tiltakspengeforskriften", "§2", "1", "Hvem som kan få tiltakspenger"),
    STØNADSDAGER("Tiltakspengeforskriften", "§6", "1", "Stønadsdager"),
    FRIST_FOR_FRAMSETTING_AV_KRAV(
        "Tiltakspengeforskriften",
        "§11",
        "1",
        "Utbetaling, frist for framsetting av krav og rett til etterbetaling",
    ),
    LIVSOPPHOLDYTELSER("Tiltakspengeforskriften", "§7", "1", "Forholdet til andre ytelser"),
}
