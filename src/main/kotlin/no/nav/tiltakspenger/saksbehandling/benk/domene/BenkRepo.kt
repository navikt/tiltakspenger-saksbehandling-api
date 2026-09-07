package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

/**
 * Spørringene bak benk v2.
 * Én metode per fane, fordi hver fane har sitt eget radformat og sitt eget filter.
 */
interface BenkRepo {

    fun hentSøknader(
        command: HentBenkKommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = BenkPaginering.SIDEANTALL,
        offset: Int = 0,
    ): BenkOversikt<BenkSøknadsbehandling>

    fun hentRevurderinger(
        command: HentBenkKommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = BenkPaginering.SIDEANTALL,
        offset: Int = 0,
    ): BenkOversikt<BenkRevurdering>

    fun hentMeldekort(
        command: HentBenkKommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = BenkPaginering.SIDEANTALL,
        offset: Int = 0,
    ): BenkOversikt<BenkMeldekort>

    fun hentKlager(
        command: HentBenkKommando<BenkKlageFiltrering, BenkKlageKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = BenkPaginering.SIDEANTALL,
        offset: Int = 0,
    ): BenkOversikt<BenkKlagebehandling>

    fun hentTilbakekrevinger(
        command: HentBenkKommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = BenkPaginering.SIDEANTALL,
        offset: Int = 0,
    ): BenkOversikt<BenkTilbakekreving>

    fun hentAntallPerFane(sessionContext: SessionContext? = null): BenkAntallPerFane
}
