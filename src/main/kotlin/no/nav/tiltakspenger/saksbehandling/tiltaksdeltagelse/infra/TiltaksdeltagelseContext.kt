package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.TiltaksdeltagelseHttpKlient
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
    open val tiltaksdeltagelseKlient: TiltaksdeltagelseKlient by lazy { TiltaksdeltagelseHttpKlient(tiltaksdeltagelseklient) }
}
