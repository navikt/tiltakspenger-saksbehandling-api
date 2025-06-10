package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.TiltaksdeltagelseHttpKlient

open class TiltaksdeltagelseContext(
    entraIdSystemtokenClient: EntraIdSystemtokenClient,
) {
    open val tiltaksdeltagelseKlient: TiltaksdeltagelseKlient by lazy {
        TiltaksdeltagelseHttpKlient(
            baseUrl = Configuration.tiltakUrl,
            getToken = {
                entraIdSystemtokenClient.getSystemtoken(Configuration.tiltakScope)
            },
        )
    }
}
