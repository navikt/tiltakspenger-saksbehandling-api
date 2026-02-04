package no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil legge tilbake over en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 * Har samme logikk som for rammebehandling og meldekortbehandling, som er at du kan gjenoppta fra en annen saksbehandler uten og tildele den til deg selv først.
 */
fun Klagebehandling.gjenopptaKlagebehandling(
    kommando: GjenopptaKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeGjenopptaKlagebehandling, Klagebehandling> {
    kanOppdatereIDenneStatusen(
        rammebehandlingsstatus = rammebehandlingsstatus,
        kanVæreUnderBehandling = true,
        kanVæreKlarTilBehandling = true,
    ).onLeft {
        return KanIkkeGjenopptaKlagebehandling.KanIkkeOppdateres(it).left()
    }
    if (!ventestatus.erSattPåVent) {
        return KanIkkeGjenopptaKlagebehandling.MåVæreSattPåVent.left()
    }
    val nå = nå(clock)
    return this.copy(
        sistEndret = nå,
        ventestatus = ventestatus.gjenoppta(
            tidspunkt = nå,
            endretAv = kommando.saksbehandler.navIdent,
            status = status.toString(),
        ),
        saksbehandler = kommando.saksbehandler.navIdent,
        status = UNDER_BEHANDLING,
    ).right()
}
