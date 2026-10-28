package no.nav.tiltakspenger.saksbehandling.saksbehandler

/**
 *  Handlinger som en saksbehandler kan utføre på en behandling
 *
 *  Vi har mye logikk for dette i frontend nå for å avgjøre dette, tanken er å flytte det til backend etterhvert
 *  */
enum class SaksbehandlerBehandlingKommando {
    /** Saksbehandleren er tildelt behandlingen, og kan behandle den (behandlingen er under behandling og ikke satt på vent). */
    Saksbehandle,

    /** Beslutteren er tildelt behandlingen, og kan beslutte den (behandlingen er under beslutning og ikke satt på vent). */
    Beslutte,
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
            Saksbehandle -> SaksbehandlerBehandlingKommandoDTO.Saksbehandle
            Beslutte -> SaksbehandlerBehandlingKommandoDTO.Beslutte
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
    Saksbehandle,
    Beslutte,
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
