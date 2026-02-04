package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil sette en omgjøring til klage på vent, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.settPåVent(
    kommando: SettKlagebehandlingPåVentKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeSetteKlagebehandlingPåVent, Klagebehandling> {
    kanOppdatereIDenneStatusen(rammebehandlingsstatus).onLeft {
        return KanIkkeSetteKlagebehandlingPåVent.KanIkkeOppdateres(it).left()
    }
    if (saksbehandler != kommando.saksbehandler.navIdent) {
        return KanIkkeSetteKlagebehandlingPåVent.SaksbehandlerMismatch(
            forventetSaksbehandler = kommando.saksbehandler.navIdent,
            faktiskSaksbehandler = saksbehandler,
        ).left()
    }
    val nå = nå(clock)
    return this.copy(
        saksbehandler = null,
        ventestatus = ventestatus.settPåVent(
            tidspunkt = nå,
            endretAv = kommando.saksbehandler.navIdent,
            begrunnelse = kommando.begrunnelse,
            status = status.toString(),
        ),
        sistEndret = nå,
        status = KLAR_TIL_BEHANDLING,
    ).right()
}
