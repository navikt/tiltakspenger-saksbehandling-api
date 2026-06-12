package no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

data class AvbrytKlagebehandlingKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val status: AvbruttKlagebehandlingStatus,
    val begrunnelse: NonBlankString?,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
) {
    init {
        when (status) {
            AvbruttKlagebehandlingStatus.ANNET -> require(begrunnelse != null) {
                "Begrunnelse må være satt når status er ANNET"
            }

            AvbruttKlagebehandlingStatus.KLAGE_TRUKKET,
            AvbruttKlagebehandlingStatus.FEILREGISTRER_KLAGE,
            AvbruttKlagebehandlingStatus.MANGLENDE_UTBETALING,
            -> require(begrunnelse == null) {
                "Begrunnelse må være null når status ikke er ANNET"
            }
        }
    }
}
