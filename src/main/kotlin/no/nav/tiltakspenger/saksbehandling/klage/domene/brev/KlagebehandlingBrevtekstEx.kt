package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import java.time.Clock

fun Klagebehandling.oppdaterBrevtekst(
    kommando: KlagebehandlingBrevKommando,
    clock: Clock,
): Either<KanIkkeOppdatereBrevtekstPåKlagebehandling, Klagebehandling> {
    if (!erAvvisning || !erUnderBehandling) {
        return KanIkkeOppdatereBrevtekstPåKlagebehandling.KanIkkeOppdateres.left()
    }
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeOppdatereBrevtekstPåKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    return (resultat as Klagebehandlingsresultat.Avvist).oppdaterBrevtekst(kommando.brevtekster).let {
        this.copy(
            sistEndret = nå(clock),
            resultat = it,
        ).right()
    }
}
