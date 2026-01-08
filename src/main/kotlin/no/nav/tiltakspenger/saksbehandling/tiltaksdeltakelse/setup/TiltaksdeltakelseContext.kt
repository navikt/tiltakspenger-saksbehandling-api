package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseHttpKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerPostgresRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service.TiltaksdeltakelseService

open class TiltaksdeltakelseContext(
    texasClient: TexasClient,
    sakService: SakService,
    personService: PersonService,
    sessionFactory: SessionFactory,
) {
    open val tiltaksdeltakerRepo: TiltaksdeltakerRepo by lazy { TiltaksdeltakerPostgresRepo(sessionFactory as PostgresSessionFactory) }

    open val tiltaksdeltakelseKlient: TiltaksdeltakelseKlient by lazy {
        TiltaksdeltakelseHttpKlient(
            baseUrl = Configuration.tiltakUrl,
            getToken = {
                texasClient.getSystemToken(Configuration.tiltakScope, IdentityProvider.AZUREAD)
            },
        )
    }

    open val tiltaksdeltakelseService: TiltaksdeltakelseService by lazy {
        TiltaksdeltakelseService(
            sakService = sakService,
            personService = personService,
            tiltaksdeltakelseKlient = tiltaksdeltakelseKlient,
        )
    }
}
