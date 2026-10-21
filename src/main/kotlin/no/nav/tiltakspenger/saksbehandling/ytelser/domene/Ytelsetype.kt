package no.nav.tiltakspenger.saksbehandling.ytelser.domene

/**
 * Ytelsestypene vi henter fra sokos-utbetaldata, med teksten tjenesten sender i [tekstverdi].
 * Det gis ikke tiltakspenger for samme periode som deltakeren har rett til andre ytelser til livsopphold: https://lovdata.no/forskrift/2013-11-04-1286/%C2%A77
 * Rundskrivet til paragrafen regner med ytelser fra både offentlige og private trygde- og pensjonsordninger: https://lovdata.no/nav/rundskriv/r76-13-02
 * Typene vi kjenner, men ikke henter, står i [no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.utelatteYtelsestyper].
 * TODO: Flytt tekstverdien til klientlaget, dette skal ikke være i domenet.
 */
enum class Ytelsetype(val tekstverdi: String) {
    /** Folketrygdloven kapittel 11: https://lovdata.no/lov/1997-02-28-19/kap11, og Navs beskrivelse: https://www.nav.no/aap */
    AAP("Arbeidsavklaringspenger"),

    /** Pensjon fra en pensjonsordning, som rundskrivet regner med: https://lovdata.no/nav/rundskriv/r76-13-02, og Navs beskrivelse: https://www.nav.no/afp-offentlig og https://www.nav.no/afp-i-privat-sektor */
    AFP("Avtalefestet pensjon"),

    /** Folketrygdloven kapittel 19 og 20: https://lovdata.no/lov/1997-02-28-19/kap19 og https://lovdata.no/lov/1997-02-28-19/kap20, og Navs beskrivelse: https://www.nav.no/alderspensjon */
    ALDERSPENSJON("Alderspensjon"),

    /** Folketrygdloven kapittel 4: https://lovdata.no/lov/1997-02-28-19/kap4, og Navs beskrivelse: https://www.nav.no/dagpenger */
    DAGPENGER("Dagpenger"),

    /** Folketrygdloven kapittel 15, som også har stønad til barnetilsyn: https://lovdata.no/lov/1997-02-28-19/kap15, og Navs beskrivelse: https://www.nav.no/alene-med-barn */
    ENSLIG_FORSORGER("Enslig forsørger"),

    /** Gjenlevendepensjon etter folketrygdloven kapittel 17 slik det var før 2024, erstattet av omstillingsstønad med siste utbetaling i desember 2026: https://lovdata.no/lov/1997-02-28-19/kap17, og Navs beskrivelse: https://www.nav.no/gjenlevendepensjon */
    ETTERLATTEPENSJON("Etterlattepensjon"),

    /** Folketrygdloven kapittel 14: https://lovdata.no/lov/1997-02-28-19/kap14, og Navs beskrivelse: https://www.nav.no/foreldrepenger */
    FORELDREPENGER("Foreldrepenger"),

    /** Foreldrepenger utbetalt til arbeidsgiver, folketrygdloven § 22-3: https://lovdata.no/lov/1997-02-28-19/%C2%A722-3 */
    FORELDREPENREFARBGIV("Foreldrepenger refusjon arbeidsgiver"),

    /** Trolig kompensasjon for tapt personinntekt til selvstendig næringsdrivende og frilansere under koronapandemien; Skatteetatens beskrivelse: https://www.skatteetaten.no/bedrift-og-organisasjon/arbeidsgiver/a-meldingen/veiledning/lonn-og-ytelser/oversikt-over-lonn-og-andre-ytelser/kompensasjon-for-tapt-personinntekt-til-selvstendig-naringsdrivende/ */
    KOMPENSASJON("Kompensasjon inntektstap"),

    /** Krigspensjoneringslovene: https://lovdata.no/lov/1946-12-13-21 og https://lovdata.no/lov/1946-12-13-22, og Navs beskrivelse: https://www.nav.no/krigspensjon */
    KRIGSPENSJON("Krigspensjon"),

    /** Lønnskompensasjon til permitterte under koronapandemien, etter den opphevede lønnskompensasjonsforskriften: https://lovdata.no/forskrift/2020-05-29-1102 */
    LONNSKOMPENSASJON("Lønnskompensasjon"),

