package no.nav.tiltakspenger.vedtak.context

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.domene.sak.SaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.ports.PoaoTilgangGateway
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.ports.SaksoversiktRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.ports.TiltakGateway
import no.nav.tiltakspenger.saksbehandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakServiceImpl
import no.nav.tiltakspenger.vedtak.Profile
import no.nav.tiltakspenger.vedtak.repository.benk.BenkOversiktPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.sak.SakPostgresRepo

open class SakContext(
    sessionFactory: SessionFactory,
    søknadService: SøknadService,
    statistikkSakRepo: StatistikkSakRepo,
    tiltakGateway: TiltakGateway,
    tilgangsstyringService: TilgangsstyringService,
    poaoTilgangGateway: PoaoTilgangGateway,
    personService: PersonService,
    gitHash: String,
    profile: Profile,
) {
    val sakService: SakService by lazy {
        SakServiceImpl(
            sakRepo = sakRepo,
            søknadService = søknadService,
            tiltakGateway = tiltakGateway,
            sessionFactory = sessionFactory,
            statistikkSakRepo = statistikkSakRepo,
            saksoversiktRepo = saksoversiktRepo,
            tilgangsstyringService = tilgangsstyringService,
            poaoTilgangGateway = poaoTilgangGateway,
            personService = personService,
            gitHash = gitHash,
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
        )
    }
    open val saksoversiktRepo: SaksoversiktRepo by lazy {
        BenkOversiktPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
}
