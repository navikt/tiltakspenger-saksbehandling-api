package no.nav.tiltakspenger.saksbehandling.dokument.infra

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.periodisering.norskTidspunktFormatter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak

private data class UtbetalingsvedtakDTO(
    val meldekortId: String,
    val saksnummer: String,
    val meldekortPeriode: PeriodeDTO,
    val saksbehandler: SaksbehandlerDTO,
    val beslutter: SaksbehandlerDTO,
    val tiltak: List<TiltakDTO>,
    val iverksattTidspunkt: String,
    val fødselsnummer: String,
    val begrunnelse: String? = null,
    val sammenligningAvBeregninger: SammenligningAvBeregningerDTO,
    val korrigering: Boolean,
) {
    @Suppress("unused")
    @JsonInclude
    val barnetillegg: Boolean = sammenligningAvBeregninger.meldeperioder
        .flatMap { it.dager }
        .any { it.barnetillegg.gjeldende > 0 }

    data class SaksbehandlerDTO(
        val navn: String,
    )

    data class PeriodeDTO(
        val fom: String,
        val tom: String,
    )

    data class TiltakDTO(
        val tiltakstypenavn: String,
        val tiltakstype: String,
        val eksternDeltagelseId: String,
        val eksternGjennomføringId: String?,
    )

    data class SammenligningAvBeregningerDTO(
        val meldeperioder: List<MeldeperiodeSammenligningerDTO>,
        val begrunnelse: String?,
        val totalDifferanse: Int,
    )

    data class MeldeperiodeSammenligningerDTO(
        val tittel: String,
        val differanseFraForrige: Int,
        val dager: List<DagSammenligningDTO>,
    )

    data class DagSammenligningDTO(
        val dato: String,
        val status: ForrigeOgGjeldendeDTO<String>,
        val beløp: ForrigeOgGjeldendeDTO<Int>,
        val barnetillegg: ForrigeOgGjeldendeDTO<Int>,
        val prosent: ForrigeOgGjeldendeDTO<Int>,
        val harEndretSeg: Boolean = status.harEndretSeg || beløp.harEndretSeg || barnetillegg.harEndretSeg || prosent.harEndretSeg,
    )

    data class ForrigeOgGjeldendeDTO<T>(
        val forrige: T?,
        val gjeldende: T,
        val harEndretSeg: Boolean = forrige != null && forrige != gjeldende,
    )
}

suspend fun Utbetalingsvedtak.toJsonRequest(
    hentSaksbehandlersNavn: suspend (String) -> String,
    tiltaksdeltagelser: List<Tiltaksdeltagelse>,
    sammenlign: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
): String {
    return UtbetalingsvedtakDTO(
        fødselsnummer = fnr.verdi,
        saksbehandler = tilSaksbehandlerDto(saksbehandler, hentSaksbehandlersNavn),
        beslutter = tilSaksbehandlerDto(beslutter, hentSaksbehandlersNavn),
        meldekortId = meldekortId.toString(),
        saksnummer = saksnummer.toString(),
        meldekortPeriode = UtbetalingsvedtakDTO.PeriodeDTO(
            fom = periode.fraOgMed.format(norskDatoFormatter),
            tom = periode.tilOgMed.format(norskDatoFormatter),
        ),
        tiltak = tiltaksdeltagelser.map { it.toTiltakDTO() },
        iverksattTidspunkt = opprettet.format(norskTidspunktFormatter),
        korrigering = meldekortbehandling.type == MeldekortBehandlingType.KORRIGERING,
        sammenligningAvBeregninger = toBeregningSammenligningDTO(sammenlign),
    ).let { serialize(it) }
}

private fun Utbetalingsvedtak.toBeregningSammenligningDTO(
    sammenlign: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
): UtbetalingsvedtakDTO.SammenligningAvBeregningerDTO {
    return this.meldekortbehandling.beregning.beregninger
        .map { beregninger -> sammenlign(beregninger) }
        .map { sammenligningPerMeldeperiode ->
            sammenligningPerMeldeperiode.periode.let { periode ->
                val fraOgMed = periode.fraOgMed.format(norskDatoFormatter)
                val tilOgMed = periode.tilOgMed.format(norskDatoFormatter)
                val tittel = "Meldekort $fraOgMed - $tilOgMed"

                UtbetalingsvedtakDTO.MeldeperiodeSammenligningerDTO(
                    tittel = tittel,
                    differanseFraForrige = sammenligningPerMeldeperiode.differanseFraForrige,
                    dager = sammenligningPerMeldeperiode.dager.map { dag ->
                        UtbetalingsvedtakDTO.DagSammenligningDTO(
                            dato = dag.dato,
                            status = UtbetalingsvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.status.forrige?.toStatus(),
                                gjeldende = dag.status.gjeldende.toStatus(),
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
        }.let { meldeperiodeSammenligninger ->
            // Kommentar: Bug rundt serialisering av NonEmptyList gjør at vi konverterer til standard kotlin list
            UtbetalingsvedtakDTO.SammenligningAvBeregningerDTO(
                meldeperioder = meldeperiodeSammenligninger.toList(),
                begrunnelse = this.meldekortbehandling.begrunnelse?.verdi,
                totalDifferanse = meldeperiodeSammenligninger.toList().sumOf { it.differanseFraForrige },
            )
        }
}

private fun Tiltaksdeltagelse.toTiltakDTO() =
    UtbetalingsvedtakDTO.TiltakDTO(
        tiltakstypenavn = typeNavn,
        tiltakstype = typeKode.name,
        eksternDeltagelseId = eksternDeltagelseId,
        eksternGjennomføringId = gjennomføringId,
    )

private suspend fun tilSaksbehandlerDto(
    navIdent: String,
    hentSaksbehandlersNavn: suspend (String) -> String,
): UtbetalingsvedtakDTO.SaksbehandlerDTO {
    return UtbetalingsvedtakDTO.SaksbehandlerDTO(navn = hentSaksbehandlersNavn(navIdent))
}

private fun MeldeperiodeBeregningDag.toStatus(): String {
    return when (this) {
        is MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket -> "Deltatt med lønn i tiltaket"
        is MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket -> "Deltatt uten lønn i tiltaket"
        is MeldeperiodeBeregningDag.Fravær.Syk.SykBruker -> "Fravær - Syk"
        is MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn -> "Fravær - Sykt barn"
        is MeldeperiodeBeregningDag.Fravær.Velferd.VelferdGodkjentAvNav -> "Godkjent fravær - Velferd"
        is MeldeperiodeBeregningDag.Fravær.Velferd.VelferdIkkeGodkjentAvNav -> "Ikke godkjent fravær - Velferd"
        is MeldeperiodeBeregningDag.IkkeDeltatt -> "Ikke tiltak denne dagen"
        is MeldeperiodeBeregningDag.Sperret -> "Ikke rett på tiltakspenger"
    }
}
