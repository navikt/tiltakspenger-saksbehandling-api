package no.nav.tiltakspenger.saksbehandling.service

import no.nav.tiltakspenger.felles.Systembruker
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad

interface SøknadService {
    /** Skal i førsteomgang kun brukes til digitale søknader. Dersom en saksbehandler skal registere en papirsøknad må vi ha en egen funksjon som sjekker tilgang.*/
    fun nySøknad(søknad: Søknad, sakId: SakId, systembruker: Systembruker)

    fun hentSøknad(søknadId: SøknadId): Søknad

    fun hentSakIdForSoknad(søknadId: SøknadId): SakId
}
