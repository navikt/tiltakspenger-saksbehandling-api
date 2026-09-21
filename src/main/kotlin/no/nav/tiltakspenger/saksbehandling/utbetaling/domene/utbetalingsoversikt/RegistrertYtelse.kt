package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.periode.Periode
import java.math.BigDecimal

/**
 * Én ytelse i en registrert utbetaling.
 * Utbetalingen kan også dekke ytelser som ikke er våre.
 *
 * @param ytelsestype Fri tekst fra kilden uten kodeverk, f.eks. «Tiltakspenger».
 * @param periode Perioden ytelsen gjelder.
 * Når den ble bokført, står på [RegistrertUtbetaling.posteringsdato].
 * @param skattesum Negativ fra kilden.
 * @param trekksum Negativ fra kilden.
 * @param bilagsnummer Identifikator fra kilden; ikke dokumentert unik eller stabil.
 * @param refundertFor Organisasjonen det er refundert for når mottakeren er en samlemottaker.
 */
data class RegistrertYtelse(
    val ytelsestype: String?,
    val periode: Periode,
    val nettobeløp: BigDecimal,
    val rettighetshaver: Aktør,
    val skattesum: BigDecimal,
    val trekksum: BigDecimal,
    val komponentsum: BigDecimal,
    val komponenter: List<Ytelseskomponent>,
    val trekk: List<Trekk>,
    val skattetrekk: List<Skattetrekk>,
    val bilagsnummer: String?,
    val refundertFor: Aktør?,
)
