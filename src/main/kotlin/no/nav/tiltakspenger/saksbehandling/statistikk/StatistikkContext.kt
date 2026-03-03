package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SaksstatistikkRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadPostgresRepo
import java.time.Clock

open class StatistikkContext(
    sessionFactory: SessionFactory,
    personKlient: PersonKlient,
    gitHash: String,
    clock: Clock,
) {
    open val saksstatistikkRepo: SaksstatistikkRepo by lazy { SaksstatistikkPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val statistikkStønadRepo: StatistikkStønadRepo by lazy {
        StatistikkStønadPostgresRepo(
            sessionFactory as PostgresSessionFactory,
            clock,
        )
    }
    open val statistikkMeldekortRepo: StatistikkMeldekortRepo by lazy {
        StatistikkMeldekortPostgresRepo(
            sessionFactory as PostgresSessionFactory,
            clock,
        )
    }

    val saksstatistikkService: SaksstatistikkService by lazy {
        SaksstatistikkService(
            personKlient = personKlient,
            gitHash = gitHash,
            clock = clock,
        )
    }
}
