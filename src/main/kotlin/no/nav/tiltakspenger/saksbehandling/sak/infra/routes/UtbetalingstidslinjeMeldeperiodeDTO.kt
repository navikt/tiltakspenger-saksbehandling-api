package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeløpDTO

data class UtbetalingstidslinjeMeldeperiodeDTO(
    val kjedeId: String,
    val periode: PeriodeDTO,
    val beløp: BeløpDTO,
)
