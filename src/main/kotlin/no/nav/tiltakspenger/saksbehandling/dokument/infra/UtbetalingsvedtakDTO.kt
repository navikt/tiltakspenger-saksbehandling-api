package no.nav.tiltakspenger.saksbehandling.dokument.infra

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.periodisering.norskTidspunktFormatter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak

private data class UtbetalingsvedtakDTO(
    val meldekortId: String,
    val saksnummer: String,
    val meldekortPeriode: PeriodeDTO,
    val saksbehandler: SaksbehandlerDTO,
    val beslutter: SaksbehandlerDTO,
    val meldekortDager: List<MeldekortDagDTO>, // TODO Erstattes av beregningSammenligning
    val tiltak: List<TiltakDTO>,
    val iverksattTidspunkt: String,
    val fødselsnummer: String,
    val sammenligningAvBeregninger: SammenligningAvBeregningerDTO,
) {
    @Suppress("unused")
    @JsonInclude
    val barnetillegg: Boolean = meldekortDager.map { it.beløpBarnetillegg }.any { it > 0 }

    data class SaksbehandlerDTO(
        val navn: String,
    )

    data class PeriodeDTO(
        val fom: String,
        val tom: String,
    )

    data class MeldekortDagDTO(
        val dato: String,
        val status: String,
        val beløp: Int,
        val beløpBarnetillegg: Int,
        val prosent: Int,
        val reduksjon: String?,
    )

    data class TiltakDTO(
        val tiltakstypenavn: String,
        val tiltakstype: String,
        val eksternDeltagelseId: String,
        val eksternGjennomføringId: String?,
    )

    data class SammenligningAvBeregningerDTO(
        val meldeperioder: List<MeldeperiodeSammenligningerDTO>,
    )

    data class MeldeperiodeSammenligningerDTO(
        val tittel: String,
        val dager: List<DagSammenligningDTO>,
    )

    data class DagSammenligningDTO(
        val dato: String,
        val status: ForrigeOgGjeldendeDTO<String>,
        val beløp: ForrigeOgGjeldendeDTO<Int>,
        val barnetillegg: ForrigeOgGjeldendeDTO<Int>,
        val prosent: ForrigeOgGjeldendeDTO<Int>,
    )

    data class ForrigeOgGjeldendeDTO<T>(
        val forrige: T?,
        val gjeldende: T,
    )
}

suspend fun Utbetalingsvedtak.toJsonRequest(
    hentSaksbehandlersNavn: suspend (String) -> String,
    tiltaksdeltagelser: List<Tiltaksdeltagelse>,
    sammenlign: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
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
        // Kommentar: Bug rundt serialisering av NonEmptyList gjør at vi konverterer til standard kotlin List før mapping
        meldekortDager = meldekortbehandling.beregning.alleDagerBeregnet.toList().map { dag ->
            UtbetalingsvedtakDTO.MeldekortDagDTO(
                dato = dag.dato.format(norskDatoFormatter),
                status = dag.toStatus(),
                beløp = dag.beløp,
                beløpBarnetillegg = dag.beløpBarnetillegg,
                prosent = dag.prosent,
                reduksjon = dag.toReduksjon(),
            )
        },
        tiltak = tiltaksdeltagelser.map { it.toTiltakDTO() },
        iverksattTidspunkt = opprettet.format(norskTidspunktFormatter),
        sammenligningAvBeregninger = toBeregningSammenligningDTO(sammenlign),
    ).let { serialize(it) }
}

private fun Utbetalingsvedtak.toBeregningSammenligningDTO(
    sammenlign: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
): UtbetalingsvedtakDTO.SammenligningAvBeregningerDTO {
    return this.meldekortbehandling.beregning.beregninger
        .map { sammenlign(it) }
        .map { sammenligningPerMeldeperiode ->
            sammenligningPerMeldeperiode.periode.let { periode ->
                val fraOgMed = periode.fraOgMed.format(norskDatoFormatter)
                val tilOgMed = periode.tilOgMed.format(norskDatoFormatter)
                val tittel = "Meldekort $fraOgMed - $tilOgMed"

                UtbetalingsvedtakDTO.MeldeperiodeSammenligningerDTO(
                    tittel = tittel,
                    dager = sammenligningPerMeldeperiode.dager.map { dag ->
                        UtbetalingsvedtakDTO.DagSammenligningDTO(
                            dato = dag.dato,
                            status = UtbetalingsvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.status.forrige,
                                gjeldende = dag.status.gjeldende,
                            ),
                            beløp = UtbetalingsvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.beløp.forrige,
                                gjeldende = dag.beløp.gjeldende,
                            ),
                            barnetillegg = UtbetalingsvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.barnetillegg.forrige,
                                gjeldende = dag.barnetillegg.gjeldende,
                            ),
                            prosent = UtbetalingsvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.prosent.forrige,
                                gjeldende = dag.prosent.gjeldende,
                            ),
                        )
                    },
                )
            }
        }.let {
            // Kommentar: Bug rundt serialisering av NonEmptyList gjør at vi konverterer til standard kotlin list
            UtbetalingsvedtakDTO.SammenligningAvBeregningerDTO(meldeperioder = it.toList())
        }
}

private fun Tiltaksdeltagelse.toTiltakDTO() =
    UtbetalingsvedtakDTO.TiltakDTO(
        tiltakstypenavn = typeNavn,
        tiltakstype = typeKode.name,
        eksternDeltagelseId = eksternDeltagelseId,
        eksternGjennomføringId = gjennomføringId,
    )

private suspend fun tilSaksbehadlerDto(
    navIdent: String,
    hentSaksbehandlersNavn: suspend (String) -> String,
): UtbetalingsvedtakDTO.SaksbehandlerDTO {
    return UtbetalingsvedtakDTO.SaksbehandlerDTO(navn = hentSaksbehandlersNavn(navIdent))
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
