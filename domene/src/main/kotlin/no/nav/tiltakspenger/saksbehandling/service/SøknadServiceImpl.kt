package no.nav.tiltakspenger.saksbehandling.service

import no.nav.tiltakspenger.felles.Systembruker
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.ports.SøknadRepo

class SøknadServiceImpl(
    private val søknadRepo: SøknadRepo,
) : SøknadService {
    override fun nySøknad(søknad: Søknad, sakId: SakId, systembruker: Systembruker) {
        require(systembruker.roller.harLageHendelser()) { "Systembruker mangler rollen LAGE_HENDELSER. Systembrukers roller: ${systembruker.roller}" }
        søknadRepo.lagre(søknad, sakId)
    }

    override fun hentSøknad(søknadId: SøknadId): Søknad {
        return søknadRepo.hentForSøknadId(søknadId)!!
    }

    override fun hentSakIdForSoknad(søknadId: SøknadId): SakId {
        return søknadRepo.hentSakIdForSoknad(søknadId) ?: throw IllegalStateException("Fant ikke sak for søknad med id $søknadId")
    }
}
