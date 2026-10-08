package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Hva avgrensningen tok bort fra ett svar, som tall og regel.
 * Ingenting om innholdet som ble tatt bort.
 *
 * @param mottattSvarSha256 Sha256 av svaret slik det ble mottatt, for å kunne sammenligne med kildens logg uten å lagre innholdet.
 * @param antallFjernedeUtbetalinger Utbetalinger som sto igjen uten ytelser og derfor ble tatt bort.
 */
data class Utbetalingsoversiktavgrensning(
    val regelversjon: Int,
    val perioder: List<Periode>,
    val antallUtbetalingerMottatt: Int,
    val antallYtelserMottatt: Int,
    val fjernedeYtelserPerÅrsak: Map<Avgrensningsårsak, Int>,
    val antallFjernedeUtbetalinger: Int,
    val mottattSvarSha256: String,
    val mottattSvarLengde: Int,
)
