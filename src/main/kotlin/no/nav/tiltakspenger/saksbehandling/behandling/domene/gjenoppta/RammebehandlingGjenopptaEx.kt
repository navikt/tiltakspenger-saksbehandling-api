package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.GjenopptaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.gjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Kan kun gjenoppta en behandling som er satt på vent.
 * Forutsetningene håndheves av [kanGjenoppta], og feilene derfra returneres som venstre-verdi.
 * @param hentSaksopplysninger Henter saksopplysninger på nytt dersom denne ikke er null.
 * Merk at det vi ikke henter saksopplysninger på nytt hvis den er sendt til beslutning.
 */
suspend fun Rammebehandling.gjenoppta(
    kommando: GjenopptaRammebehandlingKommando,
    clock: Clock,
    hentSaksopplysninger: (suspend () -> Saksopplysninger)?,
): Either<KanIkkeGjenopptaRammebehandling, Pair<Rammebehandling, Statistikkhendelser>> {
    kanGjenoppta(kommando.saksbehandler).onLeft { return it.left() }

    return when (status) {
        VEDTATT, AVBRUTT -> KanIkkeGjenopptaRammebehandling.UgyldigStatus(status).left()

        KLAR_TIL_BEHANDLING, UNDER_BEHANDLING -> {
            gjenopptaBehandling(
                kommando = kommando,
                oppdatertSaksbehandler = kommando.saksbehandler.navIdent,
                // Dersom den er underkjent ønsker vi ikke å fjerne beslutter.
                oppdatertBeslutter = beslutter,
                oppdatertStatus = UNDER_BEHANDLING,
                clock = clock,
                hentSaksopplysninger = hentSaksopplysninger,
            )
        }

        UNDER_AUTOMATISK_BEHANDLING -> {
            if (kommando.saksbehandler == AUTOMATISK_SAKSBEHANDLER) {
                // Dette betyr at det er den automatiske jobben som gjenopptar behandlingen.
                gjenopptaBehandling(
                    kommando = kommando,
                    oppdatertSaksbehandler = AUTOMATISK_SAKSBEHANDLER.navIdent,
                    oppdatertBeslutter = null,
                    oppdatertStatus = UNDER_AUTOMATISK_BEHANDLING,
                    clock = clock,
                    hentSaksopplysninger = hentSaksopplysninger,
                )
            } else {
                // En saksbehandler har tar over behandlingen fra den automatiske jobben.
                gjenopptaBehandling(
                    kommando = kommando,
                    oppdatertSaksbehandler = kommando.saksbehandler.navIdent,
                    oppdatertBeslutter = null,
                    oppdatertStatus = UNDER_BEHANDLING,
                    clock = clock,
                    hentSaksopplysninger = hentSaksopplysninger,
                )
            }
        }

        KLAR_TIL_BESLUTNING, UNDER_BESLUTNING -> {
            gjenopptaBehandling(
                kommando = kommando,
                oppdatertSaksbehandler = saksbehandler,
                oppdatertBeslutter = kommando.saksbehandler.navIdent,
                oppdatertStatus = UNDER_BESLUTNING,
                clock = clock,
                hentSaksopplysninger = null,
            )
        }
    }
}

/**
 * Kalles kun fra [gjenoppta], som allerede har verifisert forutsetningene via [kanGjenoppta].
 * @param hentSaksopplysninger Henter saksopplysninger på nytt dersom denne ikke er null.
 */
