package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import java.time.Clock

open class StatistikkContext(
    sessionFactory: SessionFactory,
    personKlient: PersonKlient,
    gitHash: String,
    clock: Clock,
) {
    open val statistikkRepo: StatistikkRepo by lazy {
        StatistikkPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }

    val statistikkService: StatistikkService by lazy {
        StatistikkService(
            personKlient = personKlient,
            gitHash = gitHash,
            clock = clock,
            statistikkRepo = statistikkRepo,
        )
    }
}
