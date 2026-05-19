package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.v2

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.BrukersMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toBrukersMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.toStatusDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDate

data class MeldeperiodeKjedeDTOV2(
    val id: String,
    val periode: PeriodeDTO,
    val tiltaksnavn: List<String>,
    val meldeperioder: List<MeldeperiodeDTO>,
    val meldekortbehandlingIder: List<String>,
    val meldekortbehandlingStatus: MeldekortbehandlingStatusDTO?,
    val brukersMeldekort: List<BrukersMeldekortDTO>,
    val brukersMeldekortStatus: BrukersMeldekortStatusDTO,
    val gjeldendeBeregning: MeldeperiodeBeregningDTO?,
)

fun Sak.tilMeldeperiodeKjedeDTOV2(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeDTOV2 {
    val meldeperiodeKjede = this.meldeperiodeKjeder.single { it.kjedeId == kjedeId }

    val brukersMeldekort = this.brukersMeldekort
        .filter { it.kjedeId == kjedeId }
        .sortedBy { it.mottatt }

    val meldekortbehandlinger = this.meldekortbehandlinger
        .hentIkkeAvbrutteBehandlingerForKjede(meldeperiodeKjede.kjedeId)

    return MeldeperiodeKjedeDTOV2(
        id = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        tiltaksnavn = this.rammevedtaksliste
            .valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode)
            .perioderMedVerdi.toList().map { it.verdi.typeNavn },
        meldeperioder = meldeperiodeKjede.map { it.toMeldeperiodeDTO() },
        meldekortbehandlingIder = meldekortbehandlinger.map { it.id.toString() },
        meldekortbehandlingStatus = meldekortbehandlinger.lastOrNull()?.status?.toStatusDTO(),
        brukersMeldekort = brukersMeldekort.map { it.toBrukersMeldekortDTO() },
        // TODO: behandlet statuser
        brukersMeldekortStatus = when (brukersMeldekort.size) {
            0 -> BrukersMeldekortStatusDTO.IKKE_MOTTATT
            1 -> BrukersMeldekortStatusDTO.VENTER_BEHANDLING
            else -> BrukersMeldekortStatusDTO.KORRIGERING_VENTER_BEHANDLING
        },
        gjeldendeBeregning = meldeperiodeBeregninger
            .hentSisteForKjedeId(kjedeId)
            ?.tilMeldeperiodeBeregningDTO(),
    )
}

fun Sak.tilMeldeperiodeKjederDTOV2(clock: Clock): List<MeldeperiodeKjedeDTOV2> {
    return this.meldeperiodeKjeder.mapNotNull {
        if (it.periode.fraOgMed > LocalDate.now(clock)) {
            return@mapNotNull null
        }

        this.tilMeldeperiodeKjedeDTOV2(it.kjedeId, clock)
    }
}
