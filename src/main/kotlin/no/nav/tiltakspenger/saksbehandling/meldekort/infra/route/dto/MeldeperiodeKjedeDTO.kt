package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.BrukersMeldekortStatusDTO.BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.BrukersMeldekortStatusDTO.IKKE_MOTTATT
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.BrukersMeldekortStatusDTO.KORRIGERING_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.BrukersMeldekortStatusDTO.KORRIGERING_VENTER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.BrukersMeldekortStatusDTO.VENTER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

data class MeldeperiodeKjedeDTO(
    val id: String,
    val periode: PeriodeDTO,
    val tiltaksnavn: List<String>,
    val sisteMeldeperiode: MeldeperiodeDTO,
    val meldekortbehandlingIder: List<String>,
    val meldekortbehandlingStatus: MeldekortbehandlingStatusDTO?,
    val brukersMeldekort: List<BrukersMeldekortDTO>,
    val brukersMeldekortStatus: BrukersMeldekortStatusDTO,
    val gjeldendeBeregning: MeldeperiodeBeregningDTO?,
    val erKlarTilUtfylling: Boolean,
    val kanBehandles: Boolean,
)

fun Sak.tilMeldeperiodeKjederDTO(clock: Clock): List<MeldeperiodeKjedeDTO> {
    return this.meldeperiodeKjeder.map {
        this.tilMeldeperiodeKjedeDTO(it.kjedeId, clock)
    }
}

private fun Sak.tilMeldeperiodeKjedeDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeDTO {
    val meldeperiodeKjede = this.meldeperiodeKjeder.single { it.kjedeId == kjedeId }

    val sisteMeldeperiode = meldeperiodeKjede.siste

    val brukersMeldekort = this.brukersMeldekort
        .filter { it.kjedeId == kjedeId }
        .sortedBy { it.mottatt }

    val sisteBrukersMeldekort = brukersMeldekort.lastOrNull()

    val meldekortbehandlinger = this.meldekortbehandlinger
        .hentIkkeAvbrutteBehandlingerForKjede(kjedeId)

    val harBehandletSiste = this.meldekortbehandlinger
        .hentSisteMeldekortbehandlingForKjede(kjedeId)
        ?.let { sisteBehandling ->
            sisteBrukersMeldekort == null || sisteBehandling.sistEndret > sisteBrukersMeldekort.mottatt
        } ?: false

    return MeldeperiodeKjedeDTO(
        id = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        tiltaksnavn = this.rammevedtaksliste
            .valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode)
            .perioderMedVerdi.toList().map { it.verdi.typeNavn }
            .distinct(),
        sisteMeldeperiode = sisteMeldeperiode.toMeldeperiodeDTO(),
        meldekortbehandlingIder = meldekortbehandlinger.map { it.id.toString() },
        meldekortbehandlingStatus = meldekortbehandlinger.lastOrNull()?.status?.toStatusDTO(),
        brukersMeldekort = brukersMeldekort.map { it.toBrukersMeldekortDTO() },
        brukersMeldekortStatus = when (brukersMeldekort.size) {
            0 -> IKKE_MOTTATT
            1 -> if (harBehandletSiste) BEHANDLET else VENTER_BEHANDLING
            else -> if (harBehandletSiste) KORRIGERING_BEHANDLET else KORRIGERING_VENTER_BEHANDLING
        },
        gjeldendeBeregning = meldeperiodeBeregninger
            .hentSisteForKjedeId(kjedeId)
            ?.tilMeldeperiodeBeregningDTO(),
        erKlarTilUtfylling = sisteMeldeperiode.erKlarTilUtfylling(clock),
        kanBehandles = sisteMeldeperiode.kanBehandles(clock) || (brukersMeldekort.isNotEmpty() && !harBehandletSiste),
    )
}
