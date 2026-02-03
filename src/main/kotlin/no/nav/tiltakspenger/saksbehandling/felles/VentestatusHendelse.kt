package no.nav.tiltakspenger.saksbehandling.felles

import java.time.LocalDateTime

/**
 * @param status Statusen når hendelsen ble satt på vent eller gjenopptatt. Brukes kun for historikk og debugformål. Brukes på tvers av behandlingstyper. Vi har ikke behov for å standardisere statusverdiene.
 */
data class VentestatusHendelse(
    val tidspunkt: LocalDateTime,
    val endretAv: String,
    val begrunnelse: String,
    val erSattPåVent: Boolean,
    val status: String,
)
