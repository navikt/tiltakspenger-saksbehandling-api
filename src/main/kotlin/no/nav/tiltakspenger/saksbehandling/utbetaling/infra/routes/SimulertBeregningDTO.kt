package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BeløpFørOgNåDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BeregningerSummertDTO
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.tilMeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Postering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsflagg
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsmerke
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning.SimulertBeregningMeldeperiode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning.SimulertBeregningMeldeperiode.SimulertBeregningDag
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param behandlingId meldekortbehandlingId eller rammebehandlingId.
 * @param simuleringstidspunkt er tidspunktet simuleringen ble utført.
 * Kan være null hvis simulering ikke er utført, eller simuleringen ble utført før vi la på dette feltet.
 * @param simuleringsdato kommer fra Økonomisystemet.
 * @param simuleringTotalBeløp kommer fra Økonomisystemet.
 */
data class SimulertBeregningDTO(
    val behandlingId: String,
    val behandlingstype: Behandlingstype,
    val meldeperioder: List<SimulertBeregningMeldeperiode>,
    val beregningstidspunkt: LocalDateTime,
    val simuleringstidspunkt: LocalDateTime?,
    val simuleringsdato: LocalDate?,
    val simuleringTotalBeløp: Int?,
    val simulerteBeløp: SimulerteBeløp?,
    val simuleringResultat: SimuleringResultatDTO,
    val beregning: BeregningerSummertDTO,
) {

    enum class Behandlingstype {
        MELDEKORT,
        RAMME,
    }

    data class SimulerteBeløp(
        val feilutbetaling: Int,
        val etterbetaling: Int,
        val tidligereUtbetaling: Int,
        val nyUtbetaling: Int,
        val totalJustering: Int,
        val totalTrekk: Int,
    )

    data class SimulertBeregningMeldeperiode(
        val kjedeId: String,
        val dager: List<SimulertBeregningDag>,
        val simulerteBeløp: SimulerteBeløp?,
        val beregning: BeregningerSummertDTO,
        val flagg: SimuleringsflaggDTO,

        /**
         * Posteringene fra oppdragssystemet som treffer meldeperioden, med kildens egne beløp og perioder.
         * Dagenes merker sier hvilke dager som er dekket; beløpene per postering hører hjemme her.
         */
        val posteringer: List<SimuleringsposteringDTO>,
    ) {

        data class SimulertBeregningDag(
            val dato: LocalDate,
            val status: MeldekortDagStatusDTO?,
            val beregning: BeregningerSummertDTO?,

            /**
             * Dagsverdier utledet fra posteringene, med avrunding vi selv har valgt.
             * Frontenden skal over på [merker] -- dagen skal si noe om simuleringsstatusen sin, ikke vise beløp kilden aldri har fordelt per dag.
             * Feltet står til frontenden har sluttet å lese det, så gamle klienter ikke knekker mot ny backend.
             */
            val simulerteBeløp: SimulerteBeløp?,
            val merker: List<SimuleringsmerkeDTO>,
        )
    }

    /**
     * Fakta om hva simuleringen sier, ikke en dom over hva som skal skje.
     * Klienten avgjør hva som gir advarsel og hva som bare er informasjon.
     */
    data class SimuleringsflaggDTO(
        val harJustering: Boolean,
        val justeringGårOppINull: Boolean,
        val justeringPåTversAvMeldeperiodeEllerMåned: Boolean,
        val harFeilutbetaling: Boolean,
        val harTrekk: Boolean,
    )

    /**
     * Hva oppdragssystemet har å melde om én dag.
     *
     * @param beløp er satt kun når posteringen dekker nøyaktig én dag, og beløpet dermed er kildens eget.
     * Er det null, finnes det ingen dagsandel -- vis [periodeFraOgMed]--[periodeTilOgMed] i stedet.
     * @param erJustering utledes av klassekoden, ikke av [type], siden oppdragssystemet har sendt justeringer under flere typer.
     * Utledningen bor i backend slik at frontenden slipper å kjenne klassekodene.
     * @param erNegativt er posteringsbeløpets fortegn, som er kildedata også når [beløp] er null fordi posteringen dekker flere dager.
     */
    data class SimuleringsmerkeDTO(
        val type: PosteringstypeDTO,
        val periodeFraOgMed: LocalDate,
        val periodeTilOgMed: LocalDate,
        val klassekode: String,
        val beløp: Int?,
        val erJustering: Boolean,
        val erNegativt: Boolean,
    )

    /**
     * Én postering slik oppdragssystemet sendte den, knyttet til meldeperioden den treffer.
     *
     * @param beløp er alltid satt -- det er kildens eget beløp for posteringens periode, uten dagsfordeling.
     * @param erJustering utledes av klassekoden, som for [SimuleringsmerkeDTO].
     */
    data class SimuleringsposteringDTO(
        val type: PosteringstypeDTO,
        val periodeFraOgMed: LocalDate,
        val periodeTilOgMed: LocalDate,
        val klassekode: String,
        val beløp: Int,
        val erJustering: Boolean,
    )

    enum class PosteringstypeDTO {
        YTELSE,
        FEILUTBETALING,
        FORSKUDSSKATT,
        JUSTERING,
        TREKK,
        MOTPOSTERING,
    }

    enum class SimuleringResultatDTO {
        ENDRING,
        INGEN_ENDRING,
        IKKE_SIMULERT,
    }
}

