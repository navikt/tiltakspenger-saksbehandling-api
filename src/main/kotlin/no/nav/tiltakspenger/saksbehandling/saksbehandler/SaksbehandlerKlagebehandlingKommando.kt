package no.nav.tiltakspenger.saksbehandling.saksbehandler

/**
 * Handlinger som en saksbehandler kan utføre på en klagebehandling.
 *
 * Klagebehandlingen har ingen beslutter, så den har et eget, mindre sett med kommandoer enn [SaksbehandlerBehandlingKommando].
 * Dersom klagen har en aktiv tilknyttet ramme- eller meldekortbehandling, utledes kommandoene fra den tilknyttede behandlingen.
 */
enum class SaksbehandlerKlagebehandlingKommando {
    /** Saksbehandleren er tildelt klagebehandlingen, og kan behandle den videre (den er ikke satt på vent). */
    Saksbehandle,
    Tildel,
    Overta,
    LeggTilbake,
    SettPåVent,
    OppdaterSettPåVent,
    Gjenoppta,
    Avbryt,
    ;

    fun tilDTO(): SaksbehandlerKlagebehandlingKommandoDTO {
        return when (this) {
            Saksbehandle -> SaksbehandlerKlagebehandlingKommandoDTO.Saksbehandle
            Tildel -> SaksbehandlerKlagebehandlingKommandoDTO.Tildel
            Overta -> SaksbehandlerKlagebehandlingKommandoDTO.Overta
            LeggTilbake -> SaksbehandlerKlagebehandlingKommandoDTO.LeggTilbake
            SettPåVent -> SaksbehandlerKlagebehandlingKommandoDTO.SettPåVent
            OppdaterSettPåVent -> SaksbehandlerKlagebehandlingKommandoDTO.OppdatereSettPåVent
            Gjenoppta -> SaksbehandlerKlagebehandlingKommandoDTO.Gjenoppta
            Avbryt -> SaksbehandlerKlagebehandlingKommandoDTO.Avbryt
        }
    }
}

enum class SaksbehandlerKlagebehandlingKommandoDTO {
    Saksbehandle,
    Tildel,
    Overta,
    LeggTilbake,
    SettPåVent,
    OppdatereSettPåVent,
    Gjenoppta,
    Avbryt,
}

@JvmName("klagebehandlingKommandoerTilDTO")
fun List<SaksbehandlerKlagebehandlingKommando>.tilDTO(): List<SaksbehandlerKlagebehandlingKommandoDTO> {
    return this.map { it.tilDTO() }
}
