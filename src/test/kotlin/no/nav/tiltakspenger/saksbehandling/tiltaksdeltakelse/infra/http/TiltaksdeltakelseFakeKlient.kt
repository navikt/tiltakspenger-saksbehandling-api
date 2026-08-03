@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient

class TiltaksdeltakelseFakeKlient(
    /**
     * Utleder tiltaksdeltakelser fra personens lagrede søknader når ingen deltakelser er seedet for fnr-et.
     * Brukes kun av LocalApplicationContext; testene seeder deltakelser eksplisitt via [lagre].
     */
    private val søknadFallback: (suspend (Fnr) -> List<Søknad>)? = null,
) : TiltaksdeltakelseKlient {
    private val data = Atomic(mutableMapOf<Fnr, TiltaksdeltakelserFraRegister>())

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, TiltaksdeltakelserFraRegister> {
        return (
            data.get()[fnr] ?: if (søknadFallback != null) {
                hentTiltaksdeltakelseFraSøknad(fnr, søknadFallback)
            } else {
                TiltaksdeltakelserFraRegister.empty()
            }
            ).right()
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<TiltaksdeltakelseMedArrangørnavn>> {
        return listOf(ObjectMother.tiltaksdeltakelseMedArrangørnavn()).right()
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

    private suspend fun hentTiltaksdeltakelseFraSøknad(
        fnr: Fnr,
        søknadFallback: suspend (Fnr) -> List<Søknad>,
    ): TiltaksdeltakelserFraRegister {
        // TODO: Denne utledningen av tiltaksdeltakelser fra søknaden er skjør og henger tett sammen med søknadsflyten.
        // Den fungerer bare når søknaden allerede er persistert.
        // For manuelt registrerte (papir) søknader beregnes saksopplysningene før søknaden lagres (se StartBehandlingAvManueltRegistrertSøknadService), så tiltaksdeltakelsen mangler på saksopplysning-tidspunktet og lister returneres tom.
        // Den forutsetter også at toTiltak()/toTiltaksdeltakelseFraRegister() bevarer internDeltakelseId slik at en påfølgende innvilgelse matcher.
        // Vurder å seede data[fnr] eksplisitt når en søknad opprettes (både digital seed og papir-route) i stedet for å utlede fra lagrede søknader.
        val søknader = søknadFallback(fnr)
        val tiltak = søknader
            .sortedByDescending { it.opprettet }
            .mapNotNull { it.tiltak?.toTiltak() }
            .distinctBy { it.eksternDeltakelseId }
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
