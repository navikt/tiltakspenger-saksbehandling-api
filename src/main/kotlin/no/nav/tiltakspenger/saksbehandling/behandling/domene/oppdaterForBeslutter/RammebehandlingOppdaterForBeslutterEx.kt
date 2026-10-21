package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdaterForBeslutter

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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import java.time.Clock

/**
 * Lar beslutter oppdatere rammebehandlingen mens den er under beslutning.
 */
fun Rammebehandling.oppdaterForBeslutter(
    kommando: OppdaterRammebehandlingForBeslutterKommando,
    clock: Clock,
): Either<KanIkkeOppdatereRammebehandlingForBeslutter, Rammebehandling> {
    kanOppdatereForBeslutter(kommando.beslutter, kommando.skalJournalføreNotat).onLeft { return it.left() }

    val tidspunkt = nå(clock)
    val nyeBeslutterEndringer = beslutterEndringer.oppdater(
        gjeldendeValg = skalJournalføreNotat,
        besluttersValg = kommando.skalJournalføreNotat,
        tidspunkt = tidspunkt,
    )

    return when (this) {
        is Søknadsbehandling -> this.copy(
            skalJournalføreNotat = kommando.skalJournalføreNotat,
            beslutterEndringer = nyeBeslutterEndringer,
            sistEndret = tidspunkt,
        )

        is Revurdering -> this.copy(
            skalJournalføreNotat = kommando.skalJournalføreNotat,
            beslutterEndringer = nyeBeslutterEndringer,
            sistEndret = tidspunkt,
        )
    }.right()
}

private fun Rammebehandling.kanOppdatereForBeslutter(
    utøvendeBeslutter: Saksbehandler,
    nyttValg: SkalJournalføreNotat,
): Either<KanIkkeOppdatereRammebehandlingForBeslutter, Unit> {
    krevBeslutterRolle(utøvendeBeslutter)

    when (status) {
        UNDER_BESLUTNING -> Unit

        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> return KanIkkeOppdatereRammebehandlingForBeslutter.RammebehandlingenErIkkeUnderBeslutning(status).left()
    }

    if (this.beslutter != utøvendeBeslutter.navIdent) {
        return KanIkkeOppdatereRammebehandlingForBeslutter.BeslutterMåVæreTildeltRammebehandlingen.left()
    }
    if (this.attesteringer.any { it.isGodkjent() }) {
        return KanIkkeOppdatereRammebehandlingForBeslutter.RammebehandlingenErAlleredeGodkjent.left()
    }
    if (ventestatus.erSattPåVent) {
        return KanIkkeOppdatereRammebehandlingForBeslutter.RammebehandlingenErSattPåVent.left()
    }
    if (nyttValg.vilkårsvurdering && begrunnelseVilkårsvurdering == null) {
        return KanIkkeOppdatereRammebehandlingForBeslutter.MåHaBegrunnelseForÅJournalføreNotat.left()
    }
    if (nyttValg.barnetillegg && barnetillegg?.begrunnelse == null) {
        return KanIkkeOppdatereRammebehandlingForBeslutter.MåHaBegrunnelseForBarnetilleggForÅJournalføreNotat.left()
    }
    return Unit.right()
}
