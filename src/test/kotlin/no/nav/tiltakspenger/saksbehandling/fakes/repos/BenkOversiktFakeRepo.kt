package no.nav.tiltakspenger.saksbehandling.fakes.repos

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
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
                status = when (behandling.status) {
                    Behandlingsstatus.KLAR_TIL_BEHANDLING -> BehandlingssammendragStatus.KLAR_TIL_BEHANDLING
                    Behandlingsstatus.UNDER_BEHANDLING -> BehandlingssammendragStatus.UNDER_BEHANDLING
                    Behandlingsstatus.KLAR_TIL_BESLUTNING -> BehandlingssammendragStatus.KLAR_TIL_BESLUTNING
                    Behandlingsstatus.UNDER_BESLUTNING -> BehandlingssammendragStatus.UNDER_BESLUTNING
                    Behandlingsstatus.VEDTATT -> throw IllegalStateException("Vedtatte behandlinger skal ikke være åpne")
                    Behandlingsstatus.AVBRUTT -> throw IllegalStateException("Avbrutte behandlinger skal ikke være åpne")
                },
                saksbehandler = behandling.saksbehandler,
                beslutter = behandling.beslutter,
                sakId = behandling.sakId,
                kravtidspunkt = if (behandling.behandlingstype == Behandlingstype.SØKNADSBEHANDLING) behandling.opprettet else null,
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
            sakId = søknad.sakId,
            kravtidspunkt = søknad.opprettet,
        )
    }
}
