package no.nav.tiltakspenger.vedtak.service.behandling

import no.nav.tiltakspenger.domene.behandling.Behandling
import no.nav.tiltakspenger.domene.behandling.BehandlingVilkårsvurdert
import no.nav.tiltakspenger.domene.behandling.Søknadsbehandling
import no.nav.tiltakspenger.domene.saksopplysning.Saksopplysning
import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.vedtak.Søknad
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingRepo

class BehandlingServiceImpl(
    val behandlingRepo: BehandlingRepo,
) : BehandlingService {
    override fun automatiskSaksbehandle(
        // behandling: Søknadsbehandling.Opprettet,
        søknad: Søknad,
        saksopplysning: List<Saksopplysning>,
        saksbehandler: Saksbehandler,
    ): Behandling {
        val behandling = Søknadsbehandling.Opprettet.opprettBehandling(søknad = søknad)
        val behandlingVilkårsvurdert = behandling.vilkårsvurder(
            saksopplysninger = saksopplysning,
        )

        return when (behandlingVilkårsvurdert) {
            is BehandlingVilkårsvurdert.Avslag -> behandlingVilkårsvurdert.iverksett(saksbehandler)
            is BehandlingVilkårsvurdert.Innvilget -> behandlingVilkårsvurdert.iverksett(saksbehandler)
            is BehandlingVilkårsvurdert.Manuell -> return behandlingVilkårsvurdert
        }
    }

    override fun hentBehandling(behandlingId: BehandlingId): Behandling {
        return behandlingRepo.hent(behandlingId)
    }

    override fun lagreBehandling(behandling: Behandling) {
        when (behandling) {
            is Søknadsbehandling -> behandlingRepo.lagre(behandling)
        }
    }
}
