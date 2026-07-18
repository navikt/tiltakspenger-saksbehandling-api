package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators

/**
 * Postgres-versjon av [TestApplicationContext].
 * Arver alle context- og repo-konfigurasjoner fra basisklassen, og bruker Postgres-bakte repoer via [PostgresSessionFactory].
 *
 * Setter kun opp Postgres-spesifikke ting — fake-klienter og hjelpemetoder kommer fra basisklassen.
 *
 * Klassen er `open` så enkelttester kan lage anonyme subklasser for å overstyre fake-klienter eller injisere fake-repoer via `*RepoOverride`-hookene fra basisklassen.
 */
open class TestApplicationContextMedPostgres(
    override val sessionFactory: PostgresSessionFactory,
    clock: TikkendeKlokke = TikkendeKlokke(fixedClock),
    override val texasClient: TexasClient = TexasClientFake(clock),
    tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
    idGenerators: IdGenerators,
) : TestApplicationContext(clock, idGenerators, tilgangsmaskinFakeClient)
