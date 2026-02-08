package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import java.time.LocalDateTime

data class Ventestatus(
    val ventestatusHendelser: List<VentestatusHendelse> = emptyList(),
) {
    val erSattPåVent: Boolean = ventestatusHendelser.lastOrNull()?.erSattPåVent ?: false

    init {
        require(
            ventestatusHendelser.zipWithNext { a, b -> a.tidspunkt < b.tidspunkt }.all { it },
        ) { "Hendelsene må være sortert etter tidspunkt" }
        require(
            ventestatusHendelser.withIndex().all { (index, hendelse) ->
                hendelse.erSattPåVent == (index % 2 == 0)
            },
        ) { "erSattPåVent må alternere, og første hendelse må være sattPåVent=true" }
    }

    fun settPåVent(
        tidspunkt: LocalDateTime,
        endretAv: String,
        begrunnelse: String,
        status: String,
    ): Ventestatus {
        return copy(
            ventestatusHendelser = ventestatusHendelser + VentestatusHendelse(
                tidspunkt = tidspunkt,
                endretAv = endretAv,
                begrunnelse = begrunnelse,
                erSattPåVent = true,
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
                status = status,
            ),
        )
    }
}
