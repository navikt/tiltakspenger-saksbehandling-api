package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Gjelder kun saksbehandler. Dersom en beslutter vil sette en omgjøring til klage på vent, må dette gjøres fra omgjøringsbehandlingen.
 * @param sjekkSaksbehandler om vi skal validere at saksbehandleren som utfører operasjonen er den samme som står på klagebehandlingen
 *  Merk - Fordi klagebhenadling ikke har noe forhold til beslutter - vil funksjonen kunne få inn en kommando med beslutter som saksbehandler.
 *      Beslutter skal få lov til å sette en behandling på vent.  Dette må da gjøres fra behandlings-eieren av klagebehandling.
 */
fun Klagebehandling.settPåVent(
    kommando: SettKlagebehandlingPåVentKommando,
    clock: Clock,
    sjekkSaksbehandler: Boolean = true,
): Either<KanIkkeSetteKlagebehandlingPåVent, Pair<Klagebehandling, Statistikkhendelser>> {
    if (this.erFerdigstilt) {
        return Pair(this, Statistikkhendelser(emptyList())).right()
    }

    kanOppdatereIDenneStatusen(null, kanVæreMottattFraKA = true, kanVæreOmgjørEtterKA = true).onLeft {
        return KanIkkeSetteKlagebehandlingPåVent.KanIkkeOppdateres(it).left()
    }
    if (sjekkSaksbehandler && saksbehandler != kommando.saksbehandler.navIdent) {
        return KanIkkeSetteKlagebehandlingPåVent.SaksbehandlerMismatch(
            forventetSaksbehandler = kommando.saksbehandler.navIdent,
            faktiskSaksbehandler = saksbehandler,
        ).left()
    }
    val nå = nå(clock)
    val oppdatertKlagebehandling = this.copy(
        saksbehandler = null,
        ventestatus = ventestatus.settPåVent(
            tidspunktSattPåVent = nå,
            endretAv = kommando.saksbehandler.navIdent,
            begrunnelse = kommando.begrunnelse,
            status = status.toString(),
            frist = kommando.frist,
        ),
        sistEndret = nå,
        status = KLAR_TIL_BEHANDLING,
    )
    val statistikkhendelser = Statistikkhendelser(
        oppdatertKlagebehandling.genererSaksstatistikk(
            hendelse = StatistikkhendelseType.BEHANDLING_SATT_PA_VENT,
        ),
    )
    return (oppdatertKlagebehandling to statistikkhendelser).right()
}
