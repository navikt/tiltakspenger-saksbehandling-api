package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeløpDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak

data class UtbetalingstidslinjeMeldeperiodeDTO(
    val kjedeId: String,
    val periode: PeriodeDTO,
    val beløp: BeløpDTO,
    // TODO - denne kan være utbetalingsstatusDTO
    val status: String?,
)

fun Sak.tilUtbetalingstidslinjeMeldeperiodeDTO(): List<UtbetalingstidslinjeMeldeperiodeDTO> {
    return utbetalinger.tidslinje.perioderMedVerdi.flatMap { (utbetaling, periode) ->
        utbetaling.beregning.beregninger
            .filter { it.periode.overlapperMed(periode) }
            .map { meldeperiodeberegning ->
                UtbetalingstidslinjeMeldeperiodeDTO(
                    status = utbetaling.status?.name,
                    kjedeId = meldeperiodeberegning.kjedeId.verdi,
                    periode = meldeperiodeberegning.periode.toDTO(),
                    beløp = BeløpDTO(
                        totalt = meldeperiodeberegning.totalBeløp,
                        ordinært = meldeperiodeberegning.ordinærBeløp,
                        barnetillegg = meldeperiodeberegning.barnetilleggBeløp,
                    ),
                )
            }
    }
}
