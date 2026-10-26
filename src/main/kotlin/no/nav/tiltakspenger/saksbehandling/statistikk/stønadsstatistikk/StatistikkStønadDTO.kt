package no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk

import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadDTO.OmgjørRammevedtakStatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadDTO.OmgjøringsgradStatistikk
import java.time.LocalDate
import java.util.UUID

/**
 * Én rad i `statistikk_stonad` per rammevedtak; Team Spenn (DVH) leser tabellen sammen med `statistikk_utbetaling` og `statistikk_meldekort`.
 *
 * Kildekode: https://github.com/navikt/dvh_tiltakspenger (internt repo; kolonnene de henter står i `etl/datalast_fra_bq.py`)
 * Dokumentasjon: https://confluence.adeo.no/display/DVH/dvh_tiltakspenger og dbt-modellene i https://dbt.ansatt.nav.no/docs/spenn/dvh-tiltakspenger/index.html
 * Slack: #tiltakspenger-dvh
 * Teamkatalog: https://teamkatalogen.nav.no/team/3c411c72-9587-4c6f-937e-2a404935b778
 *
 * Nye og fjernede kolonner må avklares med Team Spenn; se `docs/statistikk/README.md`.
 */
data class StatistikkStønadDTO(
    // tilfeldig id
    val id: UUID,
    // fnr
    val brukerId: String,

    val sakId: String,
    val saksnummer: String,
    val resultat: VedtakStatistikkResultat,
    val sakDato: LocalDate,

    // For søknadsbehandling+forlengelse (ren innvilgelse), vil de tilsvare innvilgelsesperiode.
    // For omgjøring, vil de tilsvare omgjøringsperioden (innvilgelse + implisitt opphør/ikke rett).
    // For stans vil de tilsvare stansperiode.
    // For opphør vil de tilsvare opphørsperiode.
    // En omgjøring kan omgjøre flere vedtak og ha hull, men vi beholder totalperioden her for bakoverkompatibilitet.
    // De faktiske periodene som omgjøres ligger i [omgjørRammevedtak].
    val vedtaksperiodeFraOgMed: LocalDate,
    val vedtaksperiodeTilOgMed: LocalDate,

    // Listen er tom for stans og avslag
    val innvilgelsesperioder: List<InnvilgelsesperiodeStatistikk>,

    // TODO: erstattet av [omgjørRammevedtak].
    // Team Spenn bruker det ikke lenger i dbt, men henter kolonnen fortsatt i etl/datalast_fra_bq.py i navikt/dvh_tiltakspenger; fjern den når de har tatt den ut der.
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
    Opphør,
    ;

    companion object {
        fun Rammebehandlingsresultat.toVedtakStatistikkResultat(): VedtakStatistikkResultat = when (this) {
            is Rammebehandlingsresultat.Innvilgelse -> Innvilgelse

            is Revurderingsresultat.Stans -> Stans

            is Omgjøringsresultat.OmgjøringOpphør -> Opphør

            is Omgjøringsresultat.OmgjøringIkkeValgt,
            is Søknadsbehandlingsresultat.Avslag,
            -> throw IllegalStateException("Skal ikke opprette vedtaksstatistikk for $this")
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
