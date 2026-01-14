package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDateTime

/**
 * Representerer registrering og vurdering av en klage på et vedtak om tiltakspenger.
 * En klagebehandling har ingen beslutter, da klager avgjøres av en saksbehandler alene. Hvis det fører til medhold, vil en beslutter måtte beslutte selve revurderingen.
 * TODO jah: Vurder om vi skal arve en felles behandling. Da må vi fjerne feltene sendtTilBeslutning, beslutter og attesteringer.
 *
 */
data class Klagebehandling(
    val id: KlagebehandlingId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val saksbehandler: String?,
    val journalpostId: JournalpostId,
    val journalpostOpprettet: LocalDateTime,
    val status: Klagebehandlingsstatus,
    val resultat: Klagebehandlingsresultat?,
    val formkrav: KlageFormkrav,
) {

    val erÅpen =
        status == Klagebehandlingsstatus.KLAR_TIL_BEHANDLING || status == Klagebehandlingsstatus.UNDER_BEHANDLING

    fun oppdaterFormkrav(
        kommando: OppdaterKlagebehandlingFormkravKommando,
        journalpostOpprettet: LocalDateTime,
        clock: Clock,
    ): Either<KanIkkeOppdatereKlagebehandlingFormkrav, Klagebehandling> {
        if (saksbehandler != kommando.saksbehandler.navIdent) {
            return KanIkkeOppdatereKlagebehandlingFormkrav.SaksbehandlerMismatch(
                forventetSaksbehandler = this.saksbehandler!!,
                faktiskSaksbehandler = kommando.saksbehandler.navIdent,
            ).left()
        }
        return this.copy(
            sistEndret = nå(clock),
            formkrav = kommando.toKlageFormkrav(),
            journalpostId = kommando.journalpostId,
            journalpostOpprettet = journalpostOpprettet,
        ).right()
    }

    companion object {
        fun opprett(
            id: KlagebehandlingId = KlagebehandlingId.random(),
            saksnummer: Saksnummer,
            fnr: Fnr,
            opprettet: LocalDateTime,
            journalpostOpprettet: LocalDateTime,
            kommando: OpprettKlagebehandlingKommando,
        ): Klagebehandling {
            val formkrav = kommando.toKlageFormkrav()
            return Klagebehandling(
                id = id,
                sakId = kommando.sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                sistEndret = opprettet,
                saksbehandler = kommando.saksbehandler.navIdent,
                formkrav = formkrav,
                journalpostOpprettet = journalpostOpprettet,
                journalpostId = kommando.journalpostId,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                resultat = if (formkrav.erAvvisning) Klagebehandlingsresultat.AVVIST else null,
            )
        }
    }

    init {
        if (formkrav.erAvvisning) {
            require(resultat == Klagebehandlingsresultat.AVVIST) {
                "Klagebehandling som er avvist må ha resultat satt til AVVIST"
            }
        } else {
            require(resultat == null) {
                "Klagebehandling som ikke er avvist kan ikke ha resultat satt ved opprettelse"
            }
        }
        when (status) {
            Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Klagebehandling som er $status kan ikke ha saksbehandler satt"
                }
            }

            Klagebehandlingsstatus.UNDER_BEHANDLING -> {
                require(saksbehandler != null) {
                    "Klagebehandling som er $status må ha saksbehandler satt"
                }
            }
        }
    }
}
