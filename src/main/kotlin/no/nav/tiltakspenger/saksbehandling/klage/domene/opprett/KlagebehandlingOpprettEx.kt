package no.nav.tiltakspenger.saksbehandling.klage.domene.opprett

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import java.time.LocalDateTime

fun Klagebehandling.Companion.opprett(
    id: KlagebehandlingId = KlagebehandlingId.random(),
    saksnummer: Saksnummer,
    fnr: Fnr,
    opprettet: LocalDateTime,
    journalpostOpprettet: LocalDateTime,
    kommando: OpprettKlagebehandlingKommando,
    behandlingDetKlagesPå: BehandlingId?,
): Klagebehandling {
    val formkrav = kommando.toKlageFormkrav(behandlingDetKlagesPå)
    return Klagebehandling(
        id = id,
        sakId = kommando.sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        opprettet = opprettet,
        sistEndret = opprettet,
        saksbehandler = kommando.saksbehandler.navIdent,
        formkrav = formkrav,
        klagensJournalpostOpprettet = journalpostOpprettet,
        klagensJournalpostId = kommando.journalpostId,
        status = UNDER_BEHANDLING,
        resultat = if (formkrav.erAvvisning) Klagebehandlingsresultat.Avvist.empty else null,
        iverksattTidspunkt = null,
        avbrutt = null,
        ventestatus = Ventestatus(),
    )
}
