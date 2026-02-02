package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil ta over en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.overta(
    kommando: OvertaKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeOvertaKlagebehandling, Klagebehandling> {
    kanOppdatereIDenneStatusen(rammebehandlingsstatus).onLeft {
        return KanIkkeOvertaKlagebehandling.KanIkkeOppdateres(it).left()
    }
    // Spesialtilfelle: Dersom saksbehandler forsøker å overta fra seg selv, så endres ikke behandlingen.
    if (saksbehandler == kommando.saksbehandler.navIdent) return this.right()
    return this.copy(
        saksbehandler = kommando.saksbehandler.navIdent,
        sistEndret = nå(clock),
    ).right()
}