    /** Folketrygdloven kapittel 9: https://lovdata.no/lov/1997-02-28-19/kap9, og Navs beskrivelse: https://www.nav.no/omsorgspenger */
    OMSORGSPENGER("Omsorgspenger"),

    /** Folketrygdloven kapittel 17: https://lovdata.no/lov/1997-02-28-19/kap17, og Navs beskrivelse: https://www.nav.no/omstillingsstonad */
    OMSTILLINGSSTONAD("Omstillingsstønad"),

    /** Folketrygdloven kapittel 9: https://lovdata.no/lov/1997-02-28-19/kap9, og Navs beskrivelse: https://www.nav.no/opplaringspenger */
    OPPLAERINGSPENGER("Opplæringspenger"),

    /** Usikker på hvilken ordning typen gjelder. */
    PEN_REINDRIFTSSAMER("Pensjon reindriftssamer"),

    /** Folketrygdloven kapittel 9: https://lovdata.no/lov/1997-02-28-19/kap9, og Navs beskrivelse: https://www.nav.no/pleiepenger-barn */
    PLEIEPENGER("Pleiepenger"),

    /** Pleiepenger utbetalt til arbeidsgiver, folketrygdloven § 22-3: https://lovdata.no/lov/1997-02-28-19/%C2%A722-3 */
    PLEIEPENGERREFARBGIV("Pleiepenger refusjon arbeidgiver"),

    /** Usikker på hva typen dekker, men den kommer ofte sammen med andre ytelser som påvirker tiltakspenger, så den hentes. */
    REFUSJON("Refusjon"),

    /** Usikker på hva typen dekker; README-en til sokos-utbetaldata oppgir teksten «Refusjon PTS»: https://github.com/navikt/sokos-utbetaldata#ytelseskodertyper */
    REFUSJON_PST("Refusjon PST"),

    /** Usikker på hva typen dekker. */
    REFUSJON_SPK("Refusjon SPK"),

    /** Usikker på hva typen dekker. */
    REFUSJONSKRAV("Refusjonskrav"),

    /** Pensjon fra Statens pensjonskasse: https://lovdata.no/lov/1949-07-28-26 */
    SPK("Statens pensjonkasse"),

    /** Lov om supplerende stønad til personer med kort botid i Norge: https://lovdata.no/lov/2005-04-29-21, og Navs beskrivelse: https://www.nav.no/supplerende-stonad-over-67 og https://www.nav.no/supplerende-stonad-flyktninger */
    SUPPLERENDE_STONAD("Supplerende_stønad"),

    /** Folketrygdloven kapittel 14: https://lovdata.no/lov/1997-02-28-19/kap14, og Navs beskrivelse: https://www.nav.no/svangerskapspenger */
    SVANGERSKAPSPENGER("Svangerskapspenger"),

    /** Folketrygdloven kapittel 8: https://lovdata.no/lov/1997-02-28-19/kap8, og Navs beskrivelse: https://www.nav.no/sykepenger */
    SYKEPENGER("Sykepenger"),

    /** Sykepenger utbetalt til arbeidsgiver, folketrygdloven § 22-3: https://lovdata.no/lov/1997-02-28-19/%C2%A722-3 */
    SYKEPENGERREFARBGIV("Sykepenger refusjon arbeidsgiver"),

    /** Pensjon fra en pensjonsordning, som rundskrivet regner med: https://lovdata.no/nav/rundskriv/r76-13-02 */
    TJENESTEPENSJON("Tjenestepensjon"),

    /** Vår egen ytelse, arbeidsmarkedsloven § 13: https://lovdata.no/lov/2004-12-10-76/%C2%A713, og Navs beskrivelse: https://www.nav.no/tiltakspenger */
    TILTAKSPENGER("Tiltakspenger"),

    /** Usikker på hvilken ordning typen gjelder. */
    UFOREPENSJON("Uførepensjon"),

    /** Folketrygdloven kapittel 12: https://lovdata.no/lov/1997-02-28-19/kap12, og Navs beskrivelse: https://www.nav.no/uforetrygd */
    UFORETRYGD("Uføretrygd"),

    /** Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8: https://lovdata.no/forskrift/2025-06-20-1182/%C2%A78, og Navs beskrivelse: https://www.nav.no/ungdomsprogrammet */
    UNGDOMSPROGRAMYTELSE("Ungdomsprogram ytelse"),

    /** Ytelse uten ytelsestype i svaret fra sokos-utbetaldata. */
    UKJENT("Ukjent"),
}
