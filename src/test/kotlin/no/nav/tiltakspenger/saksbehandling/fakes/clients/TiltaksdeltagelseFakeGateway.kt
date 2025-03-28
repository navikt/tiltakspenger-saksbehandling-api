@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.fakes.clients

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseGateway

class TiltaksdeltagelseFakeGateway(private val søknadRepo: SøknadRepo) : TiltaksdeltagelseGateway {
    private val data = Atomic(mutableMapOf<Fnr, List<Tiltaksdeltagelse>>())

    override suspend fun hentTiltaksdeltagelse(
        fnr: Fnr,
        correlationId: CorrelationId,
    ): List<Tiltaksdeltagelse> {
        return data.get()[fnr] ?: hentTiltaksdeltagelseFraSøknad(fnr)
    }

    fun lagre(
        fnr: Fnr,
        tiltaksdeltagelse: Tiltaksdeltagelse,
    ) {
        data.get()[fnr] = listOf(tiltaksdeltagelse)
    }

    private fun hentTiltaksdeltagelseFraSøknad(fnr: Fnr): List<Tiltaksdeltagelse> {
        val søknader = søknadRepo.hentSøknaderForFnr(fnr)
        val tiltak = søknader.lastOrNull()?.tiltak?.toTiltak()

        return tiltak?.let { listOf(it) } ?: emptyList()
    }
}
