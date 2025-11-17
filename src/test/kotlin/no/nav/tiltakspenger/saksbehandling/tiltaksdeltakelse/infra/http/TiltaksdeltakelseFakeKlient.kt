@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient

class TiltaksdeltakelseFakeKlient(
    private val defaultTiltaksdeltagelserTilSøknadHvisDenMangler: Boolean = false,
    private val søknadRepoProvider: suspend () -> SøknadRepo? = { null },
) : TiltaksdeltakelseKlient {
    private val data = Atomic(mutableMapOf<Fnr, Tiltaksdeltakelser>())

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Tiltaksdeltakelser {
        return data.get()[fnr] ?: if (defaultTiltaksdeltagelserTilSøknadHvisDenMangler) hentTiltaksdeltagelseFraSøknad(fnr) else Tiltaksdeltakelser.empty()
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): List<TiltaksdeltakelseMedArrangørnavn> {
        return listOf(ObjectMother.tiltaksdeltagelseMedArrangørnavn())
    }

    fun lagre(
        fnr: Fnr,
        tiltaksdeltakelse: Tiltaksdeltakelse?,
    ) {
        val current = data.get()[fnr]
        if (tiltaksdeltakelse == null) {
            data.get().remove(fnr)
            return
        }
        if (current == null) {
            data.get()[fnr] = Tiltaksdeltakelser(listOf(tiltaksdeltakelse))
            return
        }
        data.get()[fnr] = if (current.getTiltaksdeltagelse(tiltaksdeltakelse.eksternDeltagelseId) != null) {
            Tiltaksdeltakelser(
                current.map {
                    if (it.eksternDeltagelseId == tiltaksdeltakelse.eksternDeltagelseId) {
                        tiltaksdeltakelse
                    } else {
                        it
                    }
                },
            )
        } else {
            Tiltaksdeltakelser(current + tiltaksdeltakelse)
        }
    }

    private suspend fun hentTiltaksdeltagelseFraSøknad(fnr: Fnr): Tiltaksdeltakelser {
        val søknadRepo = søknadRepoProvider()!!
        val søknader = søknadRepo.hentSøknaderForFnr(fnr)
        val tiltak = søknader.lastOrNull()?.tiltak?.toTiltak()

        return tiltak?.let { Tiltaksdeltakelser(listOf(it)) } ?: Tiltaksdeltakelser(emptyList())
    }
}
