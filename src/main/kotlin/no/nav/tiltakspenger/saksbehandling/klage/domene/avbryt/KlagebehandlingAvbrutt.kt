package no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt

import no.nav.tiltakspenger.libs.common.NonBlankString
import java.time.LocalDateTime

class KlagebehandlingAvbrutt(
    val tidspunkt: LocalDateTime,
    val saksbehandler: String,
    val begrunnelse: NonBlankString?,
    val status: AvbruttKlagebehandlingStatus,
) {

    init {
        when (status) {
            AvbruttKlagebehandlingStatus.KLAGE_TRUKKET,
            AvbruttKlagebehandlingStatus.FEILREGISTRER_KLAGE,
            AvbruttKlagebehandlingStatus.MANGLENDE_UTBETALING,
            -> {
                require(begrunnelse == null) {
                    "Begrunnelse må være null når status ikke er ANNET"
                }
            }

            AvbruttKlagebehandlingStatus.ANNET -> {
                require(begrunnelse != null) {
                    "Begrunnelse må være satt når status er ANNET"
                }
            }
        }
    }
}
