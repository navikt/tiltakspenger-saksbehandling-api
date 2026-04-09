package no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil legge tilbake over en klagebehandling til omgjøring, må dette gjøres fra omgjøringsbehandlingen.
 */
fun Klagebehandling.leggTilbake(
    kommando: LeggTilbakeKlagebehandlingKommando,
    rammebehandlingsstatus: Rammebehandlingsstatus?,
    clock: Clock,
): Either<KanIkkeLeggeTilbakeKlagebehandling, Pair<Klagebehandling, Statistikkhendelser>> {
    if (this.erFerdigstilt) {
        return Pair(this, Statistikkhendelser(emptyList())).right()
    }

    kanOppdatereIDenneStatusen(rammebehandlingsstatus, kanVæreOmgjørEtterKA = true).onLeft {
        return KanIkkeLeggeTilbakeKlagebehandling.KanIkkeOppdateres(it).left()
    }
    if (saksbehandler != kommando.saksbehandler.navIdent) {
        return KanIkkeLeggeTilbakeKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = kommando.saksbehandler.navIdent,
            faktiskSaksbehandler = saksbehandler,
        ).left()
    }
    val oppdatertKlagebehandling = this.copy(
        saksbehandler = null,
        sistEndret = nå(clock),
        status = Klagebehandlingsstatus.KLAR_TIL_BEHANDLING,
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(OPPDATERT_SAKSBEHANDLER_BESLUTTER),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}