fun SimulertBeregning.toSimulertBeregningDTO(): SimulertBeregningDTO {
    return SimulertBeregningDTO(
        behandlingId = this.beregningskilde.id.toString(),
        behandlingstype = when (this.beregningskilde) {
            is BeregningKilde.BeregningKildeRammebehandling -> SimulertBeregningDTO.Behandlingstype.RAMME
            is BeregningKilde.BeregningKildeMeldekort -> SimulertBeregningDTO.Behandlingstype.MELDEKORT
        },
        meldeperioder = this.meldeperioder.map { it.toDTO() }.toList(),
        beregningstidspunkt = this.beregningstidspunkt,
        simuleringstidspunkt = this.simuleringstidspunkt,
        simuleringsdato = this.simuleringsdato,
        simuleringTotalBeløp = this.simuleringTotalBeløp,
        simulerteBeløp = this.simuleringsdager?.tilSimulerteBeløpDTO(),
        beregning = this.beregning.tilBeregningerSummertDTO(this.forrigeBeregning),
        simuleringResultat = this.simuleringResultat.tilDTO(),
    )
}

fun SimulertBeregningMeldeperiode.toDTO(): SimulertBeregningDTO.SimulertBeregningMeldeperiode {
    return SimulertBeregningDTO.SimulertBeregningMeldeperiode(
        kjedeId = this.kjedeId.toString(),
        dager = this.dager.map { it.toDTO() }.toList(),
        simulerteBeløp = this.simuleringsdager?.tilSimulerteBeløpDTO(),
        beregning = this.beregning.tilBeregningerSummertDTO(this.forrigeBeregning),
        flagg = this.flagg.tilDTO(),
        posteringer = this.posteringer.map { it.tilDTO() },
    )
}

private fun Simuleringsflagg.tilDTO(): SimulertBeregningDTO.SimuleringsflaggDTO {
    return SimulertBeregningDTO.SimuleringsflaggDTO(
        harJustering = this.harJustering,
        justeringGårOppINull = this.justeringGårOppINull,
        justeringPåTversAvMeldeperiodeEllerMåned = this.justeringPåTversAvMeldeperiodeEllerMåned,
        harFeilutbetaling = this.harFeilutbetaling,
        harTrekk = this.harTrekk,
    )
}

private fun Postering.tilDTO(): SimulertBeregningDTO.SimuleringsposteringDTO {
    return SimulertBeregningDTO.SimuleringsposteringDTO(
        type = this.type.tilDTO(),
        periodeFraOgMed = this.periode.fraOgMed,
        periodeTilOgMed = this.periode.tilOgMed,
        klassekode = this.klassekode,
        beløp = this.beløp,
        erJustering = this.erJustering,
    )
}

private fun Simuleringsmerke.tilDTO(): SimulertBeregningDTO.SimuleringsmerkeDTO {
    return SimulertBeregningDTO.SimuleringsmerkeDTO(
        type = this.type.tilDTO(),
        periodeFraOgMed = this.periode.fraOgMed,
        periodeTilOgMed = this.periode.tilOgMed,
        klassekode = this.klassekode,
        beløp = this.beløp,
        erJustering = this.erJustering,
        erNegativt = this.erNegativt,
    )
}

