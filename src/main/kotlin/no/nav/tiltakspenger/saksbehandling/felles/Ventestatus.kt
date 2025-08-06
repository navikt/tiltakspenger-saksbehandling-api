package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import java.time.LocalDateTime

data class Ventestatus(
    val ventestatusHendelser: List<VentestatusHendelse> = emptyList(),
) {
    val erSattPåVent: Boolean = ventestatusHendelser.lastOrNull()?.erSattPåVent ?: false

    init {
        require(
            ventestatusHendelser.zipWithNext { a, b -> a.tidspunkt <= b.tidspunkt }.all { it },
        ) { "Hendelsene må være sortert etter tidspunkt" }
    }

    fun leggTil(
        tidspunkt: LocalDateTime,
        endretAv: String,
        begrunnelse: String = "",
        erSattPåVent: Boolean,
        status: Behandlingsstatus,
    ): Ventestatus {
        return copy(
            ventestatusHendelser = ventestatusHendelser + VentestatusHendelse(
                tidspunkt = tidspunkt,
                endretAv = endretAv,
                begrunnelse = begrunnelse,
                erSattPåVent = erSattPåVent,
                status = status,
            ),
        )
    }
}
