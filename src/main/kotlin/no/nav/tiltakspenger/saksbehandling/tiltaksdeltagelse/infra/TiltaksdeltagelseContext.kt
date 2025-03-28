package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.TiltaksdeltagelseGatewayImpl
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.TiltaksdeltagelseHttpklient

open class TiltaksdeltagelseContext(
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
    open val tiltaksdeltagelseGateway: TiltaksdeltagelseGateway by lazy { TiltaksdeltagelseGatewayImpl(tiltaksdeltagelseklient) }
}
