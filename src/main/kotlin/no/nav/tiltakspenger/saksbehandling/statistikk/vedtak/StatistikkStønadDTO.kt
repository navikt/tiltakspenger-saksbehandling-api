package no.nav.tiltakspenger.saksbehandling.statistikk.vedtak

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import java.time.LocalDate
import java.util.UUID

// Team Spenn (DVH) bruker denne tabellen, samt statistikk_utbetaling. De har beskrevet
// behovet sitt her: https://confluence.adeo.no/display/DVH/Datamodell+for+tiltakspenger
data class StatistikkStønadDTO(
    // tilfeldig id
    val id: UUID,
    // fnr
    val brukerId: String,

    val sakId: String,
    val saksnummer: String,
    val resultat: VedtakStatistikkResultat,
    val sakDato: LocalDate,
    val vedtakFom: LocalDate,
    val vedtakTom: LocalDate,
    // sakFraDato og sakTilDato er de samme som datoene for vedtaket siden sak ikke har periode lenger.
    // For innvilgelse, vil de tilsvare innvilgelsesperiode. For stans vil de tilsvare stansperiode. For opphør vil de tilsvare opphørsperiode. For avslag vil de være avslagsperiode.
    // Fra vi utvidet med et omgjøringsvedtak, vil de ikke formidle hvilken periode som ikke lenger gir rett i et omgjøringsvedtak.
    // Derfor er de deprecated.
    val sakFraDato: LocalDate,
    val sakTilDato: LocalDate,

    // Lagt til 2025-10-20, vil være null for rader før dette. Kan vurdere migrere rader som mangler dette senere.
    // For søknadsbehandling+forlengelse (ren innvilgese), vil de tilsvare innvilgelsesperiode. For omgjøring, vil de tilsvare omgjøringsperioden (innvilgelse + implisitt opphør/ikke rett). For stans vil de tilsvare stansperiode. For opphør vil de tilsvare opphørsperiode. For avslag vil de være avslagsperiode.
    // Selv om det på lang sikt kan tenkes at vi støtter omgjøringer av vedtak med hull (forskjellige ikke-overlappende tiltak o.l), så innfører vi ikke denne om til en liste den 2025-10-20.
    val virkningsperiodeFraOgMed: LocalDate?,
    val virkningsperiodeTilOgMed: LocalDate?,

    // Lagt til 2025-10-20, vil være null for rader før dette. Kan vurdere migrere rader som mangler dette senere.
    // Per 2025-10-20 har vi kun støtte for én innvilgelsesperiode per vedtak, men på sikt vil vi kunne ha flere. Dette for å hovedsakelig støtte hull i innvilgelsesperioden ved omgjøringsvedtak. Men kan tenkes at den kan bli brukt i søknadsbehandling og forlengelse også.
    val innvilgelsesperioder: List<PeriodeDTO>?,

    // Lagt til 2025-10-20, vil være null for rader før dette. Kan vurdere migrere rader som mangler dette senere.
    // Dette vedtaket omgjør et tidligere rammevedtak i sin helhet.
    val omgjørRammevedtakId: String?,

    // IND
    val ytelse: String,

    val søknadId: String?,
    val søknadDato: LocalDate?,
    // perioden for tiltaksdeltakelsen det er søkt for
    val søknadFraDato: LocalDate?,
    val søknadTilDato: LocalDate?,

    val vedtakId: String,
    val vedtaksType: String,
    val vedtakDato: LocalDate,

    // Brukes av DVH for å identifisere vedtakssystem når de sammenstiller data
    val fagsystem: String = "TPSAK",
    // tiltaksdeltakelser (eksternId) som det er innvilget tiltakspenger for
    val tiltaksdeltakelser: List<String>,
    val barnetillegg: List<Barnetillegg>,
    val harBarnetillegg: Boolean,
) {
    data class Barnetillegg(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val antallBarn: Int,
    )
}

enum class VedtakStatistikkResultat {
    Innvilgelse,
    Avslag,
    Stans,
    ;

    companion object {
        fun Vedtakstype.toVedtakStatistikkResultat(): VedtakStatistikkResultat = when (this) {
            Vedtakstype.INNVILGELSE -> Innvilgelse
            Vedtakstype.AVSLAG -> Avslag
            Vedtakstype.STANS -> Stans
        }
    }
}
