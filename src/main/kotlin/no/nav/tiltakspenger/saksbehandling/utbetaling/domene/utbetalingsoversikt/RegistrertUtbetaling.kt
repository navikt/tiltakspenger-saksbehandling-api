package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Én utbetaling slik den står i økonomisystemets reskontro.
 * Den kan dekke flere ytelser og har ingen referanse til utbetalingen vi sendte.
 *
 * @param utbetaltTil Den som fikk pengene, som kan være en annen enn rettighetshaveren på ytelsen.
 * @param utbetalingsstatus Fri tekst fra kilden uten kodeverk.
 * Om pengene er utbetalt, avgjøres av [utbetalingsdato].
 * @param posteringsdato Når utbetalingen ble bokført, ikke perioden den gjelder.
 * @param utbetalingsdato Datoen pengene ble utbetalt; mangler til det har skjedd.
 * @param nettobeløp Gjelder hele utbetalingen.
 * Beløpet per ytelse står på [RegistrertYtelse.nettobeløp].
 */
data class RegistrertUtbetaling(
    val utbetaltTil: Aktør,
    val utbetalingsmetode: String,
    val utbetalingsstatus: String,
    val posteringsdato: LocalDate,
    val forfallsdato: LocalDate?,
    val utbetalingsdato: LocalDate?,
    val nettobeløp: BigDecimal?,
    val melding: String?,
    val ytelser: List<RegistrertYtelse>,
)
