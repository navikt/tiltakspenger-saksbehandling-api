package no.nav.tiltakspenger.saksbehandling.ytelser.domene

/**
 * Ytelsestypene vi henter fra sokos-utbetaldata.
 * Det gis ikke tiltakspenger for samme periode som deltakeren har rett til andre ytelser til livsopphold: https://lovdata.no/forskrift/2013-11-04-1286/%C2%A77
 * Rundskrivet til paragrafen regner med ytelser fra både offentlige og private trygde- og pensjonsordninger: https://lovdata.no/nav/rundskriv/r76-13-02
 * Typene vi kjenner, men ikke henter, står i [no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.utelatteYtelsestyper].
 * Teksten tjenesten sender står i klientlaget, lagringsformatet i repo-laget og visningsteksten i route-laget.
 */
enum class Ytelsetype {
    /** Folketrygdloven kapittel 11: https://lovdata.no/lov/1997-02-28-19/kap11, og Navs beskrivelse: https://www.nav.no/aap */
    AAP,

    /** Pensjon fra en pensjonsordning, som rundskrivet regner med: https://lovdata.no/nav/rundskriv/r76-13-02, og Navs beskrivelse: https://www.nav.no/afp-offentlig og https://www.nav.no/afp-i-privat-sektor */
    AFP,

    /** Folketrygdloven kapittel 19 og 20: https://lovdata.no/lov/1997-02-28-19/kap19 og https://lovdata.no/lov/1997-02-28-19/kap20, og Navs beskrivelse: https://www.nav.no/alderspensjon */
    ALDERSPENSJON,

    /** Folketrygdloven kapittel 4: https://lovdata.no/lov/1997-02-28-19/kap4, og Navs beskrivelse: https://www.nav.no/dagpenger */
    DAGPENGER,

    /** Folketrygdloven kapittel 15, som også har stønad til barnetilsyn: https://lovdata.no/lov/1997-02-28-19/kap15, og Navs beskrivelse: https://www.nav.no/alene-med-barn */
    ENSLIG_FORSORGER,

    /** Gjenlevendepensjon etter folketrygdloven kapittel 17 slik det var før 2024, erstattet av omstillingsstønad med siste utbetaling i desember 2026: https://lovdata.no/lov/1997-02-28-19/kap17, og Navs beskrivelse: https://www.nav.no/gjenlevendepensjon */
    ETTERLATTEPENSJON,

    /** Folketrygdloven kapittel 14: https://lovdata.no/lov/1997-02-28-19/kap14, og Navs beskrivelse: https://www.nav.no/foreldrepenger */
    FORELDREPENGER,

    /** Foreldrepenger utbetalt til arbeidsgiver, folketrygdloven § 22-3: https://lovdata.no/lov/1997-02-28-19/%C2%A722-3 */
    FORELDREPENREFARBGIV,

    /** Trolig kompensasjon for tapt personinntekt til selvstendig næringsdrivende og frilansere under koronapandemien; Skatteetatens beskrivelse: https://www.skatteetaten.no/bedrift-og-organisasjon/arbeidsgiver/a-meldingen/veiledning/lonn-og-ytelser/oversikt-over-lonn-og-andre-ytelser/kompensasjon-for-tapt-personinntekt-til-selvstendig-naringsdrivende/ */
    KOMPENSASJON,

    /** Krigspensjoneringslovene: https://lovdata.no/lov/1946-12-13-21 og https://lovdata.no/lov/1946-12-13-22, og Navs beskrivelse: https://www.nav.no/krigspensjon */
    KRIGSPENSJON,

    /** Lønnskompensasjon til permitterte under koronapandemien, etter den opphevede lønnskompensasjonsforskriften: https://lovdata.no/forskrift/2020-05-29-1102 */
    LONNSKOMPENSASJON,

    /** Folketrygdloven kapittel 9: https://lovdata.no/lov/1997-02-28-19/kap9, og Navs beskrivelse: https://www.nav.no/omsorgspenger */
    OMSORGSPENGER,

    /** Folketrygdloven kapittel 17: https://lovdata.no/lov/1997-02-28-19/kap17, og Navs beskrivelse: https://www.nav.no/omstillingsstonad */
    OMSTILLINGSSTONAD,

    /** Folketrygdloven kapittel 9: https://lovdata.no/lov/1997-02-28-19/kap9, og Navs beskrivelse: https://www.nav.no/opplaringspenger */
    OPPLAERINGSPENGER,

    /** Usikker på hvilken ordning typen gjelder. */
    PEN_REINDRIFTSSAMER,

    /** Folketrygdloven kapittel 9: https://lovdata.no/lov/1997-02-28-19/kap9, og Navs beskrivelse: https://www.nav.no/pleiepenger-barn */
    PLEIEPENGER,

    /** Pleiepenger utbetalt til arbeidsgiver, folketrygdloven § 22-3: https://lovdata.no/lov/1997-02-28-19/%C2%A722-3 */
    PLEIEPENGERREFARBGIV,

    /** Usikker på hva typen dekker, men den kommer ofte sammen med andre ytelser som påvirker tiltakspenger, så den hentes. */
    REFUSJON,

    /** Usikker på hva typen dekker; README-en til sokos-utbetaldata oppgir teksten «Refusjon PTS»: https://github.com/navikt/sokos-utbetaldata#ytelseskodertyper */
    REFUSJON_PST,

    /** Usikker på hva typen dekker. */
    REFUSJON_SPK,

    /** Usikker på hva typen dekker. */
    REFUSJONSKRAV,

    /** Pensjon fra Statens pensjonskasse: https://lovdata.no/lov/1949-07-28-26 */
    SPK,

    /** Lov om supplerende stønad til personer med kort botid i Norge: https://lovdata.no/lov/2005-04-29-21, og Navs beskrivelse: https://www.nav.no/supplerende-stonad-over-67 og https://www.nav.no/supplerende-stonad-flyktninger */
    SUPPLERENDE_STONAD,

    /** Folketrygdloven kapittel 14: https://lovdata.no/lov/1997-02-28-19/kap14, og Navs beskrivelse: https://www.nav.no/svangerskapspenger */
    SVANGERSKAPSPENGER,

    /** Folketrygdloven kapittel 8: https://lovdata.no/lov/1997-02-28-19/kap8, og Navs beskrivelse: https://www.nav.no/sykepenger */
    SYKEPENGER,

    /** Sykepenger utbetalt til arbeidsgiver, folketrygdloven § 22-3: https://lovdata.no/lov/1997-02-28-19/%C2%A722-3 */
    SYKEPENGERREFARBGIV,

    /** Pensjon fra en pensjonsordning, som rundskrivet regner med: https://lovdata.no/nav/rundskriv/r76-13-02 */
    TJENESTEPENSJON,

    /** Vår egen ytelse, arbeidsmarkedsloven § 13: https://lovdata.no/lov/2004-12-10-76/%C2%A713, og Navs beskrivelse: https://www.nav.no/tiltakspenger */
    TILTAKSPENGER,

    /** Usikker på hvilken ordning typen gjelder. */
    UFOREPENSJON,

    /** Folketrygdloven kapittel 12: https://lovdata.no/lov/1997-02-28-19/kap12, og Navs beskrivelse: https://www.nav.no/uforetrygd */
    UFORETRYGD,

    /** Forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8: https://lovdata.no/forskrift/2025-06-20-1182/%C2%A78, og Navs beskrivelse: https://www.nav.no/ungdomsprogrammet */
    UNGDOMSPROGRAMYTELSE,
}
