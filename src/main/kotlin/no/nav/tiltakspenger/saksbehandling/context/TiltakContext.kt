package no.nav.tiltakspenger.saksbehandling.context

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.saksbehandling.Configuration
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.TiltakGateway
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltakGatewayImpl
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseHttpklient

open class TiltakContext(
    entraIdSystemtokenClient: EntraIdSystemtokenClient,
) {
    private val tiltaksdeltagelseklient: TiltaksdeltagelseHttpklient by lazy {
        TiltaksdeltagelseHttpklient(
            baseUrl = Configuration.tiltakUrl,
            getToken = {
                entraIdSystemtokenClient.getSystemtoken(Configuration.tiltakScope)
            },
        )
    }
    open val tiltakGateway: TiltakGateway by lazy { TiltakGatewayImpl(tiltaksdeltagelseklient) }
}
