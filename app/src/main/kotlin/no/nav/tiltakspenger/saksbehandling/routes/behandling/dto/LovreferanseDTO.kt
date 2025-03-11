package no.nav.tiltakspenger.saksbehandling.routes.behandling.dto

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vilkår.Lovreferanse

data class LovreferanseDTO(
    val lovverk: String,
    val paragraf: String,
    val beskrivelse: String,
)

internal fun Lovreferanse.toDTO() =
    LovreferanseDTO(
        lovverk = this.lovverk,
        paragraf = this.paragraf,
        beskrivelse = this.beskrivelse,
    )
