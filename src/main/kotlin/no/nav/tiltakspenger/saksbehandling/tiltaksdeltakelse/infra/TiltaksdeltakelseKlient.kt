package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn

interface TiltaksdeltakelseKlient {
    /**
     * Filtrerer vekk tiltaksdeltakelser som ikke gir rett til tiltakspenger.
     * Filtrer vekk tiltaksdeltakelser som mangler både fraOgMed og tilOgMed samtidig som den ikke venter på oppstart.
     * Tiltak som det er søkt om tiltakspenger for skal ikke filtreres bort så lenge tiltakstypen gir rett på tiltakspenger.
     */
    suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Tiltaksdeltakelser

    suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): List<TiltaksdeltakelseMedArrangørnavn>
}
