package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.felles.OppgaveId
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import java.time.LocalDateTime

/**
 * Command-delen av CQRS. Denne klassen representerer et meldekort som bruker har fylt ut.
 * Skal ikke brukes til lesing av data.
 *
 * Merk at vi ikke validerer disse dataene før vi lagrer de, siden vi stoler på at meldekort-api har gjort det.
 *
 * @param id Unik id (ULID/UUID) for dette meldekortet
 * @param meldeperiodeId En unik versjon av meldeperioden. Alternativ til å sende meldeperiodeKjedeId+versjon.
 */
data class NyttBrukersMeldekort(
    val id: MeldekortId,
    val mottatt: LocalDateTime,
    val meldeperiodeId: MeldeperiodeId,
    val sakId: SakId,
    val dager: List<BrukersMeldekortDag>,
    val journalpostId: JournalpostId,
    val oppgaveId: OppgaveId?,
)
