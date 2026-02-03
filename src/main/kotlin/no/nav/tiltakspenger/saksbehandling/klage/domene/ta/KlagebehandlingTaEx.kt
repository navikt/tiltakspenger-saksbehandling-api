package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

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
fun Klagebehandling.ta(
    kommando: TaKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeTaKlagebehandling, Klagebehandling> {
    kanOppdatereIDenneStatusen(rammebehandlingsstatus).onLeft {
        return KanIkkeTaKlagebehandling.KanIkkeOppdateres(it).left()
    }
    // Spesialtilfelle: Dersom saksbehandler forsøker å ta fra seg selv, så endres ikke behandlingen.
    if (saksbehandler == kommando.saksbehandler.navIdent) return this.right()
    return this.copy(
        saksbehandler = kommando.saksbehandler.navIdent,
        sistEndret = nå(clock),
    ).right()
}
