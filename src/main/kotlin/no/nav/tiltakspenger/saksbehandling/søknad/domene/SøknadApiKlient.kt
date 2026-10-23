package no.nav.tiltakspenger.saksbehandling.søknad.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError

/** Klient mot tiltakspenger-soknad-api, som tar imot søknadene fra bruker. */
interface SøknadApiKlient {
    suspend fun sendSøknadsutfall(søknadsutfall: Søknadsutfall): Either<KunneIkkeSendeSøknadsutfall, Unit>
}

sealed interface KunneIkkeSendeSøknadsutfall {
    val httpKlientError: HttpKlientError

    /**
     * Søknad-api kjenner ikke søknaden.
     * Skjer i dev, der saksbehandling-api har digitale søknader som ikke ble sendt inn via søknad-api.
     */
    data class SøknadFinnesIkke(
        override val httpKlientError: HttpKlientError.UventetStatus,
    ) : KunneIkkeSendeSøknadsutfall

    data class Teknisk(
        override val httpKlientError: HttpKlientError,
    ) : KunneIkkeSendeSøknadsutfall
}
