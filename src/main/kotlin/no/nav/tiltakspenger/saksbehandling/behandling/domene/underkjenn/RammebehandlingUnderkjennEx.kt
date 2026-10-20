package no.nav.tiltakspenger.saksbehandling.behandling.domene.underkjenn

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
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Underkjenner rammebehandlingen og sender den tilbake til saksbehandler.
 * Forutsetningene håndheves av [kanUnderkjenne], og feilene derfra returneres som venstre-verdi.
 *
 * Hvis saken er delautomatisk innstilt til innvilgelse og den som underkjenner har saksbehandlerrollen, tildeles hen saken som saksbehandler, og beslutteren fjernes.
 * Mangler hen saksbehandlerrollen, blir saken klar til behandling uten saksbehandler.
 * Resultatet og flagget som sier at den har blitt behandlet automatisk nullstilles, slik at saksbehandler tar stilling på nytt.
 */
fun Rammebehandling.underkjenn(
    utøvendeBeslutter: Saksbehandler,
    attestering: Attestering,
    clock: Clock,
): Either<KanIkkeUnderkjenne, Pair<Rammebehandling, Statistikkhendelser>> {
    kanUnderkjenne(utøvendeBeslutter).onLeft { return it.left() }

    val attesteringer = attesteringer.leggTil(attestering)

    val oppdatertRammebehandling = when (this) {
        is Søknadsbehandling -> {
            // En delautomatisk innstilling har ingen menneskelig saksbehandler å sende saken tilbake til, så den som underkjenner tildeles saken.
            // Det krever saksbehandlerrollen, ellers blir saken klar til behandling uten saksbehandler.
            val tildelUnderkjenner = automatiskSaksbehandlet && utøvendeBeslutter.erSaksbehandler
            this.copy(
                status = if (automatiskSaksbehandlet && !tildelUnderkjenner) KLAR_TIL_BEHANDLING else UNDER_BEHANDLING,
                attesteringer = attesteringer,
                saksbehandler = when {
                    tildelUnderkjenner -> utøvendeBeslutter.navIdent
                    automatiskSaksbehandlet -> null
                    else -> saksbehandler
                },
                // Den som underkjente blir saksbehandler, og kan ikke samtidig være beslutter.
                beslutter = if (tildelUnderkjenner) null else beslutter,
                automatiskSaksbehandlet = false,
                sistEndret = nå(clock),
                resultat = if (automatiskSaksbehandlet) null else resultat,
            )
        }

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
    return (oppdatertRammebehandling to statistikkhendelser).right()
}

/**
 * Avgjør om [utøvendeBeslutter] kan underkjenne behandlingen.
 * Krever at [utøvendeBeslutter] kan opptre som beslutter, se [Rammebehandling.kanTaOgUnderkjenneSomBeslutter].
 * Kaster [no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException] ellers.
 *
 * Betingelsene speiler hvilke tilstander [underkjenn] faktisk håndterer, og sjekkes i denne rekkefølgen:
 *  - behandlingen må være [UNDER_BESLUTNING]
 *  - [utøvendeBeslutter] må være beslutteren på behandlingen
 *  - behandlingen kan ikke allerede være godkjent
 *  - behandlingen kan ikke stå på vent
 *
 * Statusen sjekkes først, fordi saksbehandleren kan ha angret sendingen til beslutning i mellomtiden.
 * Da er beslutteren fjernet fra behandlingen, og beslutteren skal få vite at behandlingen ikke lenger er under beslutning.
 */
private fun Rammebehandling.kanUnderkjenne(utøvendeBeslutter: Saksbehandler): Either<KanIkkeUnderkjenne, Unit> {
    if (!kanTaOgUnderkjenneSomBeslutter(utøvendeBeslutter)) {
        krevBeslutterRolle(utøvendeBeslutter)
    }

    when (status) {
        UNDER_BESLUTNING -> Unit

        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> return KanIkkeUnderkjenne.RammebehandlingenErIkkeUnderBeslutning(status).left()
    }

    if (this.beslutter != utøvendeBeslutter.navIdent) {
        return KanIkkeUnderkjenne.BeslutterMåVæreTildeltRammebehandlingen.left()
    }
    if (this.attesteringer.any { it.isGodkjent() }) {
        return KanIkkeUnderkjenne.RammebehandlingenErAlleredeGodkjent.left()
    }
    if (ventestatus.erSattPåVent) {
        return KanIkkeUnderkjenne.RammebehandlingenErSattPåVent.left()
    }
    return Unit.right()
}
