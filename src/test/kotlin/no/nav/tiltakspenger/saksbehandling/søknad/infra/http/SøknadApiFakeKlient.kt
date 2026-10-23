package no.nav.tiltakspenger.saksbehandling.søknad.infra.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.søknad.domene.KunneIkkeSendeSøknadsutfall
import no.nav.tiltakspenger.saksbehandling.søknad.domene.SøknadApiKlient
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfall
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Registrerer hvilke søknadsutfall som ble sendt, slik at tester kan assertere på sideeffekten av [no.nav.tiltakspenger.saksbehandling.søknad.service.SendSøknadsutfallTilSøknadApiJobb].
 * Tester kan legge inn en feil for en søknad med [feilFor], og fjerne den igjen med `null`.
 */
class SøknadApiFakeKlient : SøknadApiKlient {

    val sendteSøknadsutfall = ConcurrentLinkedQueue<Søknadsutfall>()

    private val feil = ConcurrentHashMap<SøknadId, KunneIkkeSendeSøknadsutfall>()

    fun feilFor(søknadId: SøknadId, kunneIkkeSende: KunneIkkeSendeSøknadsutfall?) {
        if (kunneIkkeSende == null) feil.remove(søknadId) else feil[søknadId] = kunneIkkeSende
    }

    override suspend fun sendSøknadsutfall(søknadsutfall: Søknadsutfall): Either<KunneIkkeSendeSøknadsutfall, Unit> {
        feil[søknadsutfall.søknadId]?.let { return it.left() }
        sendteSøknadsutfall.add(søknadsutfall)
        return Unit.right()
    }
}
