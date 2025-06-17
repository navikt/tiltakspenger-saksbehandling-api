package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadFakeRepo

class BenkOversiktFakeRepo(
    private val søknadFakeRepo: SøknadFakeRepo,
    private val behandlingFakeRepo: BehandlingFakeRepo,
    private val meldekortBehandlingFakeRepo: MeldekortBehandlingFakeRepo,
) : BenkOversiktRepo {

    override fun hentÅpneBehandlinger(
        command: HentÅpneBehandlingerCommand,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkOversikt {
        return BenkOversikt(
            behandlingssammendrag = hentÅpneBehandlinger(command) + hentÅpneMeldekortBehandlinger(command) + hentÅpneSøknader(),
            totalAntall = hentÅpneBehandlinger(command).size + hentÅpneMeldekortBehandlinger(command).size + hentÅpneSøknader().size,
        )
    }

    private fun BehandlingssammendragType.toBehandlingstype(): Behandlingstype = when (this) {
        BehandlingssammendragType.SØKNADSBEHANDLING -> Behandlingstype.SØKNADSBEHANDLING
        BehandlingssammendragType.REVURDERING -> Behandlingstype.REVURDERING
        BehandlingssammendragType.MELDEKORTBEHANDLING -> throw IllegalArgumentException("Meldekortbehanding er ikke en behandlingstype")
    }

    private fun BehandlingssammendragStatus.toBehandlingsstatus(): Behandlingsstatus = when (this) {
        BehandlingssammendragStatus.KLAR_TIL_BEHANDLING -> Behandlingsstatus.KLAR_TIL_BEHANDLING
        BehandlingssammendragStatus.UNDER_BEHANDLING -> Behandlingsstatus.UNDER_BEHANDLING
        BehandlingssammendragStatus.KLAR_TIL_BESLUTNING -> Behandlingsstatus.KLAR_TIL_BESLUTNING
        BehandlingssammendragStatus.UNDER_BESLUTNING -> Behandlingsstatus.UNDER_BESLUTNING
    }

    private fun Behandlingstype.toBehandlingssammendragType(): BehandlingssammendragType = when (this) {
        Behandlingstype.SØKNADSBEHANDLING -> BehandlingssammendragType.SØKNADSBEHANDLING
        Behandlingstype.REVURDERING -> BehandlingssammendragType.REVURDERING
    }

    private fun Behandlingsstatus.toBehandlingssammendragStatus(): BehandlingssammendragStatus = when (this) {
        Behandlingsstatus.KLAR_TIL_BEHANDLING -> BehandlingssammendragStatus.KLAR_TIL_BEHANDLING
        Behandlingsstatus.UNDER_BEHANDLING -> BehandlingssammendragStatus.UNDER_BEHANDLING
        Behandlingsstatus.KLAR_TIL_BESLUTNING -> BehandlingssammendragStatus.KLAR_TIL_BESLUTNING
        Behandlingsstatus.UNDER_BESLUTNING -> BehandlingssammendragStatus.UNDER_BESLUTNING
        Behandlingsstatus.VEDTATT -> throw IllegalStateException("Vedtatte behandlinger skal ikke være åpne")
        Behandlingsstatus.AVBRUTT -> throw IllegalStateException("Avbrutte behandlinger skal ikke være åpne")
    }

    private fun hentÅpneBehandlinger(command: HentÅpneBehandlingerCommand): List<Behandlingssammendrag> {
        val ønsketBehandlingstype = command.åpneBehandlingerFiltrering.behandlingstype?.filter {
            Either.catch {
                it.toBehandlingstype()
            }.fold(
                ifLeft = { false },
                ifRight = { true },
            )
        }?.map {
            it.toBehandlingstype()
        }

        val ønsketBehandlingsstatus = command.åpneBehandlingerFiltrering.status?.filter {
            Either.catch {
                it.toBehandlingsstatus()
            }.fold(
                ifLeft = { false },
                ifRight = { true },
            )
        }?.map {
            it.toBehandlingsstatus()
        }

        return behandlingFakeRepo.alle.filter {
            it.avbrutt == null ||
                ønsketBehandlingstype?.contains(it.behandlingstype) == true ||
                ønsketBehandlingsstatus?.contains(it.status) == true
        }.map { behandling ->
            Behandlingssammendrag(
                fnr = behandling.fnr,
                saksnummer = behandling.saksnummer,
                startet = behandling.opprettet,
                behandlingstype = behandling.behandlingstype.toBehandlingssammendragType(),
                status = behandling.status.toBehandlingssammendragStatus(),
                saksbehandler = behandling.saksbehandler,
                beslutter = behandling.beslutter,
                sakId = behandling.sakId,
                kravtidspunkt = if (behandling.behandlingstype == Behandlingstype.SØKNADSBEHANDLING) behandling.opprettet else null,
            )
        }
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

    private fun hentÅpneMeldekortBehandlinger(command: HentÅpneBehandlingerCommand): List<Behandlingssammendrag> {
        val ønsketBehandlingsstatus = command.åpneBehandlingerFiltrering.status?.filter {
            Either.catch {
                it.toMeldekortBehandlingStatus()
            }.fold(
                ifLeft = { false },
                ifRight = { true },
            )
        }?.map {
            it.toMeldekortBehandlingStatus()
        }

        return meldekortBehandlingFakeRepo.alle.filter {
            it.avbrutt == null ||
                ønsketBehandlingsstatus?.contains(it.status) == true
        }.map {
            Behandlingssammendrag(
                sakId = it.sakId,
                fnr = it.fnr,
                saksnummer = it.saksnummer,
                startet = it.opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                status = it.status.toBehandlingssamendragStatus(),
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
            )
        }
    }

    private fun BehandlingssammendragStatus.toMeldekortBehandlingStatus(): MeldekortBehandlingStatus = when (this) {
        BehandlingssammendragStatus.KLAR_TIL_BEHANDLING -> MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING
        BehandlingssammendragStatus.UNDER_BEHANDLING -> MeldekortBehandlingStatus.UNDER_BEHANDLING
        BehandlingssammendragStatus.KLAR_TIL_BESLUTNING -> MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
        BehandlingssammendragStatus.UNDER_BESLUTNING -> MeldekortBehandlingStatus.UNDER_BESLUTNING
    }

    private fun MeldekortBehandlingStatus.toBehandlingssamendragStatus(): BehandlingssammendragStatus = when (this) {
        MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> BehandlingssammendragStatus.KLAR_TIL_BEHANDLING
        MeldekortBehandlingStatus.UNDER_BEHANDLING -> BehandlingssammendragStatus.UNDER_BEHANDLING
        MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> BehandlingssammendragStatus.KLAR_TIL_BESLUTNING
        MeldekortBehandlingStatus.UNDER_BESLUTNING -> BehandlingssammendragStatus.UNDER_BESLUTNING
        MeldekortBehandlingStatus.AVBRUTT -> throw IllegalStateException("Avbrutte meldekortbehandlinger skal ikke være åpne")
        MeldekortBehandlingStatus.GODKJENT -> throw IllegalStateException("Godkjente meldekortbehandlinger skal ikke være åpne")
        MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET -> throw IllegalStateException("Automatisk behandlede meldekortbehandlinger skal ikke være åpne")
        MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> throw IllegalStateException("Ikke rett til tiltakspenger meldekortbehandlinger skal ikke være åpne")
    }
}
