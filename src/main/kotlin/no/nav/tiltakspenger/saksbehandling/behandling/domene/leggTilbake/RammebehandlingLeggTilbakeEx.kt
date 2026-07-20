package no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake

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
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.LeggTilbakeKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.leggTilbake
import no.nav.tiltakspenger.saksbehandling.klage.domene.tilTilknyttetBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Legger tilbake rammebehandlingen for [saksbehandler]:
 *  - [UNDER_BEHANDLING] -> nullstiller saksbehandler ([KLAR_TIL_BEHANDLING]).
 *  - [UNDER_BESLUTNING] -> nullstiller beslutter ([KLAR_TIL_BESLUTNING]).
 *
 * Legger også tilbake en eventuell tilknyttet klagebehandling.
 */
fun Rammebehandling.leggTilbakeRammebehandling(
    saksbehandler: Saksbehandler,
    clock: Clock,
): Either<KanIkkeLeggeTilbakeRammebehandling, Pair<Rammebehandling, Statistikkhendelser>> {
    kanLeggeTilbake(saksbehandler).onLeft { return it.left() }

    return when (status) {
        UNDER_BEHANDLING -> {
            val (oppdatertKlagebehandling, klagestatistikk) = klagebehandling?.leggTilbake(
                kommando = LeggTilbakeKlagebehandlingKommando(
                    sakId = sakId,
                    klagebehandlingId = klagebehandling!!.id,
                    saksbehandler = saksbehandler,
                ),
                tilknyttetBehandlingsstatus = status.tilTilknyttetBehandlingsstatus(),
                clock = clock,
            )?.getOrElse {
                return KanIkkeLeggeTilbakeRammebehandling.FeilVedKlagebehandling(it).left()
            } ?: (null to Statistikkhendelser.empty())
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    saksbehandler = null,
                    status = KLAR_TIL_BEHANDLING,
                    sistEndret = nå(clock),
                    klagebehandling = oppdatertKlagebehandling,
                )

                is Revurdering -> this.copy(
                    saksbehandler = null,
                    status = KLAR_TIL_BEHANDLING,
                    sistEndret = nå(clock),
                    klagebehandling = oppdatertKlagebehandling,
                )
            }
            val statistikkhendelser = klagestatistikk.leggTil(
                oppdatertRammebehandling.genererSaksstatistikk(
                    StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER,
                ),
            )
            (oppdatertRammebehandling to statistikkhendelser).right()
        }

        UNDER_BESLUTNING -> {
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    beslutter = null,
                    status = KLAR_TIL_BESLUTNING,
                    sistEndret = nå(clock),
                )

                is Revurdering -> this.copy(
                    beslutter = null,
                    status = KLAR_TIL_BESLUTNING,
                    sistEndret = nå(clock),
                )
            }
            val statistikkhendelser = Statistikkhendelser(
                oppdatertRammebehandling.genererSaksstatistikk(
                    StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER,
                ),
            )
            (oppdatertRammebehandling to statistikkhendelser).right()
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> throw IllegalStateException("Skal ha blitt fanget opp av kanLeggeTilbake. Kan ikke legge tilbake rammebehandling med status $status")
    }
}

/**
 * Avgjør om [saksbehandler] kan legge tilbake rammebehandlingen.
 *
 * Betingelsene speiler hvilke tilstander [leggTilbakeRammebehandling] faktisk håndterer:
 *  - [UNDER_BEHANDLING]: kan legges tilbake av saksbehandleren som er tildelt behandlingen.
 *  - [UNDER_BESLUTNING]: kan legges tilbake av beslutteren som er tildelt behandlingen.
 */
fun Rammebehandling.kanLeggeTilbake(
    saksbehandler: Saksbehandler,
): Either<KanIkkeLeggeTilbakeRammebehandling, Unit> {
    return when (status) {
        UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler()) {
                KanIkkeLeggeTilbakeRammebehandling.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler != saksbehandler.navIdent) {
                KanIkkeLeggeTilbakeRammebehandling.MåVæreSaksbehandlerForBehandlingen.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING -> {
            if (!saksbehandler.erBeslutter()) {
                KanIkkeLeggeTilbakeRammebehandling.MåVæreBeslutter.left()
            } else if (this.beslutter != saksbehandler.navIdent) {
                KanIkkeLeggeTilbakeRammebehandling.MåVæreBeslutterForBehandlingen.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> KanIkkeLeggeTilbakeRammebehandling.UgyldigStatus(status).left()
    }
}
