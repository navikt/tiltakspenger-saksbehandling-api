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

// TODO - inn med tekstTilBrev
data class BrevMeldekortvedtakDTO(
    val meldekortId: String,
    val saksnummer: String,
    val meldekortPeriode: PeriodeDTO,
    val saksbehandler: SaksbehandlerDTO,
    val beslutter: SaksbehandlerDTO?,
    val tiltak: List<TiltakDTO>,
    // TODO - sjekk at denne ikke er problematisk som null i pdf
    val iverksattTidspunkt: String?,
    val fødselsnummer: String,
    val sammenligningAvBeregninger: SammenligningAvBeregningerDTO,
    val korrigering: Boolean,
    val totaltBelop: Int,
    val brevTekst: String?,
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

fun SammenligningAvBeregninger.MeldeperiodeSammenligninger.toDTO(): BrevMeldekortvedtakDTO.MeldeperiodeSammenligningerDTO {
    val fraOgMed = periode.fraOgMed.format(norskDatoFormatter)
    val tilOgMed = periode.tilOgMed.format(norskDatoFormatter)
    val tittel = "Meldekort $fraOgMed - $tilOgMed"
    val harBarnetillegg = this.dager.any {
        it.barnetillegg.gjeldende > 0 ||
            (it.barnetillegg.forrige ?: 0) > 0
    }

    return BrevMeldekortvedtakDTO.MeldeperiodeSammenligningerDTO(
        tittel = tittel,
        differanseFraForrige = this.differanseFraForrige,
        dager = this.dager.toDto(),
        harBarnetillegg = harBarnetillegg,
    )
}

// TODO: må tilpasses utbetalinger fra revurdering
suspend fun Meldekortvedtak.toJsonRequest(
    hentSaksbehandlersNavn: suspend (String) -> String,
    tiltaksdeltakelser: Tiltaksdeltakelser,
    sammenlign: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
): String {
    return BrevMeldekortvedtakDTO(
        fødselsnummer = fnr.verdi,
        saksbehandler = tilSaksbehandlerDto(saksbehandler, hentSaksbehandlersNavn),
        beslutter = if (saksbehandler == AUTOMATISK_SAKSBEHANDLER_ID && beslutter == AUTOMATISK_SAKSBEHANDLER_ID) {
            null
        } else {
            tilSaksbehandlerDto(beslutter, hentSaksbehandlersNavn)
        },
        meldekortId = meldekortId.toString(),
        saksnummer = saksnummer.toString(),
        meldekortPeriode = BrevMeldekortvedtakDTO.PeriodeDTO(
            fom = beregningsperiode.fraOgMed.format(norskDatoFormatter),
            tom = beregningsperiode.tilOgMed.format(norskDatoFormatter),
        ),
        tiltak = tiltaksdeltakelser.map { it.toTiltakDTO() },
        iverksattTidspunkt = opprettet.format(norskTidspunktFormatter),
        korrigering = erKorrigering,
        sammenligningAvBeregninger = toBeregningSammenligningDTO(sammenlign),
        totaltBelop = meldekortBehandling.beløpTotal,
        brevTekst = this.meldekortBehandling.tekstTilVedtaksbrev?.value,
    ).let { serialize(it) }
}

private fun Meldekortvedtak.toBeregningSammenligningDTO(
    sammenlign: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
): BrevMeldekortvedtakDTO.SammenligningAvBeregningerDTO {
    return this.utbetaling.beregning.beregninger
        .map { beregninger -> sammenlign(beregninger) }
        .map { it.toDTO() }
        .let { meldeperiodeSammenligninger ->
            // Kommentar: Bug rundt serialisering av NonEmptyList gjør at vi konverterer til standard kotlin list
            BrevMeldekortvedtakDTO.SammenligningAvBeregningerDTO(
                meldeperioder = meldeperiodeSammenligninger.toList(),
                totalDifferanse = meldeperiodeSammenligninger.toList().sumOf { it.differanseFraForrige },
            )
        }
}

fun List<SammenligningAvBeregninger.DagSammenligning>.toDto(): List<BrevMeldekortvedtakDTO.DagSammenligningDTO> =
    this.map { it.toDto() }

fun SammenligningAvBeregninger.DagSammenligning.toDto(): BrevMeldekortvedtakDTO.DagSammenligningDTO =
    BrevMeldekortvedtakDTO.DagSammenligningDTO(
        dato = this.dato.format(norskUkedagOgDatoUtenÅrFormatter),
        status = BrevMeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
            forrige = this.status.forrige?.toStatus(),
            gjeldende = this.status.gjeldende.toStatus(),
        ),
        beløp = BrevMeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
            forrige = this.beløp.forrige,
            gjeldende = this.beløp.gjeldende,
        ),
        barnetillegg = BrevMeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
            forrige = this.barnetillegg.forrige,
            gjeldende = this.barnetillegg.gjeldende,
        ),
        prosent = BrevMeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
            forrige = this.prosent.forrige,
            gjeldende = this.prosent.gjeldende,
        ),
    )

fun Tiltaksdeltakelse.toTiltakDTO() =
    BrevMeldekortvedtakDTO.TiltakDTO(
        tiltakstypenavn = typeNavn,
        tiltakstype = typeKode.name,
        eksternDeltagelseId = eksternDeltakelseId,
        eksternGjennomføringId = gjennomføringId,
    )

suspend fun tilSaksbehandlerDto(
    navIdent: String,
    hentSaksbehandlersNavn: suspend (String) -> String,
): BrevMeldekortvedtakDTO.SaksbehandlerDTO {
    return BrevMeldekortvedtakDTO.SaksbehandlerDTO(navn = hentSaksbehandlersNavn(navIdent))
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
