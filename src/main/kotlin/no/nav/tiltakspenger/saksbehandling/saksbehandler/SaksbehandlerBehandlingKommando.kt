package no.nav.tiltakspenger.saksbehandling.saksbehandler

/**
 *  Handlinger som en saksbehandler kan utføre på en behandling
 *
 *  Vi har mye logikk for dette i frontend nå for å avgjøre dette, tanken er å flytte det til backend etterhvert
 *  */
enum class SaksbehandlerBehandlingKommando {
    TildelSaksbehandler,
    TildelBeslutter,
    AngreSendTilBeslutning,
    OvertaSaksbehandler,
    OvertaBeslutter,
    LeggTilbakeSaksbehandler,
    LeggTilbakeBeslutter,
    HentTilbake,
    SettPåVent,
    OppdaterSettPåVent,
    Gjenoppta,
    Avbryt,
    Gjenåpne,
    ;

    fun tilDTO(): SaksbehandlerBehandlingKommandoDTO {
        return when (this) {
            TildelSaksbehandler -> SaksbehandlerBehandlingKommandoDTO.TildelSaksbehandler
            TildelBeslutter -> SaksbehandlerBehandlingKommandoDTO.TildelBeslutter
            OvertaSaksbehandler -> SaksbehandlerBehandlingKommandoDTO.OvertaSaksbehandler
            OvertaBeslutter -> SaksbehandlerBehandlingKommandoDTO.OvertaBeslutter
            LeggTilbakeSaksbehandler -> SaksbehandlerBehandlingKommandoDTO.LeggTilbakeSaksbehandler
            LeggTilbakeBeslutter -> SaksbehandlerBehandlingKommandoDTO.LeggTilbakeBeslutter
            HentTilbake -> SaksbehandlerBehandlingKommandoDTO.HentTilbake
            SettPåVent -> SaksbehandlerBehandlingKommandoDTO.SettPåVent
            OppdaterSettPåVent -> SaksbehandlerBehandlingKommandoDTO.OppdatereSettPåVent
            Gjenoppta -> SaksbehandlerBehandlingKommandoDTO.Gjenoppta
            Avbryt -> SaksbehandlerBehandlingKommandoDTO.Avbryt
            Gjenåpne -> SaksbehandlerBehandlingKommandoDTO.Gjenåpne
            AngreSendTilBeslutning -> SaksbehandlerBehandlingKommandoDTO.AngreSendTilBeslutning
        }
    }
}

enum class SaksbehandlerBehandlingKommandoDTO {
    TildelSaksbehandler,
    TildelBeslutter,
    AngreSendTilBeslutning,
    OvertaSaksbehandler,
    OvertaBeslutter,
    LeggTilbakeSaksbehandler,
    LeggTilbakeBeslutter,
    HentTilbake,
    SettPåVent,
    OppdatereSettPåVent,
    Gjenoppta,
    Avbryt,
    Gjenåpne,
}

fun List<SaksbehandlerBehandlingKommando>.tilDTO(): List<SaksbehandlerBehandlingKommandoDTO> {
    return this.map { it.tilDTO() }
}
