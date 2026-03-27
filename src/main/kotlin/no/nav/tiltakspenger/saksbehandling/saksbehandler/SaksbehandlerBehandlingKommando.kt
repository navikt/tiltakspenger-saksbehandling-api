package no.nav.tiltakspenger.saksbehandling.saksbehandler

/**
 *  Handlinger som en saksbehandler kan utføre på en behandling
 *
 *  Vi har mye logikk for dette i frontend nå for å avgjøre dette, tanken er å flytte det til backend etterhvert
 *  */
enum class SaksbehandlerBehandlingKommando {
    TildelSaksbehandler,
    TildelBeslutter,
    OvertaSaksbehandler,
    OvertaBeslutter,
    LeggTilbakeSaksbehandler,
    LeggTilbakeBeslutter,
    SettPåVent,
    Gjenoppta,
    Avslutt,
    ;

    fun tilDTO(): SaksbehandlerBehandlingKommandoDTO {
        return when (this) {
            TildelSaksbehandler -> SaksbehandlerBehandlingKommandoDTO.TildelSaksbehandler
            TildelBeslutter -> SaksbehandlerBehandlingKommandoDTO.TildelBeslutter
            OvertaSaksbehandler -> SaksbehandlerBehandlingKommandoDTO.OvertaSaksbehandler
            OvertaBeslutter -> SaksbehandlerBehandlingKommandoDTO.OvertaBeslutter
            LeggTilbakeSaksbehandler -> SaksbehandlerBehandlingKommandoDTO.LeggTilbakeSaksbehandler
            LeggTilbakeBeslutter -> SaksbehandlerBehandlingKommandoDTO.LeggTilbakeBeslutter
            SettPåVent -> SaksbehandlerBehandlingKommandoDTO.SettPåVent
            Gjenoppta -> SaksbehandlerBehandlingKommandoDTO.Gjenoppta
            Avslutt -> SaksbehandlerBehandlingKommandoDTO.Avslutt
        }
    }
}

enum class SaksbehandlerBehandlingKommandoDTO {
    TildelSaksbehandler,
    TildelBeslutter,
    OvertaSaksbehandler,
    OvertaBeslutter,
    LeggTilbakeSaksbehandler,
    LeggTilbakeBeslutter,
    SettPåVent,
    Gjenoppta,
    Avslutt,
}

fun List<SaksbehandlerBehandlingKommando>.tilDTO(): List<SaksbehandlerBehandlingKommandoDTO> {
    return this.map { it.tilDTO() }
}
