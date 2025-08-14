@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient

class TiltaksdeltagelseFakeKlient(private val søknadRepo: SøknadRepo) : TiltaksdeltagelseKlient {
    private val data = Atomic(mutableMapOf<Fnr, Tiltaksdeltagelser>())

    override suspend fun hentTiltaksdeltagelser(
        fnr: Fnr,
        correlationId: CorrelationId,
    ): Tiltaksdeltagelser {
        return data.get()[fnr] ?: hentTiltaksdeltagelseFraSøknad(fnr)
    }

    fun lagre(
        fnr: Fnr,
        tiltaksdeltagelse: Tiltaksdeltagelse,
    ) {
        data.get()[fnr] = Tiltaksdeltagelser(listOf(tiltaksdeltagelse))
    }

    private fun hentTiltaksdeltagelseFraSøknad(fnr: Fnr): Tiltaksdeltagelser {
        val søknader = søknadRepo.hentSøknaderForFnr(fnr)
        val tiltak = søknader.lastOrNull()?.tiltak?.toTiltak()

        return tiltak?.let { Tiltaksdeltagelser(listOf(it)) } ?: Tiltaksdeltagelser(emptyList())
    }
}
