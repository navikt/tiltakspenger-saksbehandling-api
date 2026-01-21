package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadFakeRepo

class BenkOversiktFakeRepo(
    private val søknadFakeRepo: SøknadFakeRepo,
    private val behandlingFakeRepo: RammebehandlingFakeRepo,
    private val meldekortBehandlingFakeRepo: MeldekortBehandlingFakeRepo,
    private val klagebehandlingFakeRepo: KlagebehandlingFakeRepo,
) : BenkOversiktRepo {

    override fun hentÅpneBehandlinger(
        command: HentÅpneBehandlingerCommand,
        sessionContext: SessionContext?,
        limit: Int,
    ): BenkOversikt {
        return BenkOversikt(
            behandlingssammendrag =
            hentÅpneBehandlinger(command) +
                hentÅpneMeldekortBehandlinger(command) +
                hentÅpneSøknader() +
                hentÅpneKlagebehandlinger(command),
            totalAntall =
            hentÅpneBehandlinger(command).size +
                hentÅpneMeldekortBehandlinger(command).size +
                hentÅpneSøknader().size +
                hentÅpneKlagebehandlinger(command).size,
        )
    }

    private fun BehandlingssammendragType.toBehandlingstype(): Behandlingstype = when (this) {
        BehandlingssammendragType.SØKNADSBEHANDLING -> Behandlingstype.SØKNADSBEHANDLING
        BehandlingssammendragType.REVURDERING -> Behandlingstype.REVURDERING
        BehandlingssammendragType.MELDEKORTBEHANDLING -> throw IllegalArgumentException("Meldekortbehanding er ikke en behandlingstype")
        BehandlingssammendragType.INNSENDT_MELDEKORT -> throw IllegalArgumentException("Innsendt meldekort er ikke en behandlingstype")
        BehandlingssammendragType.KORRIGERT_MELDEKORT -> throw IllegalArgumentException("Korrigert meldekort er ikke en behandlingstype")
        BehandlingssammendragType.KLAGEBEHANDLING -> throw IllegalArgumentException("Klagebehandling er ikke en behandlingstype")
    }

    private fun BehandlingssammendragStatus.toBehandlingsstatus(): Rammebehandlingsstatus = when (this) {
        BehandlingssammendragStatus.UNDER_AUTOMATISK_BEHANDLING -> Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        BehandlingssammendragStatus.KLAR_TIL_BEHANDLING -> Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        BehandlingssammendragStatus.UNDER_BEHANDLING -> Rammebehandlingsstatus.UNDER_BEHANDLING
        BehandlingssammendragStatus.KLAR_TIL_BESLUTNING -> Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        BehandlingssammendragStatus.UNDER_BESLUTNING -> Rammebehandlingsstatus.UNDER_BESLUTNING
    }

    private fun Behandlingstype.toBehandlingssammendragType(): BehandlingssammendragType = when (this) {
        Behandlingstype.SØKNADSBEHANDLING -> BehandlingssammendragType.SØKNADSBEHANDLING
        Behandlingstype.REVURDERING -> BehandlingssammendragType.REVURDERING
    }

    private fun Rammebehandlingsstatus.toBehandlingssammendragStatus(): BehandlingssammendragStatus = when (this) {
        Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> BehandlingssammendragStatus.KLAR_TIL_BEHANDLING
        Rammebehandlingsstatus.UNDER_BEHANDLING -> BehandlingssammendragStatus.UNDER_BEHANDLING
        Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> BehandlingssammendragStatus.KLAR_TIL_BESLUTNING
        Rammebehandlingsstatus.UNDER_BESLUTNING -> BehandlingssammendragStatus.UNDER_BESLUTNING
        Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> BehandlingssammendragStatus.UNDER_AUTOMATISK_BEHANDLING
        Rammebehandlingsstatus.VEDTATT -> throw IllegalStateException("Vedtatte behandlinger skal ikke være åpne")
        Rammebehandlingsstatus.AVBRUTT -> throw IllegalStateException("Avbrutte behandlinger skal ikke være åpne")
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
                erSattPåVent = behandling.ventestatus.erSattPåVent,
                sistEndret = behandling.sistEndret,
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
            erSattPåVent = false,
            sistEndret = null,
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
                erSattPåVent = false,
                sistEndret = it.sistEndret,
            )
        }
    }

    private fun BehandlingssammendragStatus.toMeldekortBehandlingStatus(): MeldekortBehandlingStatus = when (this) {
        BehandlingssammendragStatus.UNDER_AUTOMATISK_BEHANDLING -> throw IllegalStateException("UNDER_AUTOMATISK_BEHANDLING er ikke en tillatt status for meldekortbehandling")
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

    private fun hentÅpneKlagebehandlinger(command: HentÅpneBehandlingerCommand): List<Behandlingssammendrag> {
        val ønsketBehandlingsstatus = command.åpneBehandlingerFiltrering.status?.filter {
            Either.catch {
                it.toKlagebehandlingStatus()
            }.fold(
                ifLeft = { false },
                ifRight = { true },
            )
        }?.map {
            it.toKlagebehandlingStatus()
        }

        return klagebehandlingFakeRepo.alle.filter {
            ønsketBehandlingsstatus?.contains(it.status) == true
        }.map {
            Behandlingssammendrag(
                sakId = it.sakId,
                fnr = it.fnr,
                saksnummer = it.saksnummer,
                startet = it.opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.KLAGEBEHANDLING,
                status = it.status.toBehandlingssamendragStatus(),
                saksbehandler = it.saksbehandler,
                beslutter = null,
                erSattPåVent = false,
                sistEndret = it.sistEndret,
            )
        }
    }

    private fun BehandlingssammendragStatus.toKlagebehandlingStatus(): Klagebehandlingsstatus = when (this) {
        BehandlingssammendragStatus.UNDER_AUTOMATISK_BEHANDLING -> throw IllegalStateException("UNDER_AUTOMATISK_BEHANDLING er ikke en tillatt status for klagebehandling")
        BehandlingssammendragStatus.KLAR_TIL_BEHANDLING -> Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
        BehandlingssammendragStatus.UNDER_BEHANDLING -> Klagebehandlingsstatus.UNDER_BEHANDLING
        BehandlingssammendragStatus.KLAR_TIL_BESLUTNING -> throw IllegalStateException("KLAR_TIL_BESLUTNING er ikke en tillatt status for klagebehandling")
        BehandlingssammendragStatus.UNDER_BESLUTNING -> throw IllegalStateException("UNDER_BESLUTNING er ikke en tillatt status for klagebehandling")
    }

    private fun Klagebehandlingsstatus.toBehandlingssamendragStatus(): BehandlingssammendragStatus = when (this) {
        Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> BehandlingssammendragStatus.KLAR_TIL_BEHANDLING
        Klagebehandlingsstatus.UNDER_BEHANDLING -> BehandlingssammendragStatus.UNDER_BEHANDLING
        Klagebehandlingsstatus.AVBRUTT -> throw IllegalStateException("Avbrutte behandlinger skal ikke være åpne")
        Klagebehandlingsstatus.IVERKSATT -> throw IllegalStateException("Iverksatte behandlinger skal ikke være åpne")
    }
}
