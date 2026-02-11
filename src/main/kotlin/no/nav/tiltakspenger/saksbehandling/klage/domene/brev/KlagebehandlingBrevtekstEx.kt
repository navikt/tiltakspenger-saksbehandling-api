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
    if (!(erAvvisning && erOpprettholdt) && !erUnderBehandling) {
        return KanIkkeOppdatereBrevtekstPåKlagebehandling.KanIkkeOppdateres.left()
    }
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeOppdatereBrevtekstPåKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }

    return when (resultat) {
        is Klagebehandlingsresultat.Avvist -> this.resultat.oppdaterBrevtekst(kommando.brevtekster).let {
            this.copy(
                sistEndret = nå(clock),
                resultat = it,
            ).right()
        }

        is Klagebehandlingsresultat.Opprettholdt -> this.resultat.oppdaterBrevtekst(kommando.brevtekster).let {
            this.copy(
                sistEndret = nå(clock),
                resultat = it,
            ).right()
        }

        is Klagebehandlingsresultat.Omgjør -> throw IllegalArgumentException("Kan ikke oppdatere brevtekst på en klagebehandling med omgjøringsresultat. Dette skjedde for klagebehandling ${this.id} og resultat ${this.resultat::class.simpleName}")

        null -> throw IllegalStateException("Klagebehandling må ha et resultat for å kunne oppdatere brevtekst. Dette skjedde for klagebehandling ${this.id} og resultat ${this.resultat}")
    }
}
