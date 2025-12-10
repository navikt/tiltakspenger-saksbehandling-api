package no.nav.tiltakspenger.saksbehandling.dokument.infra

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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

data class BrevMeldekortvedtakDTO(
    val meldekortId: String,
    val saksnummer: String,
    val meldekortPeriode: PeriodeDTO,
    val saksbehandler: SaksbehandlerDTO?,
    val beslutter: SaksbehandlerDTO?,
    val tiltak: List<TiltakDTO>,
    val iverksattTidspunkt: String?,
    val fødselsnummer: String,
    val sammenligningAvBeregninger: SammenligningAvBeregningerDTO,
    val korrigering: Boolean,
    val totaltBelop: Int,
    val brevTekst: String?,
    val forhandsvisning: Boolean,
) {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = SaksbehandlerDTO.Automatisk::class, name = "AUTOMATISK"),
        JsonSubTypes.Type(value = SaksbehandlerDTO.Manuell::class, name = "MANUELL"),
    )
    sealed interface SaksbehandlerDTO {
        data object Automatisk : SaksbehandlerDTO
        data class Manuell(val navn: String) : SaksbehandlerDTO
    }

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
    forhåndsvisning: Boolean,
): String {
    return BrevMeldekortvedtakDTO(
        fødselsnummer = fnr.verdi,
        saksbehandler = saksbehandler.tilSaksbehandlerDto(hentSaksbehandlersNavn),
        beslutter = beslutter.tilSaksbehandlerDto(hentSaksbehandlersNavn),
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
        brevTekst = this.meldekortBehandling.fritekstTilVedtaksbrev?.verdi,
        forhandsvisning = forhåndsvisning,
    ).let { serialize(it) }
}

fun Meldekortvedtak.toBeregningSammenligningDTO(
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

suspend fun String.tilSaksbehandlerDto(
    hentSaksbehandlersNavn: suspend (String) -> String,
): BrevMeldekortvedtakDTO.SaksbehandlerDTO? = when (this) {
    AUTOMATISK_SAKSBEHANDLER_ID -> BrevMeldekortvedtakDTO.SaksbehandlerDTO.Automatisk
    else -> BrevMeldekortvedtakDTO.SaksbehandlerDTO.Manuell(navn = hentSaksbehandlersNavn(this))
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
