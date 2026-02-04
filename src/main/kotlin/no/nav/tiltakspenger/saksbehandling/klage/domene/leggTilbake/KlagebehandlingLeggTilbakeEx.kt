package no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil legge tilbake over en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.leggTilbake(
    kommando: LeggTilbakeKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeLeggeTilbakeKlagebehandling, Klagebehandling> {
    kanOppdatereIDenneStatusen(rammebehandlingsstatus).onLeft {
        return KanIkkeLeggeTilbakeKlagebehandling.KanIkkeOppdateres(it).left()
    }
    if (saksbehandler != kommando.saksbehandler.navIdent) {
        return KanIkkeLeggeTilbakeKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = kommando.saksbehandler.navIdent,
            faktiskSaksbehandler = saksbehandler,
        ).left()
    }
    return this.copy(
        saksbehandler = null,
        sistEndret = nå(clock),
        status = Klagebehandlingsstatus.KLAR_TIL_BEHANDLING,
    ).right()
}
