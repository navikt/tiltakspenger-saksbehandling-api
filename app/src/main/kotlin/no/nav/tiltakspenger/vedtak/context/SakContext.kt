package no.nav.tiltakspenger.vedtak.context

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.vedtak.Profile
import no.nav.tiltakspenger.vedtak.repository.benk.BenkOversiktPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.sak.SakPostgresRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.SaksnummerGenerator
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.PoaoTilgangGateway
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.SaksoversiktRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.SakServiceImpl

open class SakContext(
    sessionFactory: SessionFactory,
    tilgangsstyringService: TilgangsstyringService,
    poaoTilgangGateway: PoaoTilgangGateway,
    personService: PersonService,
    profile: Profile,
) {
    val sakService: SakService by lazy {
        SakServiceImpl(
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
        )
    }
    open val saksoversiktRepo: SaksoversiktRepo by lazy {
        BenkOversiktPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
}
