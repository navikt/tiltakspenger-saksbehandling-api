package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldeperiodeDTO(
    val id: String,
    val hendelseId: String,
    val versjon: Int,
    val periode: PeriodeDTO,
    val opprettet: LocalDateTime,
    val status: MeldeperiodeStatusDTO,
    val antallDager: Int,
    val girRett: Map<LocalDate, Boolean>,
    val brukersMeldekort: BrukersMeldekortDTO?,
    val meldekortBehandling: MeldekortBehandlingDTO?,
)

fun Sak.toMeldeperiodeDTO(meldeperiode: Meldeperiode): MeldeperiodeDTO {
    return MeldeperiodeDTO(
        id = meldeperiode.meldeperiodeKjedeId.toString(),
        hendelseId = meldeperiode.hendelseId.toString(),
        versjon = meldeperiode.versjon.value,
        periode = meldeperiode.periode.toDTO(),
        opprettet = meldeperiode.opprettet,
        status = this.toMeldeperiodeStatusDTO(meldeperiode),
        antallDager = meldeperiode.antallDagerForPeriode,
        girRett = meldeperiode.girRett,
        meldekortBehandling = this.meldekortBehandlinger.hentMeldekortForKjedeId(meldeperiode.meldeperiodeKjedeId)?.toDTO(null),
        // TODO: hent brukers meldekort!
        brukersMeldekort = null,
    )
}
