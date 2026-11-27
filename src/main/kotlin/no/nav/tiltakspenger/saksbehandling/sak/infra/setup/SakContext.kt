package no.nav.tiltakspenger.saksbehandling.sak.infra.setup

import no.nav.tiltakspenger.libs.common.SaksnummerGenerator
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.infra.skjerming.FellesSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import java.time.Clock

open class SakContext(
    sessionFactory: SessionFactory,
    fellesSkjermingsklient: FellesSkjermingsklient,
    personService: PersonService,
    val saksnummerGenerator: SaksnummerGenerator,
    clock: Clock,
    val oppdragsversjonForNyeSaker: Oppdragsversjon,
) {
    val sakService: SakService by lazy {
        SakService(
            sakRepo = sakRepo,
            fellesSkjermingsklient = fellesSkjermingsklient,
            personService = personService,
            sessionFactory = sessionFactory,
            oppdragsversjonForNyeSaker = oppdragsversjonForNyeSaker,
        )
    }
    open val sakRepo: SakRepo by lazy {
        SakPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            saksnummerGenerator = saksnummerGenerator,
            clock = clock,
        )
    }
}
