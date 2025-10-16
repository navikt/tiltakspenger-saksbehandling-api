package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltagelserDetErSøktTiltakspengerFor

interface TiltaksdeltagelseKlient {
    /**
     * Filtrerer vekk tiltaksdeltagelser som ikke gir rett til tiltakspenger.
     * Filtrer vekk tiltaksdeltagelser som mangler både fraOgMed og tilOgMed samtidig som den ikke venter på oppstart.
     * Tiltak som det er søkt om tiltakspenger for skal ikke filtreres bort så lenge tiltakstypen gir rett på tiltakspenger.
     */
    suspend fun hentTiltaksdeltagelser(
        fnr: Fnr,
        tiltaksdeltagelserDetErSøktTiltakspengerFor: TiltaksdeltagelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Tiltaksdeltagelser
}
