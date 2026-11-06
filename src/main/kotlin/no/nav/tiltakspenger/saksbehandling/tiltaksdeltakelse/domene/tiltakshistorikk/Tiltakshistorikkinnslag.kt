package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import java.time.LocalDateTime

/**
 * Nå-tilstanden for en tiltaksdeltakelse slik den ble hentet fra tiltakshistorikk-tjenesten, og det vi utledet av den.
 * [eksternDeltakerId] er id-en deltakelsen ble slått opp med.
 * [endring] er endringen som ble utledet mot saken, og er null når det ikke fantes noen relevant endring.
 * [behandlingId] og [oppgaveId] er satt når endringen førte til en automatisk opprettet revurdering eller en Gosys-oppgave.
 */
data class Tiltakshistorikkinnslag(
    val id: TiltakshistorikkId,
    val sakId: SakId,
    val tiltaksdeltakerId: TiltaksdeltakerId,
    val eksternDeltakerId: String,
    val innhentet: LocalDateTime,
    val verdi: Tiltaksdeltakelse.GirRett,
    val endring: TiltaksdeltakerEndring?,
    val behandlingId: RammebehandlingId?,
    val oppgaveId: OppgaveId?,
) {
    init {
        require(behandlingId == null || oppgaveId == null) { "Et innslag kan ikke føre til både en behandling og en oppgave" }
    }

    val førteTilBehandlingEllerOppgave: Boolean = behandlingId != null || oppgaveId != null
}
