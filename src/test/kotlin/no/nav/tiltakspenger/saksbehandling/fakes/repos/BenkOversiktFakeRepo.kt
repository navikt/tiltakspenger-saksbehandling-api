package no.nav.tiltakspenger.saksbehandling.fakes.repos

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo

class BenkOversiktFakeRepo(
    private val søknadFakeRepo: SøknadFakeRepo,
    private val behandlingFakeRepo: BehandlingFakeRepo,
) : BenkOversiktRepo {

    override fun hentÅpneBehandlinger(
        sessionContext: SessionContext?,
        limit: Int,
    ): List<Behandlingssammendrag> {
        return hentÅpneBehandlinger() + hentÅpneSøknader()
    }

    private fun hentÅpneBehandlinger(): List<Behandlingssammendrag> =
        behandlingFakeRepo.alle.filter { it.avbrutt == null }.map { behandling ->
            Behandlingssammendrag(
                fnr = behandling.fnr,
                saksnummer = behandling.saksnummer,
                startet = behandling.opprettet,
                behandlingstype = when (behandling.behandlingstype) {
                    Behandlingstype.SØKNADSBEHANDLING -> BehandlingssammendragType.SØKNADSBEHANDLING
                    Behandlingstype.REVURDERING -> BehandlingssammendragType.REVURDERING
                },
                status = behandling.status,
                saksbehandler = behandling.saksbehandler,
                beslutter = behandling.beslutter,

            )
        }

    private fun hentÅpneSøknader(): List<Behandlingssammendrag> = søknadFakeRepo.alle.map { søknad ->
        Behandlingssammendrag(
            fnr = søknad.fnr,
            saksnummer = søknad.saksnummer,
            startet = søknad.opprettet,
            behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
            status = null,
            saksbehandler = null,
            beslutter = null,
        )
    }
}
