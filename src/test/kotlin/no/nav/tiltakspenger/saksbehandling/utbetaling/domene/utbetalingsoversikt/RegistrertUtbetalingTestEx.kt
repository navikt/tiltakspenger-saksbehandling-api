package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.periode.Periode
import java.math.BigDecimal
import java.time.LocalDate

/** Byggere med en vanlig, utbetalt tiltakspengeutbetaling i august 2025 som utgangspunkt. */
fun registrertUtbetaling(
    utbetaltTil: Aktør = Aktør.Person(Fnr.random()),
    utbetalingsmetode: String = "Til konto",
    utbetalingsstatus: String = "Utbetalt",
    posteringsdato: LocalDate = 24.august(2025),
    forfallsdato: LocalDate? = 24.august(2025),
    utbetalingsdato: LocalDate? = 15.august(2025),
    nettobeløp: BigDecimal? = "900.1".toBigDecimal(),
    melding: String? = null,
    ytelser: List<RegistrertYtelse> = listOf(registrertYtelse()),
): RegistrertUtbetaling = RegistrertUtbetaling(
    utbetaltTil = utbetaltTil,
    utbetalingsmetode = utbetalingsmetode,
    utbetalingsstatus = utbetalingsstatus,
    posteringsdato = posteringsdato,
    forfallsdato = forfallsdato,
    utbetalingsdato = utbetalingsdato,
    nettobeløp = nettobeløp,
    melding = melding,
    ytelser = ytelser,
)

fun registrertYtelse(
    ytelsestype: String? = "Tiltakspenger",
    periode: Periode = Periode(1.august(2025), 14.august(2025)),
    nettobeløp: BigDecimal = "900.1".toBigDecimal(),
    rettighetshaver: Aktør = Aktør.Person(Fnr.random()),
    skattesum: BigDecimal = "-99.9".toBigDecimal(),
    trekksum: BigDecimal = "-900.75".toBigDecimal(),
    komponentsum: BigDecimal = "1900.75".toBigDecimal(),
    komponenter: List<Ytelseskomponent> = emptyList(),
    trekk: List<Trekk> = emptyList(),
    skattetrekk: List<Skattetrekk> = emptyList(),
    bilagsnummer: String? = null,
    refundertFor: Aktør? = null,
): RegistrertYtelse = RegistrertYtelse(
    ytelsestype = ytelsestype,
    periode = periode,
    nettobeløp = nettobeløp,
    rettighetshaver = rettighetshaver,
    skattesum = skattesum,
    trekksum = trekksum,
    komponentsum = komponentsum,
    komponenter = komponenter,
    trekk = trekk,
    skattetrekk = skattetrekk,
    bilagsnummer = bilagsnummer,
    refundertFor = refundertFor,
)
