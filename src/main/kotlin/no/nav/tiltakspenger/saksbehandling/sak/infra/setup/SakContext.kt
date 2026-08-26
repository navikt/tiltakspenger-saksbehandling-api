package no.nav.tiltakspenger.saksbehandling.sak.infra.setup

import no.nav.tiltakspenger.libs.common.SaksnummerGenerator
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentProfile
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakPostgresRepo
import java.time.Clock

open class SakContext(
    sessionFactory: SessionFactory,
    fellesSkjermingsklient: FellesSkjermingsklient,
    personService: PersonService,
    environmentProfile: EnvironmentProfile,
    clock: Clock,
) {
    val sakService: SakService by lazy {
        SakService(
            sakRepo = sakRepo,
            fellesSkjermingsklient = fellesSkjermingsklient,
            personService = personService,
            sessionFactory = sessionFactory,
        )
    }
    open val saksnummerGenerator: SaksnummerGenerator by lazy {
        when (environmentProfile) {
            EnvironmentProfile.LOCAL -> SaksnummerGenerator.Local
            EnvironmentProfile.DEV -> SaksnummerGenerator.Dev
            EnvironmentProfile.PROD -> SaksnummerGenerator.Prod
        }
    }
    open val sakRepo: SakRepo by lazy {
        SakPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            saksnummerGenerator = saksnummerGenerator,
            clock = clock,
        )
    }
}
