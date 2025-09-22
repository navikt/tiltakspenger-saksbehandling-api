package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import java.time.LocalDateTime

data class VentestatusHendelse(
    val tidspunkt: LocalDateTime,
    val endretAv: String,
    val begrunnelse: String,
    val erSattPåVent: Boolean,
    val status: Rammebehandlingsstatus,
)
