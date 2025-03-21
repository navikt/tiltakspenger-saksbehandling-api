package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.felles.OppgaveId
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import java.time.LocalDateTime

/**
 * Command-delen av CQRS. Denne klassen representerer et meldekort som bruker har fylt ut.
 * Skal ikke brukes til lesing av data.
 *
 * Merk at vi ikke validerer disse dataene før vi lagrer de, siden vi stoler på at meldekort-api har gjort det.
 *
 * @param id Unik id (ULID/UUID) for dette meldekortet
 * @param meldeperiodeId En unik versjon av meldeperioden. Alternativ til å sende kjedeId+versjon.
 */
data class LagreBrukersMeldekortKommando(
    val id: MeldekortId,
    val mottatt: LocalDateTime,
    val meldeperiodeId: MeldeperiodeId,
    val sakId: SakId,
    val dager: List<BrukersMeldekortDag>,
    val journalpostId: JournalpostId,
    val oppgaveId: OppgaveId?,
)
