package no.nav.tiltakspenger.saksbehandling.auth.infra

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Bruker
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.Systembrukerrolle
import no.nav.tiltakspenger.saksbehandling.felles.autoriserteBrukerroller
import java.time.Clock
import java.time.Instant

open class TexasClientFake(
    private val clock: Clock,
) : TexasClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<String, Bruker<*, *>>())

    override suspend fun introspectToken(
        token: String,
        identityProvider: IdentityProvider,
    ): TexasIntrospectionResponse {
        return godkjentResponse(token)
    }

    override suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: IdentityProvider,
        rewriteAudienceTarget: Boolean,
    ): AccessToken {
        return accessToken()
    }

    override suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: IdentityProvider,
    ): AccessToken {
        return accessToken()
    }

    fun leggTilBruker(token: String, bruker: Bruker<*, *>) {
        data.get()[token] = bruker
    }

    private fun accessToken(): AccessToken = AccessToken(
        token = "asdf",
        expiresAt = Instant.now(clock).plusSeconds(3600),
        invaliderCache = { },
    )

    private fun godkjentResponse(token: String): TexasIntrospectionResponse {
        val bruker = data.get()[token]

        return when (bruker) {
            is Saksbehandler -> TexasIntrospectionResponse(
                active = true,
                error = null,
                groups = getGroups(bruker),
                roles = null,
                other = mutableMapOf(
                    "azp_name" to bruker.klientnavn,
                    "azp" to bruker.klientId,
                    "NAVident" to bruker.navIdent,
                    "preferred_username" to bruker.epost,
                ),
            )

            is Systembruker -> TexasIntrospectionResponse(
                active = true,
                error = null,
                groups = null,
                roles = getRoles(bruker),
                other = mutableMapOf(
                    "azp_name" to bruker.klientnavn,
                    "azp" to bruker.klientId,
                    "idtyp" to "app",
                ),
            )

            else -> TexasIntrospectionResponse(
                active = false,
                error = null,
                groups = null,
                roles = null,
                other = emptyMap(),
            )
        }
    }

    private fun getGroups(saksbehandler: Saksbehandler): List<String> {
        val alleRoller = autoriserteBrukerroller()
        return saksbehandler.roller.mapNotNull { rolle ->
            alleRoller.firstOrNull { it.name == rolle }?.objectId
        }
    }

    private fun getRoles(systembruker: Systembruker): List<String> {
        return systembruker.roller.value.map { rolle ->
            when (rolle) {
                Systembrukerrolle.HENT_ELLER_OPPRETT_SAK -> "hent_eller_opprett_sak"
                Systembrukerrolle.LAGRE_MELDEKORT -> "lagre_meldekort"
                Systembrukerrolle.LAGRE_SOKNAD -> "lagre_soknad"
            }
        }
    }

    companion object {
        const val LOKAL_FRONTEND_TOKEN_BRUKER_1 = "TokenMcTokenface"
        const val LOKAL_FRONTEND_TOKEN_BRUKER_2 = "TokenMcTokenface2"
        const val LOKAL_SYSTEMBRUKER_TOKEN = "asdf"
    }
}
