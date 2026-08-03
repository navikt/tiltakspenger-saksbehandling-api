package no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import java.time.Clock

/**
 * Sender rammebehandlingen til beslutning.
 * Forutsetningene håndheves av [kanSendeTilBeslutning], og feilene derfra returneres som venstre-verdi.
 */
fun Rammebehandling.tilBeslutning(
    kommando: SendBehandlingTilBeslutningKommando,
    clock: Clock,
): Either<KanIkkeSendeRammebehandlingTilBeslutter, Rammebehandling> {
    kanSendeTilBeslutning(kommando.saksbehandler).onLeft { return it.left() }

    val status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING
    val sendtTilBeslutning = nå(clock)

    return when (this) {
        is Revurdering -> this.copy(
            status = status,
            sendtTilBeslutning = sendtTilBeslutning,
            sistEndret = sendtTilBeslutning,
        )

        is Søknadsbehandling -> this.copy(
            status = status,
            sendtTilBeslutning = sendtTilBeslutning,
            sistEndret = sendtTilBeslutning,
        )
    }.right()
}

/**
 * Avgjør om [saksbehandler] kan sende rammebehandlingen til beslutning.
 *
 * Betingelsene speiler hvilke tilstander [tilBeslutning] faktisk håndterer:
 *  - behandlingen kan ikke eies av en annen saksbehandler
 *  - behandlingen må være [UNDER_BEHANDLING] eller [UNDER_AUTOMATISK_BEHANDLING]
 *  - behandlingen kan ikke stå på vent
 */
fun Rammebehandling.kanSendeTilBeslutning(saksbehandler: Saksbehandler): Either<KanIkkeSendeRammebehandlingTilBeslutter, Unit> {
    if (this.saksbehandler != null && this.saksbehandler != saksbehandler.navIdent) {
        return KanIkkeSendeRammebehandlingTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(this.saksbehandler!!)
            .left()
    }
    if (status != UNDER_BEHANDLING && status != UNDER_AUTOMATISK_BEHANDLING) {
        return KanIkkeSendeRammebehandlingTilBeslutter.MåVæreUnderBehandlingEllerAutomatisk.left()
    }
    if (ventestatus.erSattPåVent) {
        return KanIkkeSendeRammebehandlingTilBeslutter.ErPaVent.left()
    }

    return Unit.right()
}
