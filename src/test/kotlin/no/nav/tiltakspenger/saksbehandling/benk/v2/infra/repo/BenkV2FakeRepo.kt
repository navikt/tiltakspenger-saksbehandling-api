package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2AntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Oversikt
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Repo
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.HentBenkV2Kommando

/**
 * Benk v2 har ingen in-memory-implementasjon, og skal ikke få en.
 *
 * Fanene er tverrgående spørringer over hele skjemaet — søknad, behandling, meldekortbehandling, meldekort_bruker, klagebehandling, tilbakekreving og utbetaling — og en håndskrevet kopi av dem over fake-repoene ville vært et parallelt regelsett som driver fra sql-en uten at noe fanger det opp.
 * Derfor testes benken utelukkende mot postgres, gjennom `BenkV2AggregatTest` og `HentBenkV2RouteTest`.
 *
 * Dette repoet finnes bare for at rutene skal kunne registreres i in-memory-konteksten, siden `BenkV2PostgresRepo` krever en `PostgresSessionFactory`.
 * Det kaster framfor å svare tomt, slik at en test som lener seg på benken må flytte til postgres i stedet for å stille tro til tall som aldri var sanne.
 */
class BenkV2FakeRepo : BenkV2Repo {

    override fun hentSøknader(
        command: HentBenkV2Kommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkSøknadsbehandling> = kreverPostgres()

    override fun hentRevurderinger(
        command: HentBenkV2Kommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkRevurdering> = kreverPostgres()

    override fun hentMeldekort(
        command: HentBenkV2Kommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkMeldekort> = kreverPostgres()

    override fun hentKlager(
        command: HentBenkV2Kommando<BenkKlageFiltrering, BenkKlageKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkKlagebehandling> = kreverPostgres()

    override fun hentTilbakekrevinger(
        command: HentBenkV2Kommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkV2Oversikt<BenkTilbakekreving> = kreverPostgres()

    override fun hentAntallPerFane(sessionContext: SessionContext?): BenkV2AntallPerFane = kreverPostgres()

    private fun kreverPostgres(): Nothing = throw UnsupportedOperationException(
        "Benk v2 har ingen in-memory-implementasjon. Kjør testen isolert mot postgres, slik BenkV2AggregatTest og HentBenkV2RouteTest gjør.",
    )
}
