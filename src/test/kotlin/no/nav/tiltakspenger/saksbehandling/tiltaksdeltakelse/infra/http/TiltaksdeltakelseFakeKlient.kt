@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelser
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse as LibsTiltaksdeltakelse

class TiltaksdeltakelseFakeKlient(
    /**
     * Utleder tiltaksdeltakelser fra personens lagrede søknader når ingen deltakelser er seedet for fnr-et.
     * Brukes kun av LocalApplicationContext; testene seeder deltakelser eksplisitt via [lagre].
     */
    private val søknadFallback: (suspend (Fnr) -> List<Søknad>)? = null,
) : TiltaksdeltakelseKlient {
    private val data = Atomic(mutableMapOf<Fnr, List<LibsTiltaksdeltakelse.GirRett>>())

    /** Registrerer kall til [hentTiltaksdeltakelse] som (fnr, eksternDeltakerId), slik at tester kan verifisere at jobben slo opp nå-tilstanden. */
    val hentTiltaksdeltakelseKall = Atomic(mutableListOf<Pair<Fnr, String>>())

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelser> {
        return Tiltaksdeltakelser(
            data.get()[fnr] ?: if (søknadFallback != null) {
                hentTiltaksdeltakelseFraSøknad(fnr, søknadFallback)
            } else {
                emptyList()
            },
        ).right()
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, List<TiltaksdeltakelseMedArrangørnavn>> {
        return listOf(ObjectMother.tiltaksdeltakelseMedArrangørnavn()).right()
    }

    override suspend fun hentTiltaksdeltakelse(
        fnr: Fnr,
        eksternDeltakerId: String,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, LibsTiltaksdeltakelse?> {
        hentTiltaksdeltakelseKall.get().add(fnr to eksternDeltakerId)
        return data.get()[fnr]?.find { it.id.verdi == eksternDeltakerId }.right()
    }

    fun lagre(
        fnr: Fnr,
        tiltaksdeltakelse: TiltaksdeltakelseIntern?,
    ) {
        val current = data.get()[fnr]
        if (tiltaksdeltakelse == null) {
            data.get().remove(fnr)
            return
        }
        val deltakelse = tiltaksdeltakelse.tilLibsDeltakelse()
        if (current == null) {
            data.get()[fnr] = listOf(deltakelse)
            return
        }
        data.get()[fnr] = if (current.any { it.id == deltakelse.id }) {
            current.map { if (it.id == deltakelse.id) deltakelse else it }
        } else {
            current + deltakelse
        }
    }

    private suspend fun hentTiltaksdeltakelseFraSøknad(
        fnr: Fnr,
        søknadFallback: suspend (Fnr) -> List<Søknad>,
    ): List<LibsTiltaksdeltakelse.GirRett> {
        // TODO: Denne utledningen av tiltaksdeltakelser fra søknaden er skjør og henger tett sammen med søknadsflyten.
        // Den fungerer bare når søknaden allerede er persistert.
        // For manuelt registrerte (papir) søknader beregnes saksopplysningene før søknaden lagres (se StartBehandlingAvManueltRegistrertSøknadService), så tiltaksdeltakelsen mangler på saksopplysning-tidspunktet og lister returneres tom.
        // Den forutsetter også at toTiltak()/tilLibsDeltakelse() bevarer internDeltakelseId slik at en påfølgende innvilgelse matcher.
        // Vurder å seede data[fnr] eksplisitt når en søknad opprettes (både digital seed og papir-route) i stedet for å utlede fra lagrede søknader.
        val søknader = søknadFallback(fnr)
        val tiltak = søknader
            .sortedByDescending { it.opprettet }
            .mapNotNull { it.tiltak?.toTiltak() }
            .distinctBy { it.eksternDeltakelseId }
            .map { it.tilLibsDeltakelse() }

        return tiltak
    }
}
