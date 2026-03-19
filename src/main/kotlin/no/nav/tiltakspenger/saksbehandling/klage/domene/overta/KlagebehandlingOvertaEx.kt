package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil overta en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.overta(
    kommando: OvertaKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeOvertaKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    kanOppdatereIDenneStatusen(rammebehandlingsstatus, kanVæreOmgjørEtterKA = true).onLeft {
        return KanIkkeOvertaKlagebehandling.KanIkkeOppdateres(it).left()
    }
    // Spesialtilfelle: Dersom saksbehandler forsøker å overta fra seg selv, så endres ikke behandlingen.
    if (saksbehandler == kommando.saksbehandler.navIdent) return (this to Statistikkhendelser.empty()).right()
    if (saksbehandler == null) return KanIkkeOvertaKlagebehandling.BrukTaKlagebehandlingIsteden.left()
    val oppdatertKlagebehandling = this.copy(
        saksbehandler = kommando.saksbehandler.navIdent,
        sistEndret = nå(clock),
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}
