package no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSENDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.KanIkkeOpprettholdeKlagebehandling.AndreGrunner
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.KanIkkeOpprettholdeKlagebehandling.FeilResultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.KanIkkeOpprettholdeKlagebehandling.MåHaStatusUnderBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.KanIkkeOpprettholdeKlagebehandling.SaksbehandlerMismatch
import java.time.LocalDateTime

/**
 * Iverksetter en opprettholdelse.
 * En jobb vil plukke opp klagebehandlingen i denne tilstanden og journalføre/distribuere innstillingsbrev og oversende til klageinstansen.
 */
fun Klagebehandling.oppretthold(
    kommando: OpprettholdKlagebehandlingKommando,
): Either<KanIkkeOpprettholdeKlagebehandling, Klagebehandling> {
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    if (resultat !is Opprettholdt) {
        return FeilResultat(
            forventetResultat = Opprettholdt::class.java.simpleName,
            faktiskResultat = resultat?.javaClass?.simpleName,
        ).left()
    }
    if (!erUnderBehandling) return MåHaStatusUnderBehandling(status.toString()).left()
    if (!kanIverksetteOpprettholdelse) return AndreGrunner(kanIkkeIverksetteOpprettholdelseGrunner()).left()

    // TODO jah: Vi får ta en egen beslutning om vi skal ta saksbehandler av behandlingen.
    return this.copy(
        sistEndret = kommando.tidspunkt,
        resultat = resultat.oppdaterIverksattOpprettholdelseTidspunkt(kommando.tidspunkt),
        status = Klagebehandlingsstatus.OPPRETTHOLDT,
    ).right()
}

fun Klagebehandling.oppdaterOversendtKlageinstansenTidspunkt(tidspunkt: LocalDateTime): Klagebehandling {
    return this.copy(
        sistEndret = tidspunkt,
        resultat = (resultat as Opprettholdt).oppdaterOversendtKlageinstansenTidspunkt(tidspunkt),
        status = OVERSENDT,
    )
}
