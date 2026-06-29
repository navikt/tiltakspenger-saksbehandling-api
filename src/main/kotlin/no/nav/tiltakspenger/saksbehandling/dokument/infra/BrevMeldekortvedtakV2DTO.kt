package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.dato.norskTidspunktFormatter
import no.nav.tiltakspenger.libs.dato.norskUkedagOgDatoUtenÅrFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingMedBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak

private typealias SammenlignMeldeperioder = (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger

private data class BrevMeldekortvedtakV2DTO(
    val meldekortId: String,
    val saksnummer: String,
    val periode: BrevPeriodeDTO,
    val erAutomatiskBehandlet: Boolean,
    val saksbehandlerNavn: String?,
    val beslutterNavn: String?,
    val tiltak: List<String>,
    val iverksattTidspunkt: String?,
    val fødselsnummer: String,
    val meldeperioder: List<BrevMeldeperiode>,
    val totaltBelop: Int,
    val totalDifferanse: Int,
    val brevTekst: String?,
    val forhandsvisning: Boolean,
) {

    data class BrevMeldeperiode(
        val korrigering: Boolean,
        val periode: BrevPeriodeDTO,
        val beløp: Int,
        val beløpDiff: Int,
        val harBarnetillegg: Boolean,
        val dager: List<Dag>,
    )

    data class Dag(
        val dato: String,
        val status: ForrigeOgGjeldendeDTO<String>,
        val beløp: ForrigeOgGjeldendeDTO<Int>,
        val barnetillegg: ForrigeOgGjeldendeDTO<Int>,
        val prosent: ForrigeOgGjeldendeDTO<Int>,
    ) {
        val harEndring: Boolean = status.harEndring || beløp.harEndring || barnetillegg.harEndring || prosent.harEndring
    }

    data class ForrigeOgGjeldendeDTO<T>(
        val forrige: T?,
        val gjeldende: T,
    ) {
        val harEndring: Boolean = forrige != gjeldende
    }

    init {
        if (erAutomatiskBehandlet) {
            require(saksbehandlerNavn == null && beslutterNavn == null) {
                "Skal ikke ha saksbehandler og beslutter ved automatisk behandling"
            }
        } else {
            require(saksbehandlerNavn != null && beslutterNavn != null) {
                "Må ha saksbehandler og beslutter ved manuell behandling"
            }
        }
    }
}

private fun MeldeperiodebehandlingMedBeregning.tilBrevMeldeperiode(
    sammenlign: SammenlignMeldeperioder,
): BrevMeldekortvedtakV2DTO.BrevMeldeperiode {
    requireNotNull(meldeperiodeberegning)

    val sammenligning = sammenlign(meldeperiodeberegning)

    val harBarnetillegg = sammenligning.dager.any {
        it.barnetillegg.gjeldende > 0 ||
            (it.barnetillegg.forrige ?: 0) > 0
    }

    return BrevMeldekortvedtakV2DTO.BrevMeldeperiode(
        korrigering = meldeperiodebehandling.erKorrigering,
        periode = BrevPeriodeDTO.fraPeriode(meldeperiodebehandling.periode),
        beløp = meldeperiodeberegning.dager.sumOf { it.totalBeløp },
        beløpDiff = sammenligning.differanseFraForrige,
        harBarnetillegg = harBarnetillegg,
        dager = sammenligning.dager.map { it.tilBrevDag() },
    )
}

private fun SammenligningAvBeregninger.DagSammenligning.tilBrevDag(): BrevMeldekortvedtakV2DTO.Dag =
    BrevMeldekortvedtakV2DTO.Dag(
        dato = dato.format(norskUkedagOgDatoUtenÅrFormatter),
        status = BrevMeldekortvedtakV2DTO.ForrigeOgGjeldendeDTO(
            forrige = status.forrige?.toStatus(),
            gjeldende = status.gjeldende.toStatus(),
        ),
        beløp = BrevMeldekortvedtakV2DTO.ForrigeOgGjeldendeDTO(
            forrige = beløp.forrige,
            gjeldende = beløp.gjeldende,
        ),
        barnetillegg = BrevMeldekortvedtakV2DTO.ForrigeOgGjeldendeDTO(
            forrige = barnetillegg.forrige,
            gjeldende = barnetillegg.gjeldende,
        ),
        prosent = BrevMeldekortvedtakV2DTO.ForrigeOgGjeldendeDTO(
            forrige = prosent.forrige,
            gjeldende = prosent.gjeldende,
        ),
    )

private fun MeldeperiodeBeregningDag.toStatus(): String {
    return when (this) {
        is MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket -> "Deltatt med lønn"
        is MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket -> "Deltatt"
        is MeldeperiodeBeregningDag.Fravær.Syk.SykBruker -> "Syk"
        is MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn -> "Sykt barn eller syk barnepasser"
        is MeldeperiodeBeregningDag.Fravær.Velferd.FraværSterkeVelferdsgrunnerEllerJobbintervju -> "Sterke velferdsgrunner eller jobbintervju"
        is MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav -> "Fravær godkjent av Nav"
        is MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet -> "Annet fravær"
        is MeldeperiodeBeregningDag.IkkeBesvart -> "Ikke besvart"
        is MeldeperiodeBeregningDag.IkkeDeltatt -> "Ikke tiltaksdag"
        is MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger -> "Ikke rett til tiltakspenger"
    }
}

suspend fun Meldekortvedtak.toJsonRequestV2(
    hentSaksbehandlersNavn: suspend (String) -> String,
    tiltaksdeltakelser: Tiltaksdeltakelser,
    sammenlign: SammenlignMeldeperioder,
    forhåndsvisning: Boolean,
): String {
    val meldeperioder = meldeperioderMedBeregninger.map { it.tilBrevMeldeperiode(sammenlign) }

    return BrevMeldekortvedtakV2DTO(
        fødselsnummer = fnr.verdi,
        erAutomatiskBehandlet = erAutomatiskBehandlet,
        saksbehandlerNavn = if (erAutomatiskBehandlet) null else hentSaksbehandlersNavn(saksbehandler),
        beslutterNavn = if (erAutomatiskBehandlet) null else hentSaksbehandlersNavn(beslutter),
        meldekortId = meldekortId.toString(),
        saksnummer = saksnummer.toString(),
        periode = BrevPeriodeDTO.fraPeriode(beregningsperiode),
        tiltak = tiltaksdeltakelser.map { it.typeNavn },
        iverksattTidspunkt = opprettet.format(norskTidspunktFormatter),
        brevTekst = this.meldekortbehandling.fritekstTilVedtaksbrev?.verdi,
        forhandsvisning = forhåndsvisning,
        totaltBelop = meldekortbehandling.beløpTotal,
        totalDifferanse = meldeperioder.sumOf { it.beløpDiff },
        meldeperioder = meldeperioder,
    ).let { serialize(it) }
}
