package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal
import java.time.LocalDate

data class UtbetalingsoversiktDto(
    val ytelseListe: List<YtelseDto> = emptyList(),
    val utbetaltTil: AktoerDto? = null,
    val utbetalingsmetode: String? = null,
    val utbetalingsstatus: String? = null,
    val posteringsdato: LocalDate? = null,
    val forfallsdato: LocalDate? = null,
    val utbetalingsdato: LocalDate? = null,
    val utbetalingNettobeloep: BigDecimal? = null,
    val utbetalingsmelding: String? = null,
    val utbetaltTilKonto: BankkontoDto? = null,
) {
    data class UtbetalingsperiodeDto(
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class YtelseDto(
        val ytelsestype: String? = null,
        val ytelsesperiode: UtbetalingsperiodeDto,
        val ytelseNettobeloep: BigDecimal? = null,
        val rettighetshaver: AktoerDto? = null,
        val skattsum: BigDecimal? = null,
        val trekksum: BigDecimal? = null,
        val ytelseskomponentersum: BigDecimal? = null,
        val skattListe: List<SkattDto> = emptyList(),
        val trekkListe: List<TrekkDto> = emptyList(),
        val ytelseskomponentListe: List<YtelseskomponentDto> = emptyList(),
        val bilagsnummer: String? = null,
        val refundertForOrg: AktoerDto? = null,
    )

    data class YtelseskomponentDto(
        val ytelseskomponenttype: String? = null,
        val satsbeloep: BigDecimal? = null,
        val satstype: String? = null,
        val satsantall: Double? = null,
        val ytelseskomponentbeloep: BigDecimal? = null,
    )

    data class TrekkDto(
        val trekktype: String? = null,
        val trekkbeloep: BigDecimal? = null,
        val kreditor: String? = null,
    )

    data class SkattDto(
        val skattebeloep: BigDecimal? = null,
    )

    data class AktoerDto(
        val aktoertype: String? = null,
        @JsonAlias("aktoerId")
        val ident: String? = null,
        val navn: String? = null,
    )

    data class BankkontoDto(
        val kontonummer: String? = null,
        val kontotype: String? = null,
    )
}

data class UtbetalingsoversiktRequestDto(
    val ident: String,
    val rolle: String,
    val periode: UtbetalingsoversiktDto.UtbetalingsperiodeDto,
    val periodetype: String,
)
