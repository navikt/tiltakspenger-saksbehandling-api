package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.LocalDateTime

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil ta over en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.ta(
    kommando: TaKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    sistEndret: LocalDateTime,
): Either<KanIkkeTaKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    if (this.erFerdigstilt) {
        return Pair(this, Statistikkhendelser(emptyList())).right()
    }

    kanOppdatereIDenneStatusen(
        rammebehandlingsstatus = rammebehandlingsstatus,
        kanVæreUnderBehandling = false,
        kanVæreKlarTilBehandling = true,
        kanVæreOmgjørEtterKA = false,
    ).onLeft {
        return KanIkkeTaKlagebehandling.KanIkkeOppdateres(it).left()
    }
    // Spesialtilfelle: Dersom saksbehandler forsøker å ta fra seg selv, så endres ikke behandlingen.
    if (saksbehandler == kommando.saksbehandler.navIdent) return (this to Statistikkhendelser.empty()).right()
    if (saksbehandler != null) return KanIkkeTaKlagebehandling.BrukOvertaIsteden.left()
    val oppdatertKlagebehandling = this.copy(
        saksbehandler = kommando.saksbehandler.navIdent,
        sistEndret = sistEndret,
        status = Klagebehandlingsstatus.UNDER_BEHANDLING,
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}
