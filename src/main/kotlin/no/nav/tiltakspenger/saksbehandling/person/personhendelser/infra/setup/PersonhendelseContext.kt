package no.nav.tiltakspenger.saksbehandling.person.personhendelser.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseRepo
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseService
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.infra.repo.PersonhendelsePostgresRepo
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.jobb.PersonhendelseJobb
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

open class PersonhendelseContext(
    sessionFactory: SessionFactory,
    sakRepo: SakRepo,
    personKlient: PersonKlient,
    statistikkService: StatistikkService,
    oppgaveKlient: OppgaveKlient,
    clock: Clock,
) {
    open val personhendelseRepo: PersonhendelseRepo by lazy {
        PersonhendelsePostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }

    val personhendelseService: PersonhendelseService by lazy {
        PersonhendelseService(
            sakRepo = sakRepo,
            personhendelseRepo = personhendelseRepo,
            personKlient = personKlient,
            statistikkService = statistikkService,
        )
    }

    val personhendelseJobb by lazy {
        PersonhendelseJobb(
            personhendelseRepo = personhendelseRepo,
            sakRepo = sakRepo,
            oppgaveKlient = oppgaveKlient,
            clock = clock,
        )
    }
}
