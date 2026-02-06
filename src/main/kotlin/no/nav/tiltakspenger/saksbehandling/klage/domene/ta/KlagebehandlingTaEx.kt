package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.KanIkkeGjenopptaKlagebehandling
import java.time.Clock
import java.time.LocalDateTime

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil ta over en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.ta(
    kommando: TaKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    sistEndret: LocalDateTime,
): Either<KanIkkeTaKlagebehandling, Klagebehandling> {
    kanOppdatereIDenneStatusen(
        rammebehandlingsstatus = rammebehandlingsstatus,
        kanVæreUnderBehandling = false,
        kanVæreKlarTilBehandling = true,
    ).onLeft {
        return KanIkkeTaKlagebehandling.KanIkkeOppdateres(it).left()
    }
    // Spesialtilfelle: Dersom saksbehandler forsøker å ta fra seg selv, så endres ikke behandlingen.
    if (saksbehandler == kommando.saksbehandler.navIdent) return this.right()
    if (saksbehandler != null) return KanIkkeTaKlagebehandling.BrukOvertaIsteden.left()
    return this.copy(
        saksbehandler = kommando.saksbehandler.navIdent,
        sistEndret = sistEndret,
        status = Klagebehandlingsstatus.UNDER_BEHANDLING,
    ).right()
}
