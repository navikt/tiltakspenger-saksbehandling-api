package no.nav.tiltakspenger.saksbehandling.behandling.domene.underkjenn

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
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Underkjenner rammebehandlingen og sender den tilbake til saksbehandler.
 * Forutsetningene håndheves av [krevKanUnderkjenne], som kaster dersom de ikke er oppfylt.
 *
 * Hvis saken har blitt behandlet automatisk fjernes automatisk saksbehandler og flagget som sier at den har blitt behandlet automatisk ved underkjenning.
 */
fun Rammebehandling.underkjenn(
    utøvendeBeslutter: Saksbehandler,
    attestering: Attestering,
    clock: Clock,
): Pair<Rammebehandling, Statistikkhendelser> {
    krevKanUnderkjenne(utøvendeBeslutter)

    val attesteringer = attesteringer.leggTil(attestering)

    val oppdatertRammebehandling = when (this) {
        is Søknadsbehandling -> this.copy(
            status = if (automatiskSaksbehandlet) {
                KLAR_TIL_BEHANDLING
            } else {
                UNDER_BEHANDLING
            },
            attesteringer = attesteringer,
            saksbehandler = if (automatiskSaksbehandlet) {
                null
            } else {
                saksbehandler
            },
            automatiskSaksbehandlet = false,
            sistEndret = nå(clock),
            resultat = if (automatiskSaksbehandlet) null else resultat,
        )

        is Revurdering -> this.copy(
            status = UNDER_BEHANDLING,
            attesteringer = attesteringer,
            sistEndret = nå(clock),
        )
    }

    // Genererer ikke statistikk for klage, fordi underkjennelse av rammebehandlingen underkjenner ikke klagebehandlingen.
    val statistikkhendelser = Statistikkhendelser(
        oppdatertRammebehandling.genererSaksstatistikk(StatistikkhendelseType.UNDERKJENT_BEHANDLING),
    )
    return oppdatertRammebehandling to statistikkhendelser
}

/**
 * Krever at [utøvendeBeslutter] kan underkjenne behandlingen, og kaster ellers.
 *
 * Betingelsene speiler hvilke tilstander [underkjenn] faktisk håndterer:
 *  - behandlingen må være [UNDER_BESLUTNING]
 *  - [utøvendeBeslutter] må ha beslutterrollen og være beslutteren på behandlingen
 *  - behandlingen kan ikke allerede være godkjent
 *  - behandlingen kan ikke stå på vent
 *
 * Kaster i stedet for å returnere en venstre-verdi, fordi tilstandene her ikke er noe en saksbehandler kan treffe fra saksbehandlingsflyten.
 */
private fun Rammebehandling.krevKanUnderkjenne(utøvendeBeslutter: Saksbehandler) {
    when (status) {
        UNDER_BESLUTNING -> Unit

        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> throw IllegalStateException(
            "Må ha status UNDER_BESLUTNING for å sende tilbake. Behandlingsstatus: $status",
        )
    }

    krevBeslutterRolle(utøvendeBeslutter)
    check(this.beslutter == utøvendeBeslutter.navIdent) {
        "Kun beslutter som har saken kan sende tilbake"
    }
    check(!this.attesteringer.any { it.isGodkjent() }) {
        "Behandlingen er allerede godkjent"
    }
    check(!ventestatus.erSattPåVent) { "Behandlingen må gjenopptas før den kan underkjennes." }
}
