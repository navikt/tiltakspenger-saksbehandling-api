package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser

interface TiltaksdeltagelseKlient {
    /**
     * Filtrerer vekk tiltaksdeltagelser som ikke gir rett til tiltakspenger.
     * Filtrer vekk tiltaksdeltagelser som mangler både fraOgMed og tilOgMed samtidig som den ikke venter på oppstart.
     */
    suspend fun hentTiltaksdeltagelser(fnr: Fnr, correlationId: CorrelationId): Tiltaksdeltagelser
}
