package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser
import java.time.LocalDateTime

/** Historikken over avbrytelser og gjenopprettinger av søknaden, i kronologisk rekkefølge. */
data class SøknadshendelseDTO(
    val type: Type,
    val tidspunkt: LocalDateTime,
    val utførtAv: String,
    val begrunnelse: SladdbarVerdi<String>?,
) {
    enum class Type {
        AVBRUTT,
        GJENÅPNET,
    }
}

fun Søknadshendelser.toSøknadshendelserDTO(): List<SøknadshendelseDTO> = this.toList().map {
    SøknadshendelseDTO(
        type = when (it) {
            is no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse.Avbrutt -> SøknadshendelseDTO.Type.AVBRUTT
            is no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse.Gjenåpnet -> SøknadshendelseDTO.Type.GJENÅPNET
        },
        tidspunkt = it.tidspunkt,
        utførtAv = it.utførtAv,
        begrunnelse = it.begrunnelse?.value?.ikkeSladdet(),
    )
}
