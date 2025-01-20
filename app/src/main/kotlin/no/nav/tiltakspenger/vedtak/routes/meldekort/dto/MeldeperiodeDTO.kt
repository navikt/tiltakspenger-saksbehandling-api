package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldeperiodeDTO(
    val id: String,
    val hendelseId: String,
    val versjon: String,
    val periode: PeriodeDTO,
    val opprettet: LocalDateTime,
    val antallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
    val brukersMeldekort: List<BrukersMeldekortDTO>,
    val behandlinger: List<MeldekortBehandlingDTO>,
)

data class MeldeperiodeEnkelDTO(
    val id: String,
    val hendelseId: String,
    val versjon: String,
    val periode: PeriodeDTO,
    val opprettet: LocalDateTime,
    val antallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
)

fun Meldeperiode.toEnkelDTO(): MeldeperiodeEnkelDTO {
    return MeldeperiodeEnkelDTO(
        id = this.id.toString(),
        hendelseId = this.hendelseId.toString(),
        versjon = this.versjon.toString(),
        periode = this.periode.toDTO(),
        opprettet = this.opprettet,
        antallDagerForPeriode = this.antallDagerForPeriode,
        girRett = this.girRett,
    )
}

fun MeldeperiodeKjeder.toDTO(): List<MeldeperiodeEnkelDTO> =
    this.meldeperioder.map {
        it.toEnkelDTO()
    }

fun Sak.toMeldeperiodeDTO(meldeperiode: Meldeperiode): MeldeperiodeDTO {
    return MeldeperiodeDTO(
        id = meldeperiode.id.toString(),
        hendelseId = meldeperiode.hendelseId.toString(),
        versjon = meldeperiode.versjon.toString(),
        periode = meldeperiode.periode.toDTO(),
        opprettet = meldeperiode.opprettet,
        antallDagerForPeriode = meldeperiode.antallDagerForPeriode,
        girRett = meldeperiode.girRett,
        brukersMeldekort = listOf(),
        behandlinger = listOf(),
    )
}
