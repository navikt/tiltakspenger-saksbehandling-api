@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelseDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient

class TiltaksdeltagelseFakeKlient(
    private val søknadRepoProvider: suspend () -> SøknadRepo? = { null },
) : TiltaksdeltagelseKlient {
    private val data = Atomic(mutableMapOf<Fnr, Tiltaksdeltagelser>())

    override suspend fun hentTiltaksdeltagelser(
        fnr: Fnr,
        tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Tiltaksdeltagelser {
        return data.get()[fnr] ?: hentTiltaksdeltagelseFraSøknad(fnr)
    }

    fun lagre(
        fnr: Fnr,
        tiltaksdeltagelse: Tiltaksdeltagelse,
    ) {
        val current = data.get()[fnr]
        if (current == null) {
            data.get()[fnr] = Tiltaksdeltagelser(listOf(tiltaksdeltagelse))
            return
        }
        data.get()[fnr] = if (current.getTiltaksdeltagelse(tiltaksdeltagelse.eksternDeltagelseId) != null) {
            Tiltaksdeltagelser(
                current.map {
                    if (it.eksternDeltagelseId == tiltaksdeltagelse.eksternDeltagelseId) {
                        tiltaksdeltagelse
                    } else {
                        it
                    }
                },
            )
        } else {
            Tiltaksdeltagelser(current + tiltaksdeltagelse)
        }
    }

    private suspend fun hentTiltaksdeltagelseFraSøknad(fnr: Fnr): Tiltaksdeltagelser {
        val søknadRepo = søknadRepoProvider()!!
        val søknader = søknadRepo.hentSøknaderForFnr(fnr)
        val tiltak = søknader.lastOrNull()?.tiltak?.toTiltak()

        return tiltak?.let { Tiltaksdeltagelser(listOf(it)) } ?: Tiltaksdeltagelser(emptyList())
    }
}
