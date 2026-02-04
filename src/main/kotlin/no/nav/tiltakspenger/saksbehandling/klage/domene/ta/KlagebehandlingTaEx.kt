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
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil ta over en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.ta(
    kommando: TaKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeTaKlagebehandling, Klagebehandling> {
    if (status != KLAR_TIL_BEHANDLING) {
        return KanIkkeTaKlagebehandling.KanIkkeOppdateres(
            KanIkkeOppdatereKlagebehandling.FeilKlagebehandlingsstatus(
                forventetStatus = KLAR_TIL_BEHANDLING,
                faktiskStatus = status,
            ),
        ).left()
    }
    if (rammebehandlingsstatus !in listOf(null, Rammebehandlingsstatus.KLAR_TIL_BEHANDLING)) {
        return KanIkkeTaKlagebehandling.KanIkkeOppdateres(
            KanIkkeOppdatereKlagebehandling.FeilRammebehandlingssstatus(
                forventetStatus = Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
                faktiskStatus = rammebehandlingsstatus,
            ),
        ).left()
    }
    // Spesialtilfelle: Dersom saksbehandler forsøker å ta fra seg selv, så endres ikke behandlingen.
    if (saksbehandler == kommando.saksbehandler.navIdent) return this.right()
    if (saksbehandler != null) return KanIkkeTaKlagebehandling.BrukOvertaIsteden.left()
    return this.copy(
        saksbehandler = kommando.saksbehandler.navIdent,
        sistEndret = nå(clock),
        status = Klagebehandlingsstatus.UNDER_BEHANDLING,
    ).right()
}
