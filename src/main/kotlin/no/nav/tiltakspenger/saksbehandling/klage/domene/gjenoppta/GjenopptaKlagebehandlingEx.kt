package no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil legge tilbake over en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.gjenoppta(
    kommando: GjenopptaKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeGjenopptaKlagebehandling, Klagebehandling> {
    kanOppdatereIDenneStatusen(rammebehandlingsstatus).onLeft {
        return KanIkkeGjenopptaKlagebehandling.KanIkkeOppdateres(it).left()
    }
    if (saksbehandler != kommando.saksbehandler.navIdent) {
        return KanIkkeGjenopptaKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = kommando.saksbehandler.navIdent,
            faktiskSaksbehandler = saksbehandler,
        ).left()
    }
    val nå = nå(clock)
    return this.copy(
        sistEndret = nå,
        ventestatus = ventestatus.gjenoppta(
            tidspunkt = nå,
            endretAv = kommando.saksbehandler.navIdent,
            status = status.toString(),
        ),
    ).right()
}
