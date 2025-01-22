package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldeperiodeDto(
    val id: String,
    val hendelseId: String,
    val versjon: String,
    val periode: PeriodeDTO,
    val opprettet: LocalDateTime,
    val antallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
)

fun MeldeperiodeKjeder.toDTO(): List<MeldeperiodeDto> =
    this.map {
        it.last().toDTO()
    }

fun Meldeperiode.toDTO(): MeldeperiodeDto {
    return MeldeperiodeDto(
        id = this.id.toString(),
        hendelseId = this.hendelseId.toString(),
        versjon = this.versjon.toString(),
        periode = this.periode.toDTO(),
        opprettet = this.opprettet,
        antallDagerForPeriode = this.antallDagerForPeriode,
        girRett = this.girRett,
    )
}
