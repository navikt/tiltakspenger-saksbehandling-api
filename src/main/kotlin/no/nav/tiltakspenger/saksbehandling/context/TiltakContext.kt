package no.nav.tiltakspenger.saksbehandling.context

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.saksbehandling.Configuration
import no.nav.tiltakspenger.saksbehandling.clients.tiltak.TiltakClientImpl
import no.nav.tiltakspenger.saksbehandling.clients.tiltak.TiltakGatewayImpl
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.TiltakGateway

open class TiltakContext(
    entraIdSystemtokenClient: EntraIdSystemtokenClient,
) {
    private val tiltakClient by lazy {
        TiltakClientImpl(
            baseUrl = Configuration.tiltakUrl,
            getToken = {
                entraIdSystemtokenClient.getSystemtoken(Configuration.tiltakScope)
            },
        )
    }
    open val tiltakGateway: TiltakGateway by lazy { TiltakGatewayImpl(tiltakClient) }
}
