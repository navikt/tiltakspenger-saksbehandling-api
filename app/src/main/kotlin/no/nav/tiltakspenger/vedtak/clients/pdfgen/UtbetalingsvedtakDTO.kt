package no.nav.tiltakspenger.vedtak.clients.pdfgen

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.vedtak.clients.pdfgen.formattering.norskDatoFormatter
import no.nav.tiltakspenger.vedtak.clients.pdfgen.formattering.norskTidspunktFormatter

private data class UtbetalingsvedtakDTO(
    val meldekortId: String,
    val saksnummer: String,
    val meldekortPeriode: PeriodeDTO,
    val saksbehandler: SaksbehandlerDTO,
    val beslutter: SaksbehandlerDTO,
    val meldekortDager: List<MeldekortDagDTO>,
    val tiltakstype: String,
    val eksternDeltagelseId: String,
    val eksternGjennomføringId: String?,
    val tiltaksnavn: String,
    val iverksattTidspunkt: String,
    val fødselsnummer: String,
) {
    @Suppress("unused")
    @JsonInclude
    val barnetillegg: Boolean = meldekortDager.map { it.beløpBarnetillegg }.any { it > 0 }

    data class SaksbehandlerDTO(
        val navn: String,
        val navIdent: String,
    )

    data class PeriodeDTO(
        val fom: String,
        val tom: String,
    )

    data class MeldekortDagDTO(
        val dato: String,
        val tiltakType: String,
        val status: String,
        val beløp: Int,
        val beløpBarnetillegg: Int,
        val prosent: Int,
        val reduksjon: String?,
    )
}

suspend fun Utbetalingsvedtak.toJsonRequest(
    hentSaksbehandlersNavn: suspend (String) -> String,
    tiltaksnavn: String,
    eksternGjennomføringId: String?,
    eksternDeltagelseId: String,
): String {
    return UtbetalingsvedtakDTO(
        fødselsnummer = fnr.verdi,
        saksbehandler = tilSaksbehadlerDto(saksbehandler, hentSaksbehandlersNavn),
        beslutter = tilSaksbehadlerDto(beslutter, hentSaksbehandlersNavn),
        meldekortId = meldekortId.toString(),
        saksnummer = saksnummer.toString(),
        meldekortPeriode = UtbetalingsvedtakDTO.PeriodeDTO(
            fom = periode.fraOgMed.format(norskDatoFormatter),
            tom = periode.tilOgMed.format(norskDatoFormatter),
        ),
        meldekortDager = meldekortbehandling.beregning.dager.map { dag ->
            UtbetalingsvedtakDTO.MeldekortDagDTO(
                dato = dag.dato.format(norskDatoFormatter),
                tiltakType = dag.tiltakstype.toString(),
                status = dag.toStatus(),
                beløp = dag.beløp,
                beløpBarnetillegg = dag.beløpBarnetillegg,
                prosent = dag.prosent,
                reduksjon = dag.toReduksjon(),
            )
        },
        eksternGjennomføringId = eksternGjennomføringId,
        tiltakstype = meldekortbehandling.tiltakstype.name,
        tiltaksnavn = tiltaksnavn,
        eksternDeltagelseId = eksternDeltagelseId,
        iverksattTidspunkt = opprettet.format(norskTidspunktFormatter),
    ).let { serialize(it) }
}

private suspend fun tilSaksbehadlerDto(navIdent: String, hentSaksbehandlersNavn: suspend (String) -> String): UtbetalingsvedtakDTO.SaksbehandlerDTO {
    return UtbetalingsvedtakDTO.SaksbehandlerDTO(navn = hentSaksbehandlersNavn(navIdent), navIdent = navIdent)
}

private fun MeldeperiodeBeregningDag.toStatus(): String {
    return when (this) {
        is MeldeperiodeBeregningDag.IkkeUtfylt -> "Ikke utfylt"
        is MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket -> "Deltatt med lønn i tiltaket"
        is MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket -> "Deltatt uten lønn i tiltaket"
        is MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker -> "Syk bruker"
        is MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn -> "Sykt barn"
        is MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav -> "Velferd godkjent av Nav"
        is MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav -> "Velferd ikke godkjent av Nav"
        is MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt -> "Ikke deltatt"
        is MeldeperiodeBeregningDag.Utfylt.Sperret -> "Ikke rett på tiltakspenger"
    }
}

private fun MeldeperiodeBeregningDag.toReduksjon(): String? {
    return when (reduksjon) {
        ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon -> "Ingen reduksjon"
        ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon -> "Reduksjon"
        ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort -> "Ytelsen faller bort"
        null -> null
    }
}
