@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse as LibsTiltaksdeltakelse

/**
 * Fake for lokal kjøring (LocalApplicationContext).
 * Utleder tiltaksdeltakelsene fra personens lagrede søknader, siden lokal kjøring ikke seeder deltakelser per fnr.
 * Testene bruker [TiltaksdeltakelseFakeTestKlient].
 */
class TiltaksdeltakelseFakeLokalKlient(
    private val hentSøknader: suspend (Fnr) -> List<Søknad>,
) : TiltaksdeltakelseKlient {

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelser> {
        return Tiltaksdeltakelser(hentTiltaksdeltakelserFraSøknader(fnr)).right()
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, List<TiltaksdeltakelseMedArrangørnavn>> {
        return listOf(ObjectMother.tiltaksdeltakelseMedArrangørnavn()).right()
    }

    /**
     * Lokal kjøring har ingen nå-tilstand for deltakelser, så jobben for oppdaterte tiltaksdeltakelser finner aldri en lesbar nå-tilstand.
     */
    override suspend fun hentTiltaksdeltakelse(
        fnr: Fnr,
        eksternDeltakerId: String,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, LibsTiltaksdeltakelse?> {
        return null.right()
    }

    private suspend fun hentTiltaksdeltakelserFraSøknader(fnr: Fnr): List<LibsTiltaksdeltakelse.GirRett> {
        // TODO: Denne utledningen av tiltaksdeltakelser fra søknaden er skjør og henger tett sammen med søknadsflyten.
        // Den fungerer bare når søknaden allerede er persistert.
        // For manuelt registrerte (papir) søknader beregnes saksopplysningene før søknaden lagres (se StartBehandlingAvManueltRegistrertSøknadService), så tiltaksdeltakelsen mangler på saksopplysning-tidspunktet og lister returneres tom.
        // Den forutsetter også at toTiltak()/tilLibsDeltakelse() bevarer internDeltakelseId slik at en påfølgende innvilgelse matcher.
        // Vurder å seede deltakelser eksplisitt per fnr når en søknad opprettes (både digital seed og papir-route) i stedet for å utlede fra lagrede søknader.
        return hentSøknader(fnr)
            .sortedByDescending { it.opprettet }
            .mapNotNull { it.tiltak?.toTiltak() }
            .distinctBy { it.eksternDeltakelseId }
            .map { it.tilLibsDeltakelse() }
    }
}