private fun Posteringstype.tilDTO(): SimulertBeregningDTO.PosteringstypeDTO {
    return when (this) {
        Posteringstype.YTELSE -> SimulertBeregningDTO.PosteringstypeDTO.YTELSE
        Posteringstype.FEILUTBETALING -> SimulertBeregningDTO.PosteringstypeDTO.FEILUTBETALING
        Posteringstype.FORSKUDSSKATT -> SimulertBeregningDTO.PosteringstypeDTO.FORSKUDSSKATT
        Posteringstype.JUSTERING -> SimulertBeregningDTO.PosteringstypeDTO.JUSTERING
        Posteringstype.TREKK -> SimulertBeregningDTO.PosteringstypeDTO.TREKK
        Posteringstype.MOTPOSTERING -> SimulertBeregningDTO.PosteringstypeDTO.MOTPOSTERING
    }
}

fun SimulertBeregningDag.toDTO(): SimulertBeregningDTO.SimulertBeregningMeldeperiode.SimulertBeregningDag {
    return SimulertBeregningDTO.SimulertBeregningMeldeperiode.SimulertBeregningDag(
        dato = this.dato,
        status = this.beregningsdag?.tilMeldekortDagStatusDTO(),
        simulerteBeløp = this.simuleringsdag?.let {
            SimulertBeregningDTO.SimulerteBeløp(
                feilutbetaling = it.totalFeilutbetaling,
                etterbetaling = it.totalEtterbetaling,
                tidligereUtbetaling = it.tidligereUtbetalt,
                nyUtbetaling = it.nyUtbetaling,
                totalJustering = it.totalJustering,
                totalTrekk = it.totalTrekk,
            )
        },
        beregning = this.beregningsdag?.let {
            BeregningerSummertDTO(
                totalt = BeløpFørOgNåDTO(
                    før = this.forrigeBeregningsdag?.totalBeløp,
                    nå = it.totalBeløp,
                ),
                ordinært = BeløpFørOgNåDTO(
                    før = this.forrigeBeregningsdag?.beløp,
                    nå = it.beløp,
                ),
                barnetillegg = BeløpFørOgNåDTO(
                    før = this.forrigeBeregningsdag?.beløpBarnetillegg,
                    nå = it.beløpBarnetillegg,
                ),
            )
        },
        merker = this.merker.map { it.tilDTO() },
    )
}

private fun NonEmptyList<Simuleringsdag>.tilSimulerteBeløpDTO(): SimulertBeregningDTO.SimulerteBeløp {
    return SimulertBeregningDTO.SimulerteBeløp(
        feilutbetaling = this.sumOf { it.totalFeilutbetaling },
        etterbetaling = this.sumOf { it.totalEtterbetaling },
        tidligereUtbetaling = this.sumOf { it.tidligereUtbetalt },
        nyUtbetaling = this.sumOf { it.nyUtbetaling },
        totalJustering = this.sumOf { it.totalJustering },
        totalTrekk = this.sumOf { it.totalTrekk },
    )
}

private fun SimulertBeregning.BeregningBeløp.tilBeregningerSummertDTO(forrigeBeregning: SimulertBeregning.BeregningBeløp?): BeregningerSummertDTO {
    return BeregningerSummertDTO(
        totalt = BeløpFørOgNåDTO(
            før = forrigeBeregning?.total,
            nå = this.total,
        ),
        ordinært = BeløpFørOgNåDTO(
            før = forrigeBeregning?.ordinær,
            nå = this.ordinær,
        ),
        barnetillegg = BeløpFørOgNåDTO(
            før = forrigeBeregning?.barnetillegg,
            nå = this.barnetillegg,
        ),
    )
}

private fun SimulertBeregning.SimuleringResultat.tilDTO(): SimulertBeregningDTO.SimuleringResultatDTO {
    return when (this) {
        SimulertBeregning.SimuleringResultat.ENDRING -> SimulertBeregningDTO.SimuleringResultatDTO.ENDRING
        SimulertBeregning.SimuleringResultat.INGEN_ENDRING -> SimulertBeregningDTO.SimuleringResultatDTO.INGEN_ENDRING
        SimulertBeregning.SimuleringResultat.IKKE_SIMULERT -> SimulertBeregningDTO.SimuleringResultatDTO.IKKE_SIMULERT
    }
}
