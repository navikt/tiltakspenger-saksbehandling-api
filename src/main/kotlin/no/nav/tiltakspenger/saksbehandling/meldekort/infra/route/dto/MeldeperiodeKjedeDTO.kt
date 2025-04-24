package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

/** @property korrigeringFraTidligerePeriode Korrigering på en tidligere meldeperiodekjede, som har påvirket denne kjeden,
 *  og er nyere enn siste meldekortbehandling på denne kjeden. Dvs en korrigering som har overstyrt
 *  beregningen for perioden til denne kjeden. */
data class MeldeperiodeKjedeDTO(
    val id: String,
    val periode: PeriodeDTO,
    val status: MeldeperiodeKjedeStatusDTO,
    val automatiskBehandlingStatus: BrukersMeldekortBehandletAutomatiskStatusDTO?,
    val periodeMedÅpenBehandling: PeriodeDTO?,
    val tiltaksnavn: List<String>,
    val meldeperioder: List<MeldeperiodeDTO>,
    val meldekortBehandlinger: List<MeldekortBehandlingDTO>,
    val brukersMeldekort: BrukersMeldekortDTO?,
    val korrigeringFraTidligerePeriode: MeldeperiodeKorrigeringDTO?,
)

fun Sak.toMeldeperiodeKjedeDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeDTO {
    val meldeperiodeKjede = this.meldeperiodeKjeder.single { it.kjedeId == kjedeId }
    val korrigering = meldeperiodeBeregninger.sisteBeregningForKjede[kjedeId]?.let {
        val forrigeBehandling = meldekortBehandlinger.hentMeldekortBehandling(it.beregningMeldekortId)
        if (forrigeBehandling !is MeldekortBehandletManuelt) {
            return@let null
        }

        if (forrigeBehandling.kjedeId == kjedeId) {
            null
        } else {
            forrigeBehandling.tilMeldeperiodeKorrigeringDTO(it.kjedeId)
        }
    }

    val brukersMeldekort = this.brukersMeldekort
        .find { it.kjedeId == kjedeId }

    return MeldeperiodeKjedeDTO(
        id = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        status = toMeldeperiodeKjedeStatusDTO(kjedeId, clock),
        automatiskBehandlingStatus = brukersMeldekort?.tilBehandletAutomatiskStatusDTO(),
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
                    ?.toMeldekortBehandlingDTO()
                    ?: it.toMeldekortBehandlingDTO(UtbetalingsstatusDTO.IKKE_GODKJENT)
            },
        brukersMeldekort = brukersMeldekort ?.toBrukersMeldekortDTO(),
        korrigeringFraTidligerePeriode = korrigering,
    )
}

fun Sak.toMeldeperiodeKjederDTO(clock: Clock): List<MeldeperiodeKjedeDTO> {
    return this.meldeperiodeKjeder.map { this.toMeldeperiodeKjedeDTO(it.kjedeId, clock) }
}
