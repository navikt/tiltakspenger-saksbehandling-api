package no.nav.tiltakspenger.saksbehandling.felles

import java.time.LocalDate
import java.time.LocalDateTime

data class Ventestatus(
    val ventestatusHendelser: List<VentestatusHendelse> = emptyList(),
) {
    val erSattPåVent: Boolean = ventestatusHendelser.lastOrNull()?.erSattPåVent ?: false
    val sattPåVentBegrunnelse = ventestatusHendelser.lastOrNull()?.begrunnelse
    val sattPåVentFrist = ventestatusHendelser.lastOrNull()?.frist

    init {
        require(
            ventestatusHendelser.zipWithNext { a, b -> a.tidspunkt < b.tidspunkt }.all { it },
        ) { "Hendelsene må være sortert etter tidspunkt" }
        require(
            ventestatusHendelser.zipWithNext().all { (a, b) -> !(!a.erSattPåVent && !b.erSattPåVent) },
        ) { "To hendelser på rad kan ikke være gjenopptatt" }
        ventestatusHendelser.firstOrNull()?.also { require(it.erSattPåVent) { "Første ventestatus må være satt på vent" } }
    }

    fun settPåVent(
        tidspunktSattPåVent: LocalDateTime,
        endretAv: String,
        begrunnelse: String,
        status: String,
        frist: LocalDate?,
    ): Ventestatus {
        if (frist != null) {
            require(frist >= tidspunktSattPåVent.toLocalDate()) { "Frist for å være på vent kan ikke være i fortiden" }
        }

        return copy(
            ventestatusHendelser = ventestatusHendelser + VentestatusHendelse(
                tidspunkt = tidspunktSattPåVent,
                endretAv = endretAv,
                begrunnelse = begrunnelse,
                erSattPåVent = true,
                frist = frist,
                status = status,
            ),
        )
    }

    fun gjenoppta(
        tidspunkt: LocalDateTime,
        endretAv: String,
        status: String,
    ): Ventestatus {
        return copy(
            ventestatusHendelser = ventestatusHendelser + VentestatusHendelse(
                tidspunkt = tidspunkt,
                endretAv = endretAv,
                begrunnelse = "",
                erSattPåVent = false,
                frist = null,
                status = status,
            ),
        )
    }
}
