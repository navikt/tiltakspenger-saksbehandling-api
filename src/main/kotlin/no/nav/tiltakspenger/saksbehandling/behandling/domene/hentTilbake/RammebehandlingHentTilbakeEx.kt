package no.nav.tiltakspenger.saksbehandling.behandling.domene.hentTilbake

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
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Saksbehandleren tar over en automatisk behandlet søknadsbehandling som er klar til beslutning: [KLAR_TIL_BESLUTNING] -> [UNDER_BEHANDLING].
 * Ta og overta kan ikke brukes, fordi de krever beslutterrollen når behandlingen er klar til beslutning.
 * Underkjenn kan heller ikke brukes, fordi det er en beslutterhandling.
 * Som ved underkjenning av en automatisk behandling nullstilles resultatet og flagget for automatisk saksbehandling, slik at saksbehandleren tar stilling på nytt.
 * Krever at [utøvendeSaksbehandler] har rollen saksbehandler, og kaster [no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException] ellers.
 * Forutsetningene håndheves av [kanHenteTilbake], og feilene derfra returneres som venstre-verdi.
 */
fun Rammebehandling.hentTilbakeRammebehandling(
    utøvendeSaksbehandler: Saksbehandler,
    clock: Clock,
): Either<KanIkkeHenteTilbakeRammebehandling, Pair<Rammebehandling, Statistikkhendelser>> {
    krevSaksbehandlerRolle(utøvendeSaksbehandler)
    kanHenteTilbake(utøvendeSaksbehandler).onLeft { return it.left() }

    val nå = nå(clock)
    return when (status) {
        KLAR_TIL_BESLUTNING -> {
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    status = UNDER_BEHANDLING,
                    saksbehandler = utøvendeSaksbehandler.navIdent,
                    sistEndret = nå,
                    sendtTilBeslutning = null,
                    automatiskSaksbehandlet = false,
                    resultat = null,
                )

                is Revurdering -> throw IllegalStateException("Skal ha blitt fanget opp av kanHenteTilbake. Kan ikke hente tilbake rammebehandling med type $behandlingstype")
            }
            val statistikkhendelser = Statistikkhendelser(
                oppdatertRammebehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
            )
            Pair(oppdatertRammebehandling, statistikkhendelser).right()
        }

        UNDER_AUTOMATISK_BEHANDLING,
        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        -> throw IllegalStateException("Skal ha blitt fanget opp av kanHenteTilbake. Kan ikke hente tilbake rammebehandling med status $status")
    }
}

/**
 * Avgjør om [utøvendeSaksbehandler] kan hente tilbake behandlingen.
 * Kun søknadsbehandlinger som er [KLAR_TIL_BESLUTNING] og tildelt [AUTOMATISK_SAKSBEHANDLER_ID] kan hentes tilbake, og bare av en med saksbehandlerrollen.
 * Behandlingen kan ikke hentes tilbake mens den er satt på vent, fordi det betyr at en beslutter allerede har tatt den.
 * Kalles også fra DTO-mappingen for å avgjøre om kommandoen skal tilbys, og må derfor ikke kaste.
 */
fun Rammebehandling.kanHenteTilbake(
    utøvendeSaksbehandler: Saksbehandler,
): Either<KanIkkeHenteTilbakeRammebehandling, Unit> {
    return when (status) {
        KLAR_TIL_BESLUTNING -> {
            if (!utøvendeSaksbehandler.erSaksbehandler) {
                KanIkkeHenteTilbakeRammebehandling.MåVæreSaksbehandler.left()
            } else if (this !is Søknadsbehandling) {
                KanIkkeHenteTilbakeRammebehandling.MåVæreSøknadsbehandling.left()
            } else if (this.saksbehandler != AUTOMATISK_SAKSBEHANDLER_ID) {
                KanIkkeHenteTilbakeRammebehandling.ErIkkeAutomatiskBehandlet.left()
            } else if (ventestatus.erSattPåVent) {
                KanIkkeHenteTilbakeRammebehandling.BehandlingenErSattPåVent.left()
            } else {
                Unit.right()
            }
        }

        UNDER_AUTOMATISK_BEHANDLING,
        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        -> KanIkkeHenteTilbakeRammebehandling.UgyldigStatus(status).left()
    }
}
