package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra

import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.TiltaksdeltagelseHttpKlient

open class TiltaksdeltagelseContext(
    texasClient: TexasClient,
) {
    open val tiltaksdeltagelseKlient: TiltaksdeltagelseKlient by lazy {
        TiltaksdeltagelseHttpKlient(
            baseUrl = Configuration.tiltakUrl,
            getToken = {
                texasClient.getSystemToken(Configuration.tiltakScope, IdentityProvider.AZUREAD)
            },
        )
    }
}
