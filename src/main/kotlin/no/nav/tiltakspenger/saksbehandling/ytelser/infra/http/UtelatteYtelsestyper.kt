package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

/**
 * Kjente ytelsestyper fra sokos-utbetaldata som saksbehandlingen ikke henter, skrevet slik tjenesten sender dem.
 * Tjenesten henter teksten fra kolonnen YTELSETYPE i økonomisystemet, og tabellen «Ytelseskoder/typer» i README-en til sokos-utbetaldata lister verdiene (lest 2026-10-19): https://github.com/navikt/sokos-utbetaldata#ytelseskodertyper
 * «Skatt og trekk», «Trekk» og «Skattetrekk» står ikke i tabellen, men tjenesten setter dem på en ytelse som bare har skatt eller trekk (`utledYtelsestype` i `MapResponseV2.kt`).
 * «Refusjon PTS» står i tabellen, men ikke her, fordi [Ytelsetype.REFUSJON_PST] har teksten «Refusjon PST»; kommer den, telles den som ukjent.
 * En type som verken står her eller i [Ytelsetype], telles som ukjent.
 * Kommentaren over hver type sier hva den er og hvor det står, eller at vi er usikre på hva den dekker.
 */
val utelatteYtelsestyper: Set<String> = setOf(
    // Trekk i utbetalingen.
    "Akonto trekk",
    // Usikker på hva typen dekker.
    "Akonto utbetaling",
    // Usikker på hva typen dekker.
    "Attføringshjelp",
    // Barnepensjon etter folketrygdloven kapittel 18 hindrer ikke tiltakspenger (tiltakspengeforskriften § 7 første ledd): https://lovdata.no/lov/1997-02-28-19/kap18, og Navs beskrivelse: https://www.nav.no/barnepensjon
    "Barnepensjon",
    // Stønad til barnetilsyn, for eksempel etter folketrygdloven § 15-10: https://lovdata.no/lov/1997-02-28-19/%C2%A715-10, og Navs beskrivelse: https://www.nav.no/barnetilsyn-enslig
    "Barnetilsyn",
    // Barnetrygd hindrer ikke tiltakspenger (tiltakspengeforskriften § 7 andre ledd): https://lovdata.no/lov/2002-03-08-4, og Navs beskrivelse: https://www.nav.no/barnetrygd
    "Barnetrygd",
    // Folketrygdloven kapittel 10, stønad til bedring av arbeidsevnen og funksjonsevnen: https://lovdata.no/lov/1997-02-28-19/kap10
    "Bedring funksjonsevnen",
    // Usikker på hva typen dekker.
    "Behandleroppgjør",
    // Usikker på hva typen dekker.
    "Bidrag",
    // Trekk i utbetalingen.
    "Bidragstrekk",
    // Usikker på hva typen dekker.
    "Erstatning",
    // Usikker på hva typen dekker.
    "Etterbet. foreldet FTP",
    // Trekk i utbetalingen.
    "Fagforeningstrekk",
    // Usikker på hva typen dekker.
    "Feilutbetaling",
    // Feriepenger av en annen ytelse, for eksempel folketrygdloven § 8-33: https://lovdata.no/lov/1997-02-28-19/%C2%A78-33, og Navs beskrivelse: https://www.nav.no/feriepenger
    "Feriepenger",
    // Ferietillegg, for eksempel til dagpenger etter folketrygdloven § 4-14: https://lovdata.no/lov/1997-02-28-19/%C2%A74-14
    "Ferietillegg",
    // Trekk i utbetalingen.
    "Forskuddstrekk",
    // Usikker på hva typen dekker.
    "Gebyr",
    // Folketrygdloven kapittel 7, stønad ved gravferd: https://lovdata.no/lov/1997-02-28-19/kap7, og Navs beskrivelse: https://www.nav.no/gravferd
    "Gravferdshjelp",
    // Folketrygdloven kapittel 6: https://lovdata.no/lov/1997-02-28-19/kap6, og Navs beskrivelse: https://www.nav.no/grunnstonad og https://www.nav.no/hjelpestonad
    "Grunn- og hjelpestønad",
    // Usikker på hva typen dekker.
    "Helserefusjon",
    // Trekk i utbetalingen.
    "Husleietrekk",
    // Usikker på hva typen dekker.
    "Justeringskonto OS",
    // Usikker på hva typen dekker.
    "Kommunalt boligtilskudd",
    // Kontantstøtte hindrer ikke tiltakspenger (tiltakspengeforskriften § 7 andre ledd): https://lovdata.no/lov/1998-06-26-41, og Navs beskrivelse: https://www.nav.no/kontantstotte
    "Kontantstøtte",
    // Trekk i utbetalingen.
    "Kreditortrekk",
    // Usikker på hva typen dekker.
    "Lån",
    // Usikker på hva typen dekker.
    "Motregning",
    // Usikker på hva typen dekker.
    "Omstilling",
    // Usikker på hva typen dekker.
    "Oppgjør",
    // Trekk i utbetalingen.
    "Påleggstrekk skatt",
    // Usikker på hva typen dekker.
    "Pensjonsavgift",
    // Usikker på hva typen dekker.
    "Premie",
    // Reisetilskudd i stedet for sykepenger, Navs beskrivelse: https://www.nav.no/reisetilskudd
    "Reisetilskudd",
    // Satt av sokos-utbetaldata på en ytelse som bare har skatt eller trekk.
    "Skatt og trekk",
    // Satt av sokos-utbetaldata på en ytelse som bare har skatt eller trekk.
    "Skattetrekk",
    // Usikker på hva typen dekker.
    "Tilbakebetaling",
    // Tilleggsstønad, for eksempel etter arbeidsmarkedsloven § 13 andre ledd: https://lovdata.no/lov/2004-12-10-76/%C2%A713, og Navs beskrivelse: https://www.nav.no/tilleggsstonader
    "Tilleggsstønad",
    // Usikker på hva typen dekker.
    "Tilskudd",
    // Usikker på hva typen dekker.
    "Transaksjonsgebyr",
    // Satt av sokos-utbetaldata på en ytelse som bare har skatt eller trekk.
    "Trekk",
    // Trekk i utbetalingen.
    "Trekk - avregning",
    // Trekk i utbetalingen.
    "Trekk forskudd",
    // Trekk i utbetalingen.
    "Trekk SPK",
    // Trekk i utbetalingen.
    "Utleggstrekk",
    // Påvirker ikke tiltakspenger.
    "Utdanningsstønad",
    // Trekk i utbetalingen.
    "Vederlagstrekk",
    // Folketrygdloven kapittel 13, usikker på om typen gjelder engangserstatning eller løpende ytelse: https://lovdata.no/lov/1997-02-28-19/kap13, og Navs beskrivelse: https://www.nav.no/yrkesskade
    "Yrkesskade",
)
