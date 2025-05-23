package no.nav.tiltakspenger.saksbehandling.sak.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.ports.PoaoTilgangGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SaksoversiktRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.benk.infra.repo.BenkOversiktPostgresRepo
import no.nav.tiltakspenger.saksbehandling.infra.setup.Profile
import no.nav.tiltakspenger.saksbehandling.sak.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakPostgresRepo
import java.time.Clock

open class SakContext(
    sessionFactory: SessionFactory,
    tilgangsstyringService: TilgangsstyringService,
    poaoTilgangGateway: PoaoTilgangGateway,
    personService: PersonService,
    profile: Profile,
    clock: Clock,
) {
    val sakService: SakService by lazy {
        SakService(
            sakRepo = sakRepo,
            saksoversiktRepo = saksoversiktRepo,
            tilgangsstyringService = tilgangsstyringService,
            poaoTilgangGateway = poaoTilgangGateway,
            personService = personService,
        )
    }
    open val saksnummerGenerator: SaksnummerGenerator by lazy {
        when (profile) {
            Profile.LOCAL -> SaksnummerGenerator.Local
            Profile.DEV -> SaksnummerGenerator.Dev
            Profile.PROD -> SaksnummerGenerator.Prod
        }
    }
    open val sakRepo: SakRepo by lazy {
        SakPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            saksnummerGenerator = saksnummerGenerator,
            clock = clock,
        )
    }
    open val saksoversiktRepo: SaksoversiktRepo by lazy {
        BenkOversiktPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
}
