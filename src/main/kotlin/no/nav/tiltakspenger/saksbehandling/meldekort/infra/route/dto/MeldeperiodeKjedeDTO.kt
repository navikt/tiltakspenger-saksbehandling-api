package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeKorrigeringDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
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
    val periodeMedÅpenBehandling: PeriodeDTO?,
    val tiltaksnavn: List<String>,
    val meldeperioder: List<MeldeperiodeDTO>,
    val meldekortBehandlinger: List<MeldekortBehandlingDTO>,
    val brukersMeldekort: List<BrukersMeldekortDTO>,
    val korrigeringFraTidligerePeriode: MeldeperiodeKorrigeringDTO?,
    val avbrutteMeldekortBehandlinger: List<MeldekortBehandlingDTO>,
    val sisteBeregning: MeldeperiodeBeregningDTO?,
)

fun Sak.toMeldeperiodeKjedeDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeDTO {
    val meldeperiodeKjede = this.meldeperiodeKjeder.single { it.kjedeId == kjedeId }

    // TODO: denne bør skrives om litt, bør ikke gå via beregningene her
    val korrigering = meldeperiodeBeregninger.gjeldendeBeregningPerKjede[kjedeId]?.let {
        if (it.beregningKilde !is BeregningKilde.BeregningKildeMeldekort) {
            return@let null
        }

        val forrigeBehandling = meldekortbehandlinger.hentMeldekortBehandling(it.beregningKilde.id)
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
        .filter { it.kjedeId == kjedeId }
        .sortedBy { it.mottatt }

    return MeldeperiodeKjedeDTO(
        id = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        status = toMeldeperiodeKjedeStatusDTO(kjedeId, clock),
        periodeMedÅpenBehandling = this.meldekortbehandlinger.åpenMeldekortBehandling?.periode?.toDTO(),
        tiltaksnavn = this.rammevedtaksliste
            .valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode)
            .perioderMedVerdi.toList().map { it.verdi.typeNavn },
        meldeperioder = meldeperiodeKjede.map { it.toMeldeperiodeDTO() },
        meldekortBehandlinger = this.meldekortbehandlinger
            .hentMeldekortBehandlingerForKjede(meldeperiodeKjede.kjedeId)
            .map {
                it.tilMeldekortBehandlingDTO(
                    this.meldekortvedtaksliste.hentForMeldekortBehandling(it.id),
                    beregninger = this.meldeperiodeBeregninger,
                )
            },
        brukersMeldekort = brukersMeldekort.map { it.toBrukersMeldekortDTO() },
        korrigeringFraTidligerePeriode = korrigering,
        avbrutteMeldekortBehandlinger = this.meldekortbehandlinger
            .hentAvbrutteMeldekortBehandlingerForKjede(meldeperiodeKjede.kjedeId)
            .map { it.tilMeldekortBehandlingDTO(beregninger = this.meldeperiodeBeregninger) },
        sisteBeregning = meldeperiodeBeregninger.gjeldendeBeregningPerKjede[kjedeId]?.tilMeldeperiodeBeregningDTO(),
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
