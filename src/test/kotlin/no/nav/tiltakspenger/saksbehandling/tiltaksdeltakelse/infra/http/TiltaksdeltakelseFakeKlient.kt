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
    private val defaultTiltaksdeltakelserTilSøknadHvisDenMangler: Boolean = false,
    private val søknadRepoProvider: suspend () -> SøknadRepo? = { null },
) : TiltaksdeltakelseKlient {
    private val data = Atomic(mutableMapOf<Fnr, TiltaksdeltakelserFraRegister>())

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): TiltaksdeltakelserFraRegister {
        return data.get()[fnr] ?: if (defaultTiltaksdeltakelserTilSøknadHvisDenMangler) hentTiltaksdeltakelseFraSøknad(fnr) else TiltaksdeltakelserFraRegister.empty()
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): List<TiltaksdeltakelseMedArrangørnavn> {
        return listOf(ObjectMother.tiltaksdeltakelseMedArrangørnavn())
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
        val tiltaksdeltakelseFraRegister = tiltaksdeltakelse.toTiltaksdeltakelseFraRegister()
        if (current == null) {
            data.get()[fnr] = TiltaksdeltakelserFraRegister(listOf(tiltaksdeltakelseFraRegister))
            return
        }
        data.get()[fnr] = if (current.getTiltaksdeltakelse(tiltaksdeltakelse.eksternDeltakelseId) != null) {
            TiltaksdeltakelserFraRegister(
                current.map {
                    if (it.eksternDeltakelseId == tiltaksdeltakelse.eksternDeltakelseId) {
                        tiltaksdeltakelseFraRegister
                    } else {
                        it
                    }
                },
            )
        } else {
            TiltaksdeltakelserFraRegister(current + tiltaksdeltakelseFraRegister)
        }
    }

    private suspend fun hentTiltaksdeltakelseFraSøknad(fnr: Fnr): TiltaksdeltakelserFraRegister {
        val søknadRepo = søknadRepoProvider()!!
        val søknader = søknadRepo.hentSøknaderForFnr(fnr)
        val tiltak = søknader.mapNotNull { it.tiltak?.toTiltak() }.distinctBy { it.eksternDeltakelseId }
            .map { it.toTiltaksdeltakelseFraRegister() }

        return TiltaksdeltakelserFraRegister(tiltak)
    }
}

fun Tiltaksdeltakelse.toTiltaksdeltakelseFraRegister(): TiltaksdeltakelseFraRegister =
    TiltaksdeltakelseFraRegister(
        eksternDeltakelseId = eksternDeltakelseId,
        gjennomføringId = gjennomføringId,
        typeNavn = typeNavn,
        typeKode = typeKode,
        rettPåTiltakspenger = rettPåTiltakspenger,
        deltakelseFraOgMed = deltakelseFraOgMed,
        deltakelseTilOgMed = deltakelseTilOgMed,
        deltakelseStatus = deltakelseStatus,
        deltakelseProsent = deltakelseProsent,
        antallDagerPerUke = antallDagerPerUke,
        kilde = kilde,
        deltidsprosentGjennomforing = deltidsprosentGjennomforing,
    )
