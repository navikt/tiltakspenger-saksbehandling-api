package no.nav.tiltakspenger.fakes.repos

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.domene.benk.BehandlingEllerSøknadForSaksoversikt
import no.nav.tiltakspenger.saksbehandling.domene.benk.BenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.domene.benk.toBenkBehandlingstype
import no.nav.tiltakspenger.saksbehandling.ports.SaksoversiktRepo

class SaksoversiktFakeRepo(
    private val søknadFakeRepo: SøknadFakeRepo,
    private val behandlingFakeRepo: BehandlingFakeRepo,
) : SaksoversiktRepo {

    override fun hentAlleBehandlinger(sessionContext: SessionContext?): List<BehandlingEllerSøknadForSaksoversikt> {
        return behandlingFakeRepo.alle.map { behandling ->
            val status = BehandlingEllerSøknadForSaksoversikt.Status.Behandling(behandling.status)

            BehandlingEllerSøknadForSaksoversikt(
                periode = behandling.virkningsperiode,
                status = status,
                underkjent = behandling.attesteringer.any { it.isUnderkjent() },
                // Kommentar jah: Dette vil ikke fungere hvis vi utvider denne til revurdering.
                kravtidspunkt = behandling.kravfrist!!,
                behandlingstype = behandling.behandlingstype.toBenkBehandlingstype(),
                fnr = behandling.fnr,
                saksnummer = behandling.saksnummer,
                id = behandling.id,
                saksbehandler = behandling.saksbehandler,
                beslutter = behandling.beslutter,
                sakId = behandling.sakId,
                erDeprecatedBehandling = behandling.vilkårssett != null,
            )
        }
    }

    override fun hentAlleSøknader(sessionContext: SessionContext?): List<BehandlingEllerSøknadForSaksoversikt> {
        return søknadFakeRepo.alle.map { søknad ->
            val status = BehandlingEllerSøknadForSaksoversikt.Status.Søknad
            BehandlingEllerSøknadForSaksoversikt(
                periode = null,
                status = status,
                underkjent = false,
                kravtidspunkt = søknad.opprettet,
                behandlingstype = BenkBehandlingstype.SØKNAD,
                fnr = søknad.fnr,
                saksnummer = null,
                id = søknad.id,
                saksbehandler = null,
                beslutter = null,
                sakId = null,
            )
        }
    }
}
