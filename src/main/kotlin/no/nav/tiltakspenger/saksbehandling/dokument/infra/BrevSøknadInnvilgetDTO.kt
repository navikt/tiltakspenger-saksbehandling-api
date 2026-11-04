package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.leggSammen
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.dokument.infra.BrevRammevedtakInnvilgelseBaseDTO.SatserDTO
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class BrevFørstegangsvedtakInnvilgelseDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: LocalDate,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    override val harBarnetillegg: Boolean,
    override val satser: List<SatserDTO>,
    val satshistorikk: List<SatshistorikkDTO>?,
    val perioder: List<PeriodeInnvilgelseDTO>,
    override val innvilgelsesperioder: BrevInnvilgelsesperioderDTO,
    override val barnetillegg: List<BrevBarnetilleggDTO>,
) : BrevRammevedtakInnvilgelseBaseDTO

suspend fun Rammevedtak.tilInnvilgetSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
): String {
    return genererInnvilgetSøknadsbrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        tilleggstekst = tilleggstekst,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        innvilgelsesperioder = this.rammebehandling.innvilgelsesperioder!!,
        barnetillegg = this.rammebehandling.barnetillegg!!.periodisering,
    )
}

suspend fun genererInnvilgetSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    innvilgelsesperioder: Innvilgelsesperioder,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    barnetillegg: Periodisering<AntallBarn>?,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    return BrevFørstegangsvedtakInnvilgelseDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        satser = Satser.tilSatserDTO(innvilgelsesperioder.totalPeriode),
        satshistorikk = Satser.tilSatsHistorikk(innvilgelsesperioder.totalPeriode),
        perioder = totalDagSatsForAllePeriodene(innvilgelsesperioder, barnetillegg),
        tilleggstekst = tilleggstekst?.verdi,
        forhandsvisning = forhåndsvisning,
        harBarnetillegg = barnetillegg != null && barnetillegg.any { it.verdi.value > 0 },
        datoForUtsending = vedtaksdato,
        innvilgelsesperioder = tilInnvilgelsesperioder(innvilgelsesperioder),
        barnetillegg = tilBarnetillegg(barnetillegg),
    ).let { serialize(it) }
}

private fun tilInnvilgelsesperioder(
    innvilgelsesperioder: Innvilgelsesperioder,
): BrevInnvilgelsesperioderDTO {
    val sammenhengendeInnvilgelsesperioder = innvilgelsesperioder.perioder.leggSammen(false)

    return BrevInnvilgelsesperioderDTO(
        antallDagerPerMeldeperiode = innvilgelsesperioder.antallDagerPerMeldeperiode.verdier.map { it.value }.distinct().singleOrNull(),
        perioder = sammenhengendeInnvilgelsesperioder.map {
            BrevPeriodeDTO.fraPeriode(it)
        },
    )
}

private fun tilBarnetillegg(
    barnetillegg: Periodisering<AntallBarn>?,
): List<BrevBarnetilleggDTO> {
    val perioderMedBarnetillegg = barnetillegg?.perioderMedVerdi
        ?.filter { it.verdi.value > 0 }
        ?.let { it.ifEmpty { null } }

    return perioderMedBarnetillegg?.map { periodeMedVerdi ->
        BrevBarnetilleggDTO(
            antallBarn = periodeMedVerdi.verdi.value,
            periode = BrevPeriodeDTO.fraPeriode(periodeMedVerdi.periode),
        )
    } ?: emptyList()
}

private fun totalDagSatsForAllePeriodene(
    innvilgelsesperioder: Innvilgelsesperioder,
    barnetillegg: Periodisering<AntallBarn>?,
): List<PeriodeInnvilgelseDTO> {
    return innvilgelsesperioder.perioder.leggSammen(false).map { periode ->

        val beløpPerStrekning = periode.tilDager().map { dag ->

            val satsdag = Satser.sats(dato = dag)
            val antallBarn = barnetillegg?.hentVerdiForDag(dag)?.value ?: 0

            Periode(fraOgMed = dag, tilOgMed = dag) to DagBeløp(
                antallBarn = antallBarn,
                tiltakspenger = satsdag.sats,
                barnetillegg = satsdag.satsBarnetillegg * antallBarn,
            )
        }.tilPeriodisering()

        PeriodeInnvilgelseDTO(
            periode = BrevPeriodeDTO.fraPeriode(periode),
            beløp = beløpPerStrekning.perioderMedVerdi.map { (beløp, strekning) ->
                PeriodeBeløpInnvilgelseDTO(
                    periode = BrevPeriodeDTO.fraPeriode(strekning),
                    antallBarn = beløp.antallBarn,
                    tiltakspenger = beløp.tiltakspenger,
                    barnetilleggBeløp = beløp.barnetillegg,
                    total = beløp.tiltakspenger + beløp.barnetillegg,
                )
            },
        )
    }
}

/**
 * Beløpene for én dag.
 * Må være en data class, slik at like nabodager slås sammen av tilPeriodisering.
 */
private data class DagBeløp(
    val antallBarn: Int,
    val tiltakspenger: Int,
    val barnetillegg: Int,
)
