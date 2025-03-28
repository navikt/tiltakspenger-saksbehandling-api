package no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import java.time.Clock

data class MeldeperiodeKjedeDTO(
    val id: String,
    val periode: PeriodeDTO,
    val status: MeldeperiodeKjedeStatusDTO,
    val sakHarMeldekortUnderBehandling: Boolean,
    val tiltaksnavn: List<String>,
    val meldeperioder: List<MeldeperiodeDTO>,
    val meldekortBehandlinger: List<MeldekortBehandlingDTO>,
    val brukersMeldekort: BrukersMeldekortDTO?,
)

fun Sak.toMeldeperiodeKjedeDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeDTO? {
    val meldeperiodeKjede = this.meldeperiodeKjeder.find { it.kjedeId == kjedeId } ?: return null

    return MeldeperiodeKjedeDTO(
        id = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        status = toMeldeperiodeKjedeStatusDTO(kjedeId, clock),
        sakHarMeldekortUnderBehandling = this.meldekortBehandlinger.finnesÅpenMeldekortBehandling,
        tiltaksnavn = this.vedtaksliste
            .valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode)
            .perioderMedVerdi.mapNotNull { it.verdi?.typeNavn },
        meldeperioder = meldeperiodeKjede.map { it.toDTO() },
        meldekortBehandlinger = this.meldekortBehandlinger
            .hentMeldekortBehandlingerForKjede(kjedeId)
            .map { it.toDTO() },
        brukersMeldekort = this.brukersMeldekort
            .find { it.kjedeId == kjedeId }
            ?.toDTO(),
    )
}

fun Sak.toMeldeperiodeKjederDTO(clock: Clock): List<MeldeperiodeKjedeDTO> {
    return this.meldeperiodeKjeder.map { this.toMeldeperiodeKjedeDTO(it.kjedeId, clock)!! }
}
