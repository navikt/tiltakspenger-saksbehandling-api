package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import java.time.Clock

/**
 * Oppdaterer saksopplysningene på behandlingen og resultatet.
 * Forutsetningene håndheves av [kanOppdatere], og feilene derfra returneres som venstre-verdi.
 * Validerer ikke om saksbehandler har tilgang til personen.
 */
fun Rammebehandling.oppdaterSaksopplysninger(
    saksbehandler: Saksbehandler,
    nyeSaksopplysninger: Saksopplysninger,
    clock: Clock,
): Either<KunneIkkeOppdatereSaksopplysninger, Rammebehandling> {
    return kanOppdatere(saksbehandler).mapLeft {
        KunneIkkeOppdatereSaksopplysninger.KunneIkkeOppdatereBehandling(it)
    }.map {
        when (this) {
            is Søknadsbehandling -> this.copy(
                saksopplysninger = nyeSaksopplysninger,
                resultat = this.resultat?.oppdaterSaksopplysninger(nyeSaksopplysninger)?.getOrElse {
                    return it.left()
                },
                sistEndret = nå(clock),
            )

            is Revurdering -> this.copy(
                saksopplysninger = nyeSaksopplysninger,
                resultat = this.resultat.oppdaterSaksopplysninger(nyeSaksopplysninger).getOrElse {
                    return it.left()
                },
                sistEndret = nå(clock),
            )
        }
    }
}

/**
 * Avgjør om [saksbehandler] kan oppdatere behandlingen.
 *
 * Betingelsene speiler hvilke tilstander oppdateringene faktisk håndterer:
 *  - saksbehandler må ha rollen saksbehandler (kaster [no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException] ellers)
 *  - behandlingen kan ikke eies av en annen saksbehandler
 *  - behandlingen må være under (manuell eller automatisk) behandling
 */
fun Rammebehandling.kanOppdatere(saksbehandler: Saksbehandler): Either<KanIkkeOppdatereBehandling, Unit> {
    krevSaksbehandlerRolle(saksbehandler)
    if (this.saksbehandler != null && this.saksbehandler != saksbehandler.navIdent) {
        return KanIkkeOppdatereBehandling.BehandlingenEiesAvAnnenSaksbehandler(this.saksbehandler!!).left()
    }
    if (!this.erUnderBehandling) {
        return KanIkkeOppdatereBehandling.MåVæreUnderBehandling.left()
    }

    return Unit.right()
}
