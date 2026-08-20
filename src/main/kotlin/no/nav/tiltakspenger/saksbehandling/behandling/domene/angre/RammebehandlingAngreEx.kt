package no.nav.tiltakspenger.saksbehandling.behandling.domene.angre

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Angrer sendingen til beslutning: [KLAR_TIL_BESLUTNING] -> [UNDER_BEHANDLING].
 * Saksbehandleren som sendte behandlingen beholder tildelingen.
 * Forutsetningene håndheves av [kanAngreBehandling], og feilene derfra returneres som venstre-verdi.
 */
fun Rammebehandling.angreBehandling(
    saksbehandler: Saksbehandler,
    clock: Clock,
): Either<KunneIkkeAngreBehandling, Pair<Rammebehandling, Statistikkhendelser>> {
    kanAngreBehandling(saksbehandler).onLeft { return it.left() }

    val nå = nå(clock)
    return when (status) {
        KLAR_TIL_BESLUTNING -> {
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    status = UNDER_BEHANDLING,
                    sistEndret = nå,
                )

                is Revurdering -> this.copy(
                    status = UNDER_BEHANDLING,
                    sistEndret = nå,
                )
            }
            val statistikkhendelser = Statistikkhendelser(
                oppdatertRammebehandling.genererSaksstatistikk(StatistikkhendelseType.SAKSBEHANDLER_ANGRER),
            )
            Pair(oppdatertRammebehandling, statistikkhendelser).right()
        }

        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> throw IllegalStateException("Skal ha blitt fanget opp av kanAngreBehandling. Kan ikke angre rammebehandling med status $status")
    }
}

/**
 * Avgjør om [saksbehandler] kan angre sendingen til beslutning.
 * Kun saksbehandleren som er tildelt behandlingen kan angre, og bare mens behandlingen er [KLAR_TIL_BESLUTNING].
 */
fun Rammebehandling.kanAngreBehandling(saksbehandler: Saksbehandler): Either<KunneIkkeAngreBehandling, Unit> {
    return when (status) {
        KLAR_TIL_BESLUTNING -> {
            if (saksbehandler.navIdent != this.saksbehandler) {
                KunneIkkeAngreBehandling.MåVæreSaksbehandlerForBehandlingen.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, UNDER_BESLUTNING, VEDTATT, AVBRUTT, UNDER_AUTOMATISK_BEHANDLING -> KunneIkkeAngreBehandling.BehandlingenErIEnTilstandSomIkkeTillaterÅAngre(
            status,
        ).left()
    }
}
