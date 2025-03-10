package no.nav.tiltakspenger.vedtak.saksbehandling.service

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.vedtak.felles.Systembruker
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Søknad

interface SøknadService {
    /** Skal i førsteomgang kun brukes til digitale søknader. Dersom en saksbehandler skal registere en papirsøknad må vi ha en egen funksjon som sjekker tilgang.*/
    suspend fun nySøknad(søknad: Søknad, systembruker: Systembruker)

    fun hentSøknad(søknadId: SøknadId): Søknad

    fun hentSakIdForSoknad(søknadId: SøknadId): SakId

    fun lagreAvbruttSøknad(søknad: Søknad, tx: TransactionContext)
}
