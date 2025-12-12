package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
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
    companion object {
        fun opprett(
            id: KlagebehandlingId = KlagebehandlingId.random(),
            saksnummer: Saksnummer,
            fnr: Fnr,
            opprettet: LocalDateTime,
            journalpostOpprettet: LocalDateTime,
            kommando: OpprettKlagebehandlingKommando,
        ): Klagebehandling {
            val formkrav = KlageFormkrav(
                erKlagerPartISaken = kommando.erKlagerPartISaken,
                klagesDetPåKonkreteElementerIVedtaket = kommando.klagesDetPåKonkreteElementerIVedtaket,
                erKlagefristenOverholdt = kommando.erKlagefristenOverholdt,
                erKlagenSignert = kommando.erKlagenSignert,
                vedtakDetKlagesPå = kommando.vedtakDetKlagesPå,
            )
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
    }
}
