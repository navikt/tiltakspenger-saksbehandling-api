package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.setup

import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.TiltaksdeltagelseHttpKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.service.TiltaksdeltakelseService

open class TiltaksdeltagelseContext(
    texasClient: TexasClient,
    sakService: SakService,
    personService: PersonService,

) {
    open val tiltaksdeltagelseKlient: TiltaksdeltagelseKlient by lazy {
        TiltaksdeltagelseHttpKlient(
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
            tiltaksdeltagelseKlient = tiltaksdeltagelseKlient,
        )
    }
}
