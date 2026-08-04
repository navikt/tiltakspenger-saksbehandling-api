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
    val kanIkkeBehandlesGrunn: KanIkkeBehandlesGrunnDTO?,
)

enum class KanIkkeBehandlesGrunnDTO {
    HAR_ÅPEN_BEHANDLING,
    MELDEPERIODEN_HAR_IKKE_STARTET,
    INGEN_DAGER_GIR_RETT,
}

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

    val ikkeAvbrutteBehandlinger = this.meldekortbehandlinger
        .hentIkkeAvbrutteBehandlingerForKjede(kjedeId)

    val harBehandletSiste = this.meldekortbehandlinger
        .hentSisteMeldekortbehandlingForKjede(kjedeId)
        ?.let { sisteBehandling ->
            sisteBrukersMeldekort == null || sisteBehandling.sistEndret > sisteBrukersMeldekort.mottatt
        } ?: false

    val harÅpenBehandlingForKjede = this.meldekortbehandlinger.kjedeIderMedÅpenBehandling.contains(kjedeId)

    val kanIkkeBehandlesGrunn: KanIkkeBehandlesGrunnDTO? = when {
        harÅpenBehandlingForKjede -> KanIkkeBehandlesGrunnDTO.HAR_ÅPEN_BEHANDLING

        !sisteMeldeperiode.erKlarTilUtfylling(clock) -> KanIkkeBehandlesGrunnDTO.MELDEPERIODEN_HAR_IKKE_STARTET

        // Et ubehandlet meldekort fra bruker gjør at saksbehandler må kunne behandle kjeden, også når meldeperioden ikke lengre gir rett.
        brukersMeldekort.isNotEmpty() && !harBehandletSiste -> null

        sisteMeldeperiode.ingenDagerGirRett -> KanIkkeBehandlesGrunnDTO.INGEN_DAGER_GIR_RETT

        else -> null
    }

    return MeldeperiodeKjedeDTO(
        id = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        tiltaksnavn = this.rammevedtaksliste
            .valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode)
            .perioderMedVerdi.toList().map { it.verdi.typeNavn }
            .distinct(),
        sisteMeldeperiode = sisteMeldeperiode.toMeldeperiodeDTO(),
        meldekortbehandlingIder = ikkeAvbrutteBehandlinger.map { it.id.toString() },
        meldekortbehandlingStatus = ikkeAvbrutteBehandlinger.lastOrNull()?.status?.toStatusDTO(),
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
        kanBehandles = kanIkkeBehandlesGrunn == null,
        kanIkkeBehandlesGrunn = kanIkkeBehandlesGrunn,
    )
}
