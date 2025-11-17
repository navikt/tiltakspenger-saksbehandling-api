package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.periodisering.norskTidspunktFormatter
import no.nav.tiltakspenger.libs.periodisering.norskUkedagOgDatoUtenÅrFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse

private data class MeldekortvedtakDTO(
    val meldekortId: String,
    val saksnummer: String,
    val meldekortPeriode: PeriodeDTO,
    val saksbehandler: SaksbehandlerDTO,
    val beslutter: SaksbehandlerDTO?,
    val tiltak: List<TiltakDTO>,
    val iverksattTidspunkt: String,
    val fødselsnummer: String,
    val begrunnelse: String? = null,
    val sammenligningAvBeregninger: SammenligningAvBeregningerDTO,
    val korrigering: Boolean,
    val totaltBelop: Int,
) {
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
        val harBarnetillegg: Boolean,
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

// TODO: må tilpasses utbetalinger fra revurdering
suspend fun Meldekortvedtak.toJsonRequest(
    hentSaksbehandlersNavn: suspend (String) -> String,
    tiltaksdeltakelser: Tiltaksdeltakelser,
    sammenlign: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
): String {
    return MeldekortvedtakDTO(
        fødselsnummer = fnr.verdi,
        saksbehandler = tilSaksbehandlerDto(saksbehandler, hentSaksbehandlersNavn),
        beslutter = if (saksbehandler == AUTOMATISK_SAKSBEHANDLER_ID && beslutter == AUTOMATISK_SAKSBEHANDLER_ID) {
            null
        } else {
            tilSaksbehandlerDto(beslutter, hentSaksbehandlersNavn)
        },
        meldekortId = meldekortId.toString(),
        saksnummer = saksnummer.toString(),
        meldekortPeriode = MeldekortvedtakDTO.PeriodeDTO(
            fom = beregningsperiode.fraOgMed.format(norskDatoFormatter),
            tom = beregningsperiode.tilOgMed.format(norskDatoFormatter),
        ),
        tiltak = tiltaksdeltakelser.map { it.toTiltakDTO() },
        iverksattTidspunkt = opprettet.format(norskTidspunktFormatter),
        korrigering = erKorrigering,
        sammenligningAvBeregninger = toBeregningSammenligningDTO(sammenlign),
        totaltBelop = meldekortBehandling.beløpTotal,
    ).let { serialize(it) }
}

private fun Meldekortvedtak.toBeregningSammenligningDTO(
    sammenlign: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
): MeldekortvedtakDTO.SammenligningAvBeregningerDTO {
    return this.utbetaling.beregning.beregninger
        .map { beregninger -> sammenlign(beregninger) }
        .map { sammenligningPerMeldeperiode ->
            sammenligningPerMeldeperiode.periode.let { periode ->
                val fraOgMed = periode.fraOgMed.format(norskDatoFormatter)
                val tilOgMed = periode.tilOgMed.format(norskDatoFormatter)
                val tittel = "Meldekort $fraOgMed - $tilOgMed"
                val harBarnetillegg =
                    sammenligningPerMeldeperiode.dager.any {
                        it.barnetillegg.gjeldende > 0 ||
                            (it.barnetillegg.forrige ?: 0) > 0
                    }

                MeldekortvedtakDTO.MeldeperiodeSammenligningerDTO(
                    tittel = tittel,
                    differanseFraForrige = sammenligningPerMeldeperiode.differanseFraForrige,
                    dager = sammenligningPerMeldeperiode.dager.map { dag ->
                        MeldekortvedtakDTO.DagSammenligningDTO(
                            dato = dag.dato.format(norskUkedagOgDatoUtenÅrFormatter),
                            status = MeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.status.forrige?.toStatus(),
                                gjeldende = dag.status.gjeldende.toStatus(),
                            ),
                            beløp = MeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.beløp.forrige,
                                gjeldende = dag.beløp.gjeldende,
                            ),
                            barnetillegg = MeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.barnetillegg.forrige,
                                gjeldende = dag.barnetillegg.gjeldende,
                            ),
                            prosent = MeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
                                forrige = dag.prosent.forrige,
                                gjeldende = dag.prosent.gjeldende,
                            ),
                        )
                    },
                    harBarnetillegg = harBarnetillegg,
                )
            }
        }.let { meldeperiodeSammenligninger ->
            // Kommentar: Bug rundt serialisering av NonEmptyList gjør at vi konverterer til standard kotlin list
            MeldekortvedtakDTO.SammenligningAvBeregningerDTO(
                meldeperioder = meldeperiodeSammenligninger.toList(),
                begrunnelse = begrunnelse,
                totalDifferanse = meldeperiodeSammenligninger.toList().sumOf { it.differanseFraForrige },
            )
        }
}

private fun Tiltaksdeltakelse.toTiltakDTO() =
    MeldekortvedtakDTO.TiltakDTO(
        tiltakstypenavn = typeNavn,
        tiltakstype = typeKode.name,
        eksternDeltagelseId = eksternDeltakelseId,
        eksternGjennomføringId = gjennomføringId,
    )

private suspend fun tilSaksbehandlerDto(
    navIdent: String,
    hentSaksbehandlersNavn: suspend (String) -> String,
): MeldekortvedtakDTO.SaksbehandlerDTO {
    return MeldekortvedtakDTO.SaksbehandlerDTO(navn = hentSaksbehandlersNavn(navIdent))
}

private fun MeldeperiodeBeregningDag.toStatus(): String {
    return when (this) {
        is MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket -> "Deltatt med lønn"
        is MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket -> "Deltatt"
        is MeldeperiodeBeregningDag.Fravær.Syk.SykBruker -> "Syk"
        is MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn -> "Sykt barn eller syk barnepasser"
        is MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav -> "Fravær godkjent av Nav"
        is MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet -> "Annet fravær"
        is MeldeperiodeBeregningDag.IkkeBesvart -> "Ikke besvart"
        is MeldeperiodeBeregningDag.IkkeDeltatt -> "Ikke tiltaksdag"
        is MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger -> "Ikke rett til tiltakspenger"
    }
}
