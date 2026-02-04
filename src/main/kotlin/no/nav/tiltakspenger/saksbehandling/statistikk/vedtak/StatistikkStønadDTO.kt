package no.nav.tiltakspenger.saksbehandling.statistikk.vedtak

import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadDTO.OmgjørRammevedtakStatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadDTO.OmgjøringsgradStatistikk
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

    // For søknadsbehandling+forlengelse (ren innvilgelse), vil de tilsvare innvilgelsesperiode. For omgjøring, vil de tilsvare omgjøringsperioden (innvilgelse + implisitt opphør/ikke rett). For stans vil de tilsvare stansperiode. For opphør vil de tilsvare opphørsperiode.
    // Selv om det på lang sikt kan tenkes at vi støtter omgjøringer av vedtak med hull (forskjellige ikke-overlappende tiltak o.l), så innfører vi ikke denne om til en liste den 2025-10-20.
    val vedtaksperiodeFraOgMed: LocalDate,
    val vedtaksperiodeTilOgMed: LocalDate,

    // Listen er tom for stans og avslag
    val innvilgelsesperioder: List<InnvilgelsesperiodeStatistikk>,

    // TODO: erstattes av [omgjørRammevedtak] - kan fjernes når DVH ikke bruker dette feltet lengre
    val omgjørRammevedtakId: String?,

    val omgjørRammevedtak: List<OmgjørRammevedtakStatistikk>,

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

    val barnetillegg: List<BarnetilleggStatistikk>,
    val harBarnetillegg: Boolean,
) {
    data class BarnetilleggStatistikk(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val antallBarn: Int,
    )

    data class InnvilgelsesperiodeStatistikk(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val tiltaksdeltakelse: String,
    )

    data class OmgjørRammevedtakStatistikk(
        val vedtakId: String,
        val omgjøringsgrad: OmgjøringsgradStatistikk,
        val periode: PeriodeDbJson,
    )

    enum class OmgjøringsgradStatistikk {
        HELT,
        DELVIS,
    }
}

enum class VedtakStatistikkResultat {
    Innvilgelse,
    Stans,
    ;

    companion object {
        fun Rammebehandlingsresultat.toVedtakStatistikkResultat(): VedtakStatistikkResultat = when (this) {
            is Rammebehandlingsresultat.Innvilgelse -> Innvilgelse
            is Revurderingsresultat.Stans -> Stans
            is Søknadsbehandlingsresultat.Avslag -> throw IllegalStateException("Skal ikke opprette vedtaksstatistikk for avslag")
            is Omgjøringsresultat.OmgjøringIkkeValgt -> TODO()
            is Omgjøringsresultat.OmgjøringOpphør -> TODO()
        }
    }
}

fun OmgjørRammevedtak.tilStatistikk(): List<OmgjørRammevedtakStatistikk> {
    return this.omgjøringsperioder.map {
        OmgjørRammevedtakStatistikk(
            vedtakId = it.rammevedtakId.toString(),
            omgjøringsgrad = when (it.omgjøringsgrad) {
                Omgjøringsgrad.HELT -> OmgjøringsgradStatistikk.HELT
                Omgjøringsgrad.DELVIS -> OmgjøringsgradStatistikk.DELVIS
            },
            periode = it.periode.toDbJson(),
        )
    }
}
