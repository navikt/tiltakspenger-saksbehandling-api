package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelser
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn

interface TiltaksdeltakelseKlient {
    /**
     * Inneholder kun [Tiltaksdeltakelse.GirRett] med kjent kildestatus, se [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilRelevanteTiltaksdeltakelser].
     * Filtrerer vekk tiltaksdeltakelser som ikke gir rett til tiltakspenger.
     * Filtrer vekk tiltaksdeltakelser som mangler både fraOgMed og tilOgMed samtidig som den ikke venter på oppstart.
     * Tiltak som det er søkt om tiltakspenger for skal ikke filtreres bort så lenge tiltakstypen gir rett på tiltakspenger.
     */
    suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelser>

    suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, List<TiltaksdeltakelseMedArrangørnavn>>

    /**
     * Henter nå-tilstanden for én tiltaksdeltakelse, slik den ser ut hos kilden — uten mapping til vår interne modell.
     * Tolkningen gjøres av kalleren ([no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilLesbarNåtilstand]).
     * Returnerer null dersom deltakelsen ikke finnes i historikken.
     */
    suspend fun hentTiltaksdeltakelse(
        fnr: Fnr,
        eksternDeltakerId: String,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, Tiltaksdeltakelse?>
}
