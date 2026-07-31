package no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
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
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.SettKlagebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVentOgNullstillSaksbehandler
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

fun Rammebehandling.settPåVent(
    kommando: SettRammebehandlingPåVentKommando,
    clock: Clock,
): Pair<Rammebehandling, Statistikkhendelser> {
    val endretAv = kommando.saksbehandler
    kanSettePåVent(endretAv).onLeft {
        it.kastVedManglendeRolle(endretAv)
        it.kast(this.id)
    }
    when (status) {
        UNDER_AUTOMATISK_BEHANDLING,
        UNDER_BEHANDLING,
        UNDER_BESLUTNING,
        -> {
            val oppdatertSaksbehandler = if (status == UNDER_AUTOMATISK_BEHANDLING || status == UNDER_BESLUTNING) {
                saksbehandler
            } else {
                null
            }

            val oppdatertStatus = when (status) {
                UNDER_BESLUTNING -> KLAR_TIL_BESLUTNING
                UNDER_BEHANDLING -> KLAR_TIL_BEHANDLING
                UNDER_AUTOMATISK_BEHANDLING -> UNDER_AUTOMATISK_BEHANDLING
                else -> throw IllegalStateException("Uventet status $status ved oppdatering til ventende status")
            }
            val nå = nå(clock)
            val oppdatertVentestatus = ventestatus.settPåVent(
                tidspunktSattPåVent = nå,
                endretAv = endretAv.navIdent,
                begrunnelse = kommando.begrunnelse,
                status = status.toString(),
                frist = kommando.frist,
            )
            val (oppdatertKlagebehandling, klagestatistikk) = oppdaterKlagebehandling(kommando, clock)
            val oppdatertRammebehandling = when (this) {
                is Søknadsbehandling -> {
                    this.copy(
                        ventestatus = oppdatertVentestatus,
                        saksbehandler = oppdatertSaksbehandler,
                        beslutter = null,
                        status = oppdatertStatus,
                        venterTil = kommando.venterTil,
                        sistEndret = nå,
                        klagebehandling = oppdatertKlagebehandling,
                    )
                }

                is Revurdering -> {
                    this.copy(
                        ventestatus = oppdatertVentestatus,
                        saksbehandler = oppdatertSaksbehandler,
                        beslutter = null,
                        status = oppdatertStatus,
                        venterTil = kommando.venterTil,
                        sistEndret = nå,
                        klagebehandling = oppdatertKlagebehandling,
                    )
                }
            }
            val statistikkhendelser = klagestatistikk.leggTil(
                oppdatertRammebehandling.genererSaksstatistikk(
                    hendelse = StatistikkhendelseType.BEHANDLING_SATT_PA_VENT,
                ),
            )
            return oppdatertRammebehandling to statistikkhendelser
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        -> throw IllegalStateException("Kan ikke sette behandling på vent som har status ${status.name}")
    }
}

private fun Rammebehandling.oppdaterKlagebehandling(
    kommando: SettRammebehandlingPåVentKommando,
    clock: Clock,
): Pair<Klagebehandling?, Statistikkhendelser> {
    val klage = klagebehandling ?: return (null to Statistikkhendelser.empty())
    return klage.settPåVentOgNullstillSaksbehandler(
        kommando = SettKlagebehandlingPåVentKommando(
            sakId = kommando.sakId,
            klagebehandlingId = klage.id,
            saksbehandler = kommando.saksbehandler,
            begrunnelse = kommando.begrunnelse,
            frist = kommando.frist,
        ),
        clock = clock,
        sjekkSaksbehandler = this.status != UNDER_BESLUTNING,
    ).getOrThrow()
}

/**
 * Avgjør om [saksbehandler] kan sette rammebehandlingen på vent.
 *
 * Betingelsene speiler hvilke tilstander [settPåVent] faktisk håndterer:
 *  - behandlingen kan ikke allerede være satt på vent
 *  - [UNDER_BEHANDLING]: kan settes på vent av saksbehandleren som er tildelt behandlingen
 *  - [UNDER_BESLUTNING]: kan settes på vent av beslutteren som er tildelt behandlingen
 *  - [UNDER_AUTOMATISK_BEHANDLING]: settes på vent av den automatiske saksbehandlingen, uten rollekrav
 */
fun Rammebehandling.kanSettePåVent(
    saksbehandler: Saksbehandler,
): Either<KanIkkeSetteRammebehandlingPåVent, Unit> {
    if (ventestatus.erSattPåVent) {
        return KanIkkeSetteRammebehandlingPåVent.BehandlingenErAlleredePåVent.left()
    }

    return when (status) {
        UNDER_AUTOMATISK_BEHANDLING -> Unit.right()

        UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler()) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandler.left()
            } else if (this.saksbehandler != saksbehandler.navIdent) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandlerForBehandlingen.left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING -> {
            if (!saksbehandler.erBeslutter()) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutter.left()
            } else if (this.beslutter != saksbehandler.navIdent) {
                KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutterForBehandlingen.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        -> KanIkkeSetteRammebehandlingPåVent.UgyldigStatus(status).left()
    }
}

/**
 * [settPåVent] returnerer ikke [Either], så feilene må kastes.
 * Meldingene holdes uendret slik at kallere som forholder seg til dem ikke påvirkes.
 */
private fun KanIkkeSetteRammebehandlingPåVent.kast(behandlingId: BehandlingId): Nothing {
    when (this) {
        KanIkkeSetteRammebehandlingPåVent.BehandlingenErAlleredePåVent ->
            throw IllegalArgumentException("Behandling med id $behandlingId er allerede satt på vent")

        KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandler,
        KanIkkeSetteRammebehandlingPåVent.MåVæreSaksbehandlerForBehandlingen,
        ->
            throw IllegalArgumentException("Du må være saksbehandler på behandlingen for å kunne sette den på vent.")

        KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutter,
        KanIkkeSetteRammebehandlingPåVent.MåVæreBeslutterForBehandlingen,
        ->
            throw IllegalArgumentException("Du må være beslutter på behandlingen for å kunne sette den på vent.")

        is KanIkkeSetteRammebehandlingPåVent.UgyldigStatus ->
            throw IllegalStateException("Kan ikke sette behandling på vent som har status ${status.name}")
    }
}
