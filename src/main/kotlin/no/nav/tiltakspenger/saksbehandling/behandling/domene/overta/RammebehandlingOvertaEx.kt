package no.nav.tiltakspenger.saksbehandling.behandling.domene.overta

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
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
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.OvertaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.overta.overta
import no.nav.tiltakspenger.saksbehandling.klage.domene.tilTilknyttetBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Saksbehandler/beslutter overtar behandlingen.
 * Forutsetningene håndheves av [kanOverta], og feilene derfra returneres som venstre-verdi.
 * I tillegg har vi en race-guard på at behandlingen ikke er endret det siste minuttet.
 */
fun Rammebehandling.overta(
    saksbehandler: Saksbehandler,
    correlationId: CorrelationId,
    clock: Clock,
): Either<KunneIkkeOvertaBehandling, Pair<Rammebehandling, Statistikkhendelser>> {
    val nå = nå(clock)
    val nåMinusEttMinutt = nå.minusMinutes(1)
    val erSistEndretMindreEnn1MinuttSiden = this.sistEndret.isAfter(nåMinusEttMinutt)

    if (erSistEndretMindreEnn1MinuttSiden) {
        return KunneIkkeOvertaBehandling.BehandlingenErUnderAktivBehandling.left()
    }

    kanOverta(saksbehandler).onLeft { return it.left() }

    return when (status) {
        UNDER_BEHANDLING -> {
            // dersom det er beslutteren som overtar behandlingen, skal dem nulles ut som beslutter
            val beslutter = if (this.beslutter == saksbehandler.navIdent) null else this.beslutter

            val (oppdatertKlagebehandling, klagestatistikk) = klagebehandling?.overta(
                kommando = OvertaKlagebehandlingKommando(
                    sakId = sakId,
                    klagebehandlingId = klagebehandling!!.id,
                    overtarFra = this.saksbehandler!!,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                ),
                tilknyttetBehandlingsstatus = this.status.tilTilknyttetBehandlingsstatus(),
                clock = clock,
            )?.getOrElse {
                return KunneIkkeOvertaBehandling.KanIkkeOvertaKlagebehandling(it).left()
            } ?: (null to Statistikkhendelser.empty())
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    saksbehandler = saksbehandler.navIdent,
                    beslutter = beslutter,
                    sistEndret = nå,
                    klagebehandling = oppdatertKlagebehandling,
                )

                is Revurdering -> this.copy(
                    saksbehandler = saksbehandler.navIdent,
                    beslutter = beslutter,
                    sistEndret = nå,
                    klagebehandling = oppdatertKlagebehandling,
                )
            }
            val statistikkhendelser = klagestatistikk.leggTil(
                oppdatertRammebehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
            )
            (oppdatertRammebehandling to statistikkhendelser).right()
        }

        UNDER_BESLUTNING -> {
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    beslutter = saksbehandler.navIdent,
                    sistEndret = nå,
                )

                is Revurdering -> this.copy(
                    beslutter = saksbehandler.navIdent,
                    sistEndret = nå,
                )
            }
            val statistikkhendelser = Statistikkhendelser(
                oppdatertRammebehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
            )
            (oppdatertRammebehandling to statistikkhendelser).right()
        }

        UNDER_AUTOMATISK_BEHANDLING -> {
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> this.copy(
                    status = UNDER_BEHANDLING,
                    saksbehandler = saksbehandler.navIdent,
                    sistEndret = nå,
                )

                is Revurdering -> throw IllegalStateException("Skal ha blitt fanget opp av kanOverta. Kan ikke overta revurdering under automatisk behandling")
            }
            val statistikkhendelser = Statistikkhendelser(
                oppdatertRammebehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
            )
            (oppdatertRammebehandling to statistikkhendelser).right()
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        -> throw IllegalStateException("Skal ha blitt fanget opp av kanOverta. Kan ikke overta rammebehandling med status $status")
    }
}

/**
 * Avgjør om [saksbehandler] kan overta behandlingen.
 *
 * Betingelsene speiler hvilke tilstander [overta] faktisk håndterer:
 *  - [UNDER_BEHANDLING]: kan overtas av en saksbehandler, gitt at behandlingen har en saksbehandler.
 *  - [UNDER_BESLUTNING]: kan overtas av en beslutter som ikke er saksbehandleren, gitt at behandlingen har en beslutter.
 *  - [UNDER_AUTOMATISK_BEHANDLING]: kan overtas av en saksbehandler dersom den automatiske behandlingen står på vent.
 *
 * Merk at [overta] i tillegg har en race-guard på at behandlingen ikke er endret det siste minuttet.
 * Den er bevisst utelatt her, fordi den er en tidsavhengig kollisjonssjekk og ikke en regel om hvem som har lov til å overta.
 */
fun Rammebehandling.kanOverta(saksbehandler: Saksbehandler): Either<KunneIkkeOvertaBehandling, Unit> {
    return when (status) {
        UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler()) {
                KunneIkkeOvertaBehandling.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler == null) {
                KunneIkkeOvertaBehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING -> {
            if (!saksbehandler.erBeslutter()) {
                KunneIkkeOvertaBehandling.MåVæreBeslutter.left()
            } else if (this.beslutter == null) {
                KunneIkkeOvertaBehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta.left()
            } else if (this.saksbehandler == saksbehandler.navIdent) {
                KunneIkkeOvertaBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme.left()
            } else {
                Unit.right()
            }
        }

        UNDER_AUTOMATISK_BEHANDLING -> {
            if (this.saksbehandler != AUTOMATISK_SAKSBEHANDLER_ID || !ventestatus.erSattPåVent) {
                KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreUnderAutomatiskBehandling.left()
            } else if (this is Revurdering) {
                KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreUnderAutomatiskBehandling.left()
            } else if (!saksbehandler.erSaksbehandler()) {
                KunneIkkeOvertaBehandling.MåVæreSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BEHANDLING -> KunneIkkeOvertaBehandling.BehandlingenMåVæreUnderBehandlingForÅOverta.left()

        KLAR_TIL_BESLUTNING -> KunneIkkeOvertaBehandling.BehandlingenMåVæreUnderBeslutningForÅOverta.left()

        VEDTATT,
        AVBRUTT,
        -> KunneIkkeOvertaBehandling.BehandlingenKanIkkeVæreVedtattEllerAvbrutt.left()
    }
}
