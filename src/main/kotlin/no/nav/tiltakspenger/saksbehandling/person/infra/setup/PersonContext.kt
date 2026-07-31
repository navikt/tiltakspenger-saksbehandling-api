package no.nav.tiltakspenger.saksbehandling.person.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesHttpSkjermingsklient
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesSkjermingsklient
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasSystemTokenProvider
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.behandling.ports.PersonRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonHttpklient
import no.nav.tiltakspenger.saksbehandling.person.infra.repo.PersonPostgresRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.infra.MicrosoftGraphApiClient
import java.time.Clock

@Suppress("unused")
open class PersonContext(
    sessionFactory: SessionFactory,
    texasClient: TexasClient,
    clock: Clock,
) {
    /**
     * Denne konteksten er NAIS-oppsettet, der Graph nås over https.
     * Lokal kjøring overstyrer dette i sin egen kontekstklasse i stedet for at vi legger en miljø-if her.
     */
    protected open val brukHttpsMotGraph: Boolean get() = true

    open val personKlient: PersonKlient by lazy {
        PersonHttpklient(
            endepunkt = Configuration.pdlUrl,
            clock = clock,
            getToken = { texasClient.getSystemToken(Configuration.pdlScope, IdentityProvider.AZUREAD) },
        )
    }
    open val fellesSkjermingsklient: FellesSkjermingsklient by lazy {
        FellesHttpSkjermingsklient(
            endepunkt = Configuration.skjermingUrl,
            getToken = { texasClient.getSystemToken(Configuration.skjermingScope, IdentityProvider.AZUREAD) },
            clock = clock,
        )
    }
    open val navIdentClient: NavIdentClient by lazy {
        MicrosoftGraphApiClient(
            baseUrl = Configuration.microsoftUrl,
            brukHttps = brukHttpsMotGraph,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.microsoftScope,
            ),
            clock = clock,
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
            personClient = personKlient,
        )
    }
    val auditService by lazy {
        AuditService(
            personService,
        )
    }
}
