package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

data class MeldeperiodeKjedeDTO(
    val id: String,
    val periode: PeriodeDTO,
    val status: MeldeperiodeKjedeStatusDTO,
    val periodeMedÅpenBehandling: PeriodeDTO?,
    val tiltaksnavn: List<String>,
    val meldeperioder: List<MeldeperiodeDTO>,
    val meldekortBehandlinger: List<MeldekortBehandlingDTO>,
    val brukersMeldekort: BrukersMeldekortDTO?,
)

fun Sak.toMeldeperiodeKjedeDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeDTO {
    val meldeperiodeKjede = this.meldeperiodeKjeder.single { it.kjedeId == kjedeId }

    return MeldeperiodeKjedeDTO(
        id = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        status = toMeldeperiodeKjedeStatusDTO(kjedeId, clock),
        periodeMedÅpenBehandling = this.meldekortBehandlinger.åpenMeldekortBehandling?.periode?.toDTO(),
        tiltaksnavn = this.vedtaksliste
            .valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode)
            .perioderMedVerdi.mapNotNull { it.verdi?.typeNavn },
        meldeperioder = meldeperiodeKjede.map { it.toMeldeperiodeDTO() },
        meldekortBehandlinger = this.meldekortBehandlinger
            .hentMeldekortBehandlingerForKjede(meldeperiodeKjede.kjedeId)
            .map {
                // Bruker vedtaket istedenfor behandlingen dersom det finnes ett.
                this.utbetalinger.hentUtbetalingForBehandlingId(it.id)
                    ?.toMeldekortBehandlingDTO(this.meldeperiodeBeregninger)
                    ?: it.toMeldekortBehandlingDTO(UtbetalingsstatusDTO.IKKE_GODKJENT)
            },
        brukersMeldekort = this.brukersMeldekort
            .find { it.kjedeId == kjedeId }
            ?.toBrukersMeldekortDTO(),
    )
}

fun Sak.toMeldeperiodeKjederDTO(clock: Clock): List<MeldeperiodeKjedeDTO> {
    return this.meldeperiodeKjeder.map { this.toMeldeperiodeKjedeDTO(it.kjedeId, clock) }
}
