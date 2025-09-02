package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import java.time.LocalDate

data class UtbetalingDto(
    val ytelseListe: List<YtelseDto>,
) {
    data class UtbetalingsperiodeDto(
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class YtelseDto(
        val ytelsestype: String?,
        val ytelsesperiode: UtbetalingsperiodeDto,
    )
}