private suspend fun Rammebehandling.gjenopptaBehandling(
    kommando: GjenopptaRammebehandlingKommando,
    oppdatertSaksbehandler: String?,
    oppdatertBeslutter: String?,
    oppdatertStatus: Rammebehandlingsstatus,
    clock: Clock,
    hentSaksopplysninger: (suspend () -> Saksopplysninger)?,
): Either<KanIkkeGjenopptaRammebehandling, Pair<Rammebehandling, Statistikkhendelser>> {
    val nå = nå(clock)
    val oppdatertVentestatus = ventestatus.gjenoppta(
        tidspunkt = nå,
        endretAv = kommando.saksbehandler.navIdent,
        status = status.toString(),
    )
    val (oppdatertKlagebehandling, klagestatistikk) = klagebehandling?.gjenopptaKlagebehandling(
        kommando = GjenopptaKlagebehandlingKommando(
            sakId = this.sakId,
            klagebehandlingId = klagebehandling!!.id,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ),
        clock = clock,
    )?.getOrElse {
        return KanIkkeGjenopptaRammebehandling.KunneIkkeGjenopptaKlagebehandlingen(it).left()
    } ?: (null to Statistikkhendelser.empty())
    val oppdatertRammebehandling = when (this) {
        is Søknadsbehandling -> this.copy(
            ventestatus = oppdatertVentestatus,
            venterTil = null,
            sistEndret = nå,
            klagebehandling = oppdatertKlagebehandling,
            saksbehandler = oppdatertSaksbehandler,
            beslutter = oppdatertBeslutter,
            status = oppdatertStatus,
        )

        is Revurdering -> this.copy(
            ventestatus = oppdatertVentestatus,
            venterTil = null,
            sistEndret = nå,
            klagebehandling = oppdatertKlagebehandling,
            saksbehandler = oppdatertSaksbehandler,
            beslutter = oppdatertBeslutter,
            status = oppdatertStatus,
        )
    }
    return if (hentSaksopplysninger != null) {
        oppdatertRammebehandling.oppdaterSaksopplysninger(
            saksbehandler = kommando.saksbehandler,
            nyeSaksopplysninger = hentSaksopplysninger(),
            clock = clock,
        ).mapLeft { KanIkkeGjenopptaRammebehandling.KunneIkkeOppdatereSaksopplysningene(it) }
    } else {
        oppdatertRammebehandling.right()
    }.map {
        val statistikkhendelser = klagestatistikk.leggTil(
            it.genererSaksstatistikk(
                StatistikkhendelseType.BEHANDLING_GJENOPPTATT,
            ),
        )
        it to statistikkhendelser
    }
}

/**
 * Avgjør om [saksbehandler] kan gjenoppta rammebehandlingen.
 *
 * Betingelsene speiler hvilke tilstander [gjenoppta] faktisk håndterer:
 *  - behandlingen må være satt på vent
 *  - [KLAR_TIL_BEHANDLING]/[UNDER_BEHANDLING]: krever saksbehandlerrolle
 *  - [KLAR_TIL_BESLUTNING]/[UNDER_BESLUTNING]: krever beslutterrolle
 *  - [UNDER_AUTOMATISK_BEHANDLING]: gjenopptas enten av den automatiske saksbehandlingen, eller av en saksbehandler som overtar
 */
fun Rammebehandling.kanGjenoppta(
    saksbehandler: Saksbehandler,
): Either<KanIkkeGjenopptaRammebehandling, Unit> {
    if (!ventestatus.erSattPåVent) {
        return KanIkkeGjenopptaRammebehandling.BehandlingenErIkkePåVent.left()
    }

    return when (status) {
        KLAR_TIL_BEHANDLING, UNDER_BEHANDLING -> {
            if (!saksbehandler.erSaksbehandler) {
                KanIkkeGjenopptaRammebehandling.MåVæreSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        UNDER_AUTOMATISK_BEHANDLING -> {
            if (saksbehandler == AUTOMATISK_SAKSBEHANDLER) {
                Unit.right()
            } else if (!saksbehandler.erSaksbehandler) {
                KanIkkeGjenopptaRammebehandling.MåVæreSaksbehandler.left()
            } else {
                Unit.right()
            }
        }

        KLAR_TIL_BESLUTNING, UNDER_BESLUTNING -> {
            if (!saksbehandler.erBeslutter) {
                KanIkkeGjenopptaRammebehandling.MåVæreBeslutter.left()
            } else {
                Unit.right()
            }
        }

        VEDTATT, AVBRUTT -> KanIkkeGjenopptaRammebehandling.UgyldigStatus(status).left()
    }
}
