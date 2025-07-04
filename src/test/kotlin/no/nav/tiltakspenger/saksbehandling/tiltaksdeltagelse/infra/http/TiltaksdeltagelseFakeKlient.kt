@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient

class TiltaksdeltagelseFakeKlient(private val søknadRepo: SøknadRepo) : TiltaksdeltagelseKlient {
    private val data = Atomic(mutableMapOf<Fnr, List<Tiltaksdeltagelse>>())

    override suspend fun hentTiltaksdeltagelser(
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
