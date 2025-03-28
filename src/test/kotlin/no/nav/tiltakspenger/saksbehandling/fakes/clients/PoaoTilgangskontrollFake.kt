@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.fakes.clients

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.ports.PoaoTilgangGateway

class PoaoTilgangskontrollFake : PoaoTilgangGateway {

    private val data = Atomic(mutableMapOf<Fnr, Boolean>())

    fun leggTil(
        fnr: Fnr,
        skjermet: Boolean,
    ) {
        data.get()[fnr] = skjermet
    }

    override suspend fun erSkjermet(fnr: Fnr, correlationId: CorrelationId): Boolean {
        return data.get()[fnr] ?: false
    }
}
