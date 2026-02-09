package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereSaksopplysninger
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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.GjenopptaKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.gjenopptaKlagebehandling
import java.time.Clock

/**
 * Kan kun gjenoppta en behandling som er satt på vent.
 * @param hentSaksopplysninger Henter saksopplysninger på nytt dersom denne ikke er null. Merk at det vi ikke henter saksopplysninger på nytt hvis den er sendt til beslutning.
 */
suspend fun Rammebehandling.gjenoppta(
    kommando: GjenopptaRammebehandlingKommando,
    clock: Clock,
    hentSaksopplysninger: (suspend () -> Saksopplysninger)?,
): Either<KunneIkkeOppdatereSaksopplysninger, Rammebehandling> {
    require(ventestatus.erSattPåVent) { "Behandlingen er ikke satt på vent" }

    return when (status) {
        VEDTATT, AVBRUTT -> throw IllegalStateException("Kan ikke gjenoppta behandling som har status ${status.name}")

        KLAR_TIL_BEHANDLING, UNDER_BEHANDLING -> {
            krevSaksbehandlerRolle(kommando.saksbehandler)
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
                krevSaksbehandlerRolle(kommando.saksbehandler)
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
            krevBeslutterRolle(kommando.saksbehandler)
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
 * @param hentSaksopplysninger Henter saksopplysninger på nytt dersom denne ikke er null.
 */
private suspend fun Rammebehandling.gjenopptaBehandling(
    kommando: GjenopptaRammebehandlingKommando,
    oppdatertSaksbehandler: String?,
    oppdatertBeslutter: String?,
    oppdatertStatus: Rammebehandlingsstatus,
    clock: Clock,
    hentSaksopplysninger: (suspend () -> Saksopplysninger)?,
): Either<KunneIkkeOppdatereSaksopplysninger, Rammebehandling> {
    val nå = nå(clock)
    val oppdatertVentestatus = ventestatus.gjenoppta(
        tidspunkt = nå,
        endretAv = kommando.saksbehandler.navIdent,
        status = status.toString(),
    )
    val oppdatertKlagebehandling = klagebehandling?.gjenopptaKlagebehandling(
        kommando = GjenopptaKlagebehandlingKommando(
            sakId = this.sakId,
            klagebehandlingId = klagebehandling!!.id,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ),
        clock = clock,
    )?.getOrThrow()
    return when (this) {
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
    }.let {
        if (hentSaksopplysninger != null) {
            it.oppdaterSaksopplysninger(kommando.saksbehandler, hentSaksopplysninger())
        } else {
            it.right()
        }
    }
}
