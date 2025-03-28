package no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldeperiodeDTO(
    val id: String,
    val versjon: Int,
    val kjedeId: String,
    val periode: PeriodeDTO,
    val opprettet: LocalDateTime,
    val antallDager: Int,
    val girRett: Map<LocalDate, Boolean>,
)

fun Meldeperiode.toMeldeperiodeDTO(): MeldeperiodeDTO = MeldeperiodeDTO(
    id = id.toString(),
    versjon = versjon.value,
    kjedeId = kjedeId.toString(),
    periode = periode.toDTO(),
    opprettet = opprettet,
    antallDager = antallDagerForPeriode,
    girRett = girRett,
)
