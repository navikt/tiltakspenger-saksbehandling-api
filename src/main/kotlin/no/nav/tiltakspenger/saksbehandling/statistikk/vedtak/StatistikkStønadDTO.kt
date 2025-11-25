package no.nav.tiltakspenger.saksbehandling.statistikk.vedtak

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
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

    // For søknadsbehandling+forlengelse (ren innvilgese), vil de tilsvare innvilgelsesperiode. For omgjøring, vil de tilsvare omgjøringsperioden (innvilgelse + implisitt opphør/ikke rett). For stans vil de tilsvare stansperiode. For opphør vil de tilsvare opphørsperiode.
    // Selv om det på lang sikt kan tenkes at vi støtter omgjøringer av vedtak med hull (forskjellige ikke-overlappende tiltak o.l), så innfører vi ikke denne om til en liste den 2025-10-20.
    val vedtaksperiodeFraOgMed: LocalDate,
    val vedtaksperiodeTilOgMed: LocalDate,

    // Per 2025-10-20 har vi kun støtte for én innvilgelsesperiode per vedtak, men på sikt vil vi kunne ha flere. Dette for å hovedsakelig støtte hull i innvilgelsesperioden ved omgjøringsvedtak. Men kan tenkes at den kan bli brukt i søknadsbehandling og forlengelse også.
    // Listen er tom for stans og avslag
    val innvilgelsesperioder: List<PeriodeDTO>,

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
    Stans,
    ;

    companion object {
        fun BehandlingResultat.toVedtakStatistikkResultat(): VedtakStatistikkResultat = when (this) {
            is BehandlingResultat.Innvilgelse -> Innvilgelse
            is RevurderingResultat.Stans -> Stans
            is SøknadsbehandlingResultat.Avslag -> throw IllegalStateException("Skal ikke opprette vedtaksstatistikk for avslag")
        }
    }
}
