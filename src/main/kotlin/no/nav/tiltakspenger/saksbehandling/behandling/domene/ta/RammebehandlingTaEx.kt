package no.nav.tiltakspenger.saksbehandling.behandling.domene.ta

import arrow.core.Either
import arrow.core.getOrElse
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
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.TaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.ta.ta
import no.nav.tiltakspenger.saksbehandling.klage.domene.tilTilknyttetBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Saksbehandler/beslutter tar behandlingen.
 * Forutsetningene håndheves av [kanTaBehandling], og feilene derfra returneres som venstre-verdi.
 */
fun Rammebehandling.taBehandling(
    saksbehandler: Saksbehandler,
    clock: Clock,
): Either<KunneIkkeTaBehandling, Pair<Rammebehandling, Statistikkhendelser>> {
    kanTaBehandling(saksbehandler).onLeft { return it.left() }

    val nå = nå(clock)
    return when (status) {
        KLAR_TIL_BEHANDLING -> {
            val (oppdatertKlagebehandling, klagestatistikk) = klagebehandling?.ta(
                kommando = TaKlagebehandlingKommando(sakId, klagebehandling!!.id, saksbehandler),
                tilknyttetBehandlingsstatus = this.status.tilTilknyttetBehandlingsstatus(),
                sistEndret = nå,
            )?.getOrElse {
                return KunneIkkeTaBehandling.FeilVedKlagebehandling(it).left()
            } ?: (null to Statistikkhendelser.empty())
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    saksbehandler = saksbehandler.navIdent,
                    beslutter = if (saksbehandler.navIdent == beslutter) null else beslutter,
                    status = UNDER_BEHANDLING,
                    sistEndret = nå,
                    klagebehandling = oppdatertKlagebehandling,
                )

                is Revurdering -> this.copy(
                    saksbehandler = saksbehandler.navIdent,
                    beslutter = if (saksbehandler.navIdent == beslutter) null else beslutter,
                    status = UNDER_BEHANDLING,
                    sistEndret = nå,
                    klagebehandling = oppdatertKlagebehandling,
                )
            }
            val statistikkhendelser = klagestatistikk.leggTil(
                oppdatertRammebehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
            )
            Pair(oppdatertRammebehandling, statistikkhendelser).right()
        }

        KLAR_TIL_BESLUTNING -> {
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    beslutter = saksbehandler.navIdent,
                    status = UNDER_BESLUTNING,
                    sistEndret = nå,
                )

                is Revurdering -> this.copy(
                    beslutter = saksbehandler.navIdent,
                    status = UNDER_BESLUTNING,
                    sistEndret = nå,
                )
            }
            val statistikkhendelser = Statistikkhendelser(
                oppdatertRammebehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
            )
            Pair(oppdatertRammebehandling, statistikkhendelser).right()
        }

        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> throw IllegalStateException("Skal ha blitt fanget opp av kanTaBehandling. Kan ikke ta rammebehandling med status $status")
    }
}

/**
 * Avgjør om [saksbehandler] kan ta behandlingen.
 *
 * Betingelsene speiler hvilke tilstander [taBehandling] faktisk håndterer:
 *  - [KLAR_TIL_BEHANDLING]: kan tas av en saksbehandler dersom behandlingen ikke allerede har en saksbehandler.
 *  - [KLAR_TIL_BESLUTNING]: kan tas av en beslutter (som ikke er saksbehandleren) dersom behandlingen ikke allerede har en beslutter.
 */
fun Rammebehandling.kanTaBehandling(saksbehandler: Saksbehandler): Either<KunneIkkeTaBehandling, Unit> {
    return when (status) {
        KLAR_TIL_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler()) {
                KunneIkkeTaBehandling.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler != null) {
                KunneIkkeTaBehandling.BehandlingenHarEksisterendeSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BESLUTNING -> {
            if (saksbehandler.navIdent == this.saksbehandler) {
                KunneIkkeTaBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSammePåBehandling.left()
            } else if (!saksbehandler.erBeslutter()) {
                KunneIkkeTaBehandling.MåVæreBeslutter.left()
            } else if (this.beslutter != null) {
                KunneIkkeTaBehandling.BehandlingenHarEksisterendeBeslutter.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BEHANDLING -> KunneIkkeTaBehandling.BehandlingenHarEksisterendeSaksbehandler.left()

        UNDER_BESLUTNING -> KunneIkkeTaBehandling.BehandlingenHarEksisterendeBeslutter.left()

        VEDTATT, AVBRUTT, UNDER_AUTOMATISK_BEHANDLING -> KunneIkkeTaBehandling.BehandlingenErIEnTilstandSomIkkeTillaterÅTaBehandling(status).left()
    }
}
