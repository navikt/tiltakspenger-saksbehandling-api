package no.nav.tiltakspenger.saksbehandling.arenavedtak

import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import java.time.Instant

interface ArenaStatistikkRepo {
    fun sistHentet(): Instant?

    /** Skriver over månedene som finnes fra før. */
    fun lagre(statistikk: List<ArenaStatistikkForMåned>, hentet: Instant)
}
