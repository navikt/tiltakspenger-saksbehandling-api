package no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import java.time.Clock
import java.time.LocalDateTime

fun Klagebehandling.oppdaterFormkrav(
    kommando: OppdaterKlagebehandlingFormkravKommando,
    journalpostOpprettet: LocalDateTime,
    clock: Clock,
): Either<KanIkkeOppdatereFormkravPåKlagebehandling, Klagebehandling> {
    if (!erUnderBehandling) return KanIkkeOppdatereFormkravPåKlagebehandling.KanIkkeOppdateres.left()
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeOppdatereFormkravPåKlagebehandling.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler!!,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    val oppdaterteFormkrav = kommando.toKlageFormkrav()
    val tidligereResultat = this.resultat
    val harTilknyttetRammebehandling =
        this.resultat is Klagebehandlingsresultat.Omgjør && this.resultat.rammebehandlingId != null

    if (oppdaterteFormkrav.erAvvisning && harTilknyttetRammebehandling) {
        return KanIkkeOppdatereFormkravPåKlagebehandling.KanIkkeEndreTilAvvisningNårTilknyttetRammebehandling.left()
    }

    return this.copy(
        sistEndret = nå(clock),
        formkrav = oppdaterteFormkrav,
        klagensJournalpostId = kommando.journalpostId,
        klagensJournalpostOpprettet = journalpostOpprettet,
        resultat = when {
            oppdaterteFormkrav.erAvvisning && tidligereResultat is Klagebehandlingsresultat.Avvist -> tidligereResultat
            oppdaterteFormkrav.erAvvisning -> Klagebehandlingsresultat.Avvist.empty
            resultat is Klagebehandlingsresultat.Omgjør && oppdaterteFormkrav.erOppfyllt -> this.resultat
            else -> null
        },
    ).right()
}
