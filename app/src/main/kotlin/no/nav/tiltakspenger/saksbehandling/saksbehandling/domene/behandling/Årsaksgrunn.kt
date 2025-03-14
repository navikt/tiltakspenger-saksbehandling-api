package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

/**
 * Inneholder årsaksgrunner til at et vedtak blir stanset eller avslått.
 * Barnetillegg er en tilleggsytelse som kan gis til deltakere som har barn under 18 år, i vedtaksteksten i brevet blir
 * dette markert med {og barnetillegg} for å enkelt kunne fjerne dette.
 *
 * Se forskrift om tiltakspenger for mer informasjon.
 * https://lovdata.no/dokument/SF/forskrift/2013-11-04-1286
 */
enum class Årsaksgrunn(
    lovverk: String,
    paragraf: String,
    ledd: String,
    beskrivelse: String,
    private val vedtakstekstBrev: String,
) {
    DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK(
        lovverk = "Tiltakspengerforskriften",
        paragraf = "§2",
        ledd = "1",
        beskrivelse = "tiltakspengeforskriften § 2 - ingen deltagelse",
        vedtakstekstBrev = "du ikke lenger deltar på tiltak. Deltakere som ikke deltar på tiltak, har ikke rett til tiltakspenger {og barnetillegg} etter tiltakspengeforskriften §2.",
    ),
    ALDER(
        lovverk = "Tiltakspengerforskriften",
        paragraf = "§3",
        ledd = "1",
        beskrivelse = "tiltakspengeforskriften § 3 - alder",
        vedtakstekstBrev = TODO(),
    ),
    LIVSOPPHOLDYTELSER(
        lovverk = "Tiltakspengerforskriften",
        paragraf = "§7",
        ledd = "1",
        beskrivelse = "tiltakspengeforskriften § 7, første ledd - andre livsoppholdsytelser",
        vedtakstekstBrev = "du mottar en annen stønad til livsopphold. Deltakere som mottar andre stønader til livsopphold, har ikke rett til tiltakspenger {og barnetillegg} etter forskrift om tiltakspenger § 7.",
    ),
    KVALIFISERINGSPROGRAMMET(
        lovverk = "Tiltakspengerforskriften",
        paragraf = "§7",
        ledd = "3",
        beskrivelse = "tiltakspengeforskriften § 7, tredje ledd - kvalifiseringsprogrammet",
        vedtakstekstBrev = "du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram, har ikke rett til tiltakspenger {og barnetillegg} etter forskrift om tiltakspenger § 7, tredje ledd.",
    ),
    INTRODUKSJONSPROGRAMMET(
        lovverk = "Tiltakspengerforskriften",
        paragraf = "§7",
        ledd = "3",
        beskrivelse = "tiltakspengeforskriften § 7, tredje ledd - introduksjonsprogrammet",
        vedtakstekstBrev = "du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger {og barnetillegg} etter forskrift om tiltakspenger § 7, tredje ledd.",
    ),
    LØNN_FRA_TILTAKSARRANGØR(
        lovverk = "Tiltakspengerforskriften",
        paragraf = "§8",
        ledd = "1",
        beskrivelse = "tiltakspengeforskriften § 8 - lønn fra tiltaksarrangør",
        vedtakstekstBrev = TODO(),
    ),
    LØNN_FRA_ANDRE(
        lovverk = "Arbeidsmarkedsloven",
        paragraf = "§13",
        ledd = "1",
        beskrivelse = "arbeidsmarkedsloven § 13 - lønn fra andre",
        vedtakstekstBrev = TODO(),
    ),
    INSTITUSJONSOPPHOLD(
        lovverk = "Tiltakspengerforskriften",
        paragraf = "§9",
        ledd = "1",
        beskrivelse = "tiltakspengeforskriften § 9 - institusjonsopphold",
        vedtakstekstBrev = "du oppholder deg i institusjon med fri kost og losji. Deltakere som har opphold i institusjon, fengsel mv. med fri kost og losji under gjennomføringen av tiltaket, kan ikke samtidig motta tiltakspenger etter tiltakspengeforskriften §9 ",
    ),
    ANNET(
        lovverk = "-",
        paragraf = "-",
        ledd = "-",
        beskrivelse = "Annet",
        vedtakstekstBrev = TODO(),
    ),
    ;

    fun hentVedtakstekstBrev(barnetillegg: Boolean): String {
        return if (barnetillegg) {
            vedtakstekstBrev.replace(" {og barnetillegg}", " og barnetillegg")
        } else {
            vedtakstekstBrev.replace(" {og barnetillegg}", "")
        }
    }
}
