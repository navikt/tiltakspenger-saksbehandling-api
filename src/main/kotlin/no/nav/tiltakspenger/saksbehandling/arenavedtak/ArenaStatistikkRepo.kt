package no.nav.tiltakspenger.saksbehandling.arenavedtak

import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaSisteDagMedRettEtter
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaUtbetalingerForMåned
import java.time.Instant

interface ArenaStatistikkRepo {
    fun sistHentet(): Instant?

    /** Skriver over månedene som finnes fra før. */
    fun lagre(statistikk: List<ArenaStatistikkForMåned>, hentet: Instant)

    fun sisteDagMedRettSistHentet(): Instant?

    /** Erstatter hele fordelingen. */
    fun lagreSisteDagMedRett(fordeling: ArenaSisteDagMedRettEtter, hentet: Instant)

    fun utbetalingerSistHentet(): Instant?

    /** Skriver over månedene som finnes fra før. */
    fun lagreUtbetalinger(statistikk: List<ArenaUtbetalingerForMåned>, hentet: Instant)
}
