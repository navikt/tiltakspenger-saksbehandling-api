package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningFraMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDate

/** @property korrigeringFraTidligerePeriode Korrigering på en tidligere meldeperiodekjede, som har påvirket denne kjeden,
 *  og er nyere enn siste meldekortbehandling på denne kjeden. Dvs en korrigering som har overstyrt
 *  beregningen for perioden til denne kjeden. */
data class MeldeperiodeKjedeDTO(
    val id: String,
    val periode: PeriodeDTO,
    val status: MeldeperiodeKjedeStatusDTO,
    val behandletAutomatiskStatus: BrukersMeldekortBehandletAutomatiskStatusDTO?,
    val periodeMedÅpenBehandling: PeriodeDTO?,
    val tiltaksnavn: List<String>,
    val meldeperioder: List<MeldeperiodeDTO>,
    val meldekortBehandlinger: List<MeldekortBehandlingDTO>,
    val brukersMeldekort: BrukersMeldekortDTO?,
    val korrigeringFraTidligerePeriode: MeldeperiodeKorrigeringDTO?,
    val avbrutteMeldekortBehandlinger: List<MeldekortBehandlingDTO>,
    val sisteBeregning: MeldeperiodeBeregningDTO?,
)

fun Sak.toMeldeperiodeKjedeDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeDTO {
    val meldeperiodeKjede = this.meldeperiodeKjeder.single { it.kjedeId == kjedeId }

    // TODO: denne bør skrives om litt, bør ikke gå via beregningene her
    val korrigering = meldeperiodeBeregninger.sisteBeregningPerKjede[kjedeId]?.let {
        if (it !is MeldeperiodeBeregningFraMeldekort) {
            return@let null
        }

        val forrigeBehandling = meldekortBehandlinger.hentMeldekortBehandling(it.beregnetMeldekortId)
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
        status = toMeldeperiodeKjedeStatusDTO(kjedeId, clock, brukersMeldekort),
        behandletAutomatiskStatus = brukersMeldekort?.tilBehandletAutomatiskStatusDTO(),
        periodeMedÅpenBehandling = this.meldekortBehandlinger.åpenMeldekortBehandling?.periode?.toDTO(),
        tiltaksnavn = this.vedtaksliste
            .valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode)
            .perioderMedVerdi.toList().map { it.verdi.typeNavn },
        meldeperioder = meldeperiodeKjede.map { it.toMeldeperiodeDTO() },
        meldekortBehandlinger = this.meldekortBehandlinger
            .hentMeldekortBehandlingerForKjede(meldeperiodeKjede.kjedeId)
            .map {
                // Bruker vedtaket istedenfor behandlingen dersom det finnes ett.
                this.utbetalinger.hentUtbetalingForBehandlingId(it.id)
                    ?.toMeldekortBehandlingDTO()
                    ?: it.toMeldekortBehandlingDTO(UtbetalingsstatusDTO.IKKE_GODKJENT)
            },
        brukersMeldekort = brukersMeldekort?.toBrukersMeldekortDTO(),
        korrigeringFraTidligerePeriode = korrigering,
        avbrutteMeldekortBehandlinger = this.meldekortBehandlinger
            .hentAvbrutteMeldekortBehandlingerForKjede(meldeperiodeKjede.kjedeId)
            .map {
                it.toMeldekortBehandlingDTO(
                    UtbetalingsstatusDTO.AVBRUTT,
                )
            },
        sisteBeregning = meldeperiodeBeregninger.sisteBeregningPerKjede[kjedeId]?.tilMeldeperiodeBeregningDTO(),
    )
}

fun Sak.toMeldeperiodeKjederDTO(clock: Clock): List<MeldeperiodeKjedeDTO> {
    return this.meldeperiodeKjeder.mapNotNull {
        if (it.periode.fraOgMed > LocalDate.now(clock)) {
            return@mapNotNull null
        }

        this.toMeldeperiodeKjedeDTO(it.kjedeId, clock)
    }
}
