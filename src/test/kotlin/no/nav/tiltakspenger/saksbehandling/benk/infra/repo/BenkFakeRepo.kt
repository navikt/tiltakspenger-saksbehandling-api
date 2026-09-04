package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkAntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRepo
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentBenkKommando

/**
 * Benk v2 har ingen in-memory-implementasjon, og skal ikke få en.
 *
 * Fanene er tverrgående spørringer over hele skjemaet — søknad, behandling, meldekortbehandling, meldekort_bruker, klagebehandling, tilbakekreving og utbetaling — og en håndskrevet kopi av dem over fake-repoene ville vært et parallelt regelsett som driver fra sql-en uten at noe fanger det opp.
 * Derfor testes benken utelukkende mot postgres, gjennom `BenkAggregatTest` og `HentBenkRouteTest`.
 *
 * Dette repoet finnes bare for at rutene skal kunne registreres i in-memory-konteksten, siden `BenkPostgresRepo` krever en `PostgresSessionFactory`.
 * Det kaster framfor å svare tomt, slik at en test som lener seg på benken må flytte til postgres i stedet for å stille tro til tall som aldri var sanne.
 */
class BenkFakeRepo : BenkRepo {

    override fun hentSøknader(
        command: HentBenkKommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
        offset: Int,
    ): BenkOversikt<BenkSøknadsbehandling> = kreverPostgres()

    override fun hentRevurderinger(
        command: HentBenkKommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
        offset: Int,
    ): BenkOversikt<BenkRevurdering> = kreverPostgres()

    override fun hentMeldekort(
        command: HentBenkKommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
        offset: Int,
    ): BenkOversikt<BenkMeldekort> = kreverPostgres()

    override fun hentKlager(
        command: HentBenkKommando<BenkKlageFiltrering, BenkKlageKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
        offset: Int,
    ): BenkOversikt<BenkKlagebehandling> = kreverPostgres()

    override fun hentTilbakekrevinger(
        command: HentBenkKommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        sessionContext: SessionContext?,
        limit: Int,
        offset: Int,
    ): BenkOversikt<BenkTilbakekreving> = kreverPostgres()

    override fun hentAntallPerFane(sessionContext: SessionContext?): BenkAntallPerFane = kreverPostgres()

    private fun kreverPostgres(): Nothing = throw UnsupportedOperationException(
        "Benk v2 har ingen in-memory-implementasjon. Kjør testen isolert mot postgres, slik BenkAggregatTest og HentBenkRouteTest gjør.",
    )
}
