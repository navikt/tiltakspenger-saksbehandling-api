package no.nav.tiltakspenger.vedtak.context

import no.nav.tiltakspenger.felles.NavIdentClient
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.libs.personklient.tilgangsstyring.TilgangsstyringServiceImpl
import no.nav.tiltakspenger.saksbehandling.ports.PersonGateway
import no.nav.tiltakspenger.saksbehandling.ports.PersonRepo
import no.nav.tiltakspenger.saksbehandling.ports.PoaoTilgangGateway
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.auditlog.AuditService
import no.nav.tiltakspenger.vedtak.clients.person.MicrosoftGraphApiClient
import no.nav.tiltakspenger.vedtak.clients.person.PersonHttpklient
import no.nav.tiltakspenger.vedtak.clients.poaotilgang.PoaoTilgangClient
import no.nav.tiltakspenger.vedtak.repository.person.PersonPostgresRepo

@Suppress("unused")
open class PersonContext(
    sessionFactory: SessionFactory,
    entraIdSystemtokenClient: EntraIdSystemtokenClient,
) {
    open val personGateway: PersonGateway by lazy {
        PersonHttpklient(
            endepunkt = Configuration.pdlUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.pdlScope) },
        )
    }
    open val tilgangsstyringService: TilgangsstyringService by lazy {
        TilgangsstyringServiceImpl.create(
            getPdlPipToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.pdlPipScope) },
            pdlPipBaseUrl = Configuration.pdlPipUrl,
            skjermingBaseUrl = Configuration.skjermingUrl,
            getSkjermingToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.skjermingScope) },
            sikkerlogg = sikkerlogg,
        )
    }
    open val navIdentClient: NavIdentClient by lazy {
        MicrosoftGraphApiClient(
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.microsoftScope) },
            baseUrl = Configuration.microsoftUrl,
        )
    }
    open val poaoTilgangGateway: PoaoTilgangGateway by lazy {
        PoaoTilgangClient(
            baseUrl = Configuration.poaoTilgangUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.poaoTilgangScope) },
        )
    }
    open val personRepo: PersonRepo by lazy {
        PersonPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    val personService by lazy {
        PersonService(
            personRepo = personRepo,
            personClient = personGateway,
        )
    }
    val auditService by lazy {
        AuditService(
            personService,
        )
    }
}
