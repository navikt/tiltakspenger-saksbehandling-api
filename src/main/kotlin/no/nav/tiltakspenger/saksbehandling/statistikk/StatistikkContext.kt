package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.statistikk.jobb.OpprettStatistikkJobb
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadPostgresRepo
import java.time.Clock

open class StatistikkContext(
    sessionFactory: SessionFactory,
    personKlient: PersonKlient,
    gitHash: String,
    clock: Clock,
) {
    open val statistikkSakRepo: StatistikkSakRepo by lazy { StatistikkSakPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val statistikkStønadRepo: StatistikkStønadRepo by lazy { StatistikkStønadPostgresRepo(sessionFactory as PostgresSessionFactory, clock) }
    open val statistikkMeldekortRepo: StatistikkMeldekortRepo by lazy { StatistikkMeldekortPostgresRepo(sessionFactory as PostgresSessionFactory) }

    val statistikkSakService: StatistikkSakService by lazy {
        StatistikkSakService(
            personKlient = personKlient,
            gitHash = gitHash,
            clock = clock,
        )
    }

    val opprettStatistikkJobb: OpprettStatistikkJobb by lazy {
        OpprettStatistikkJobb(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
            statistikkMeldekortRepo = statistikkMeldekortRepo,
        )
    }
}
