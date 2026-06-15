package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.OvertaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.overta
import no.nav.tiltakspenger.saksbehandling.klage.domene.tilTilknyttetBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.UNDER_BESLUTNING
import java.time.Clock

/**
 * Overtar meldekortbehandlingen for [OvertaMeldekortbehandlingKommando.saksbehandler]:
 *  - [UNDER_BEHANDLING] -> overtar som saksbehandler.
 *  - [UNDER_BESLUTNING] -> overtar som beslutter.
 */
fun Meldekortbehandling.overta(
    kommando: OvertaMeldekortbehandlingKommando,
    clock: Clock,
): Either<KunneIkkeOvertaMeldekortbehandling, Meldekortbehandling> {
    kanOverta(kommando.saksbehandler).onLeft { return it.left() }

    return when (status) {
        UNDER_BEHANDLING -> {
            require(this is MeldekortUnderBehandling) { "Meldekortbehandling med status $status må være MeldekortUnderBehandling" }
            this.copy(
                saksbehandler = kommando.saksbehandler.navIdent,
                sistEndret = nå(clock),
                klagebehandling = klagebehandling?.let { klage ->
                    klage.overta(
                        kommando = OvertaKlagebehandlingKommando(
                            sakId = sakId,
                            klagebehandlingId = klage.id,
                            overtarFra = kommando.overtarFra,
                            saksbehandler = kommando.saksbehandler,
                            correlationId = kommando.correlationId,
                        ),
                        tilknyttetBehandlingsstatus = status.tilTilknyttetBehandlingsstatus(),
                        clock = clock,
                    ).getOrThrow().first
                },
            ).right()
        }

        UNDER_BESLUTNING -> {
            require(this is MeldekortbehandlingManuell) { "Meldekortbehandling med status $status må være MeldekortbehandlingManuell" }
            this.copy(
                beslutter = kommando.saksbehandler.navIdent,
                sistEndret = nå(clock),
            ).right()
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        GODKJENT,
        AUTOMATISK_BEHANDLET,
        AVBRUTT,
        -> throw IllegalStateException("Skal ha blitt fanget opp av kanOverta. Kan ikke overta meldekortbehandling med status $status")
    }
}

/**
 * Avgjør om [saksbehandler] kan overta meldekortbehandlingen.
 *
 * Betingelsene speiler hvilke tilstander [overta] faktisk håndterer:
 *  - [UNDER_BEHANDLING]: kan overtas av en annen saksbehandler enn den som er tildelt, gitt at behandlingen har en saksbehandler.
 *  - [UNDER_BESLUTNING]: kan overtas av en annen beslutter enn saksbehandleren, gitt at behandlingen har en beslutter.
 */
fun Meldekortbehandling.kanOverta(
    saksbehandler: Saksbehandler,
): Either<KunneIkkeOvertaMeldekortbehandling, Unit> {
    return when (status) {
        UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler()) {
                KunneIkkeOvertaMeldekortbehandling.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler == null) {
                KunneIkkeOvertaMeldekortbehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING -> {
            if (!saksbehandler.erBeslutter()) {
                KunneIkkeOvertaMeldekortbehandling.MåVæreBeslutter.left()
            } else if (this.beslutter == null) {
                KunneIkkeOvertaMeldekortbehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta.left()
            } else if (this.saksbehandler == saksbehandler.navIdent) {
                KunneIkkeOvertaMeldekortbehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BESLUTNING -> KunneIkkeOvertaMeldekortbehandling.BehandlingenMåVæreUnderBeslutningForÅOverta.left()

        GODKJENT -> KunneIkkeOvertaMeldekortbehandling.BehandlingenKanIkkeVæreGodkjentEllerIkkeRett.left()

        AUTOMATISK_BEHANDLET -> KunneIkkeOvertaMeldekortbehandling.KanIkkeOvertaAutomatiskBehandling.left()

        KLAR_TIL_BEHANDLING,
        AVBRUTT,
        -> KunneIkkeOvertaMeldekortbehandling.UgyldigStatus(status).left()
    }
}
