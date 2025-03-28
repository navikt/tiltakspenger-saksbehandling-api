package no.nav.tiltakspenger.saksbehandling.oppgave.infra

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.libs.personklient.tilgangsstyring.TilgangsstyringServiceImpl
import no.nav.tiltakspenger.saksbehandling.Configuration
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.poaotilgang.infra.PoaoTilgangClient
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.oppgave.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.person.PersonGateway
import no.nav.tiltakspenger.saksbehandling.person.infra.MicrosoftGraphApiClient
import no.nav.tiltakspenger.saksbehandling.person.infra.PersonHttpklient
import no.nav.tiltakspenger.saksbehandling.person.infra.repo.PersonPostgresRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.PersonRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.PoaoTilgangGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.person.PersonService

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
