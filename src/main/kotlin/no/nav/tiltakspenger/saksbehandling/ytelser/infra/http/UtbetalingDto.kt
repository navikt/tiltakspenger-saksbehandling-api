package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import java.time.LocalDate

data class UtbetalingDto(
    val ytelseListe: List<YtelseDto>,
) {
    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class YtelseDto(
        val ytelsestype: String?,
        val ytelsesperiode: Periode,
    ) {
        fun tilYtelse() =
            Ytelse(
                ytelsetype = ytelsestype?.let { Ytelsetype.valueOf(it) } ?: Ytelsetype.UKJENT,
                periode = Periode(
                    fraOgMed = ytelsesperiode.fom,
                    tilOgMed = ytelsesperiode.tom,
                ),
            )
    }
}
