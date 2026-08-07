package no.nav.tiltakspenger.saksbehandling.benk.v2.domene

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

/**
 * Spørringene bak benk v2.
 * Én metode per fane, fordi hver fane har sitt eget radformat og sitt eget filter.
 */
interface BenkV2Repo {
    companion object {
        const val DEFAULT_LIMIT = 500
    }

    fun hentSøknader(
        command: HentBenkV2Command<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = DEFAULT_LIMIT,
    ): BenkV2Oversikt<BenkSøknadsbehandling>

    fun hentRevurderinger(
        command: HentBenkV2Command<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = DEFAULT_LIMIT,
    ): BenkV2Oversikt<BenkRevurdering>

    fun hentMeldekort(
        command: HentBenkV2Command<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = DEFAULT_LIMIT,
    ): BenkV2Oversikt<BenkMeldekort>

    fun hentKlager(
        command: HentBenkV2Command<BenkKlageFiltrering, BenkKlageKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = DEFAULT_LIMIT,
    ): BenkV2Oversikt<BenkKlagebehandling>

    fun hentTilbakekrevinger(
        command: HentBenkV2Command<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        sessionContext: SessionContext? = null,
        limit: Int = DEFAULT_LIMIT,
    ): BenkV2Oversikt<BenkTilbakekreving>

    fun hentAntallPerFane(sessionContext: SessionContext? = null): BenkV2AntallPerFane
}
