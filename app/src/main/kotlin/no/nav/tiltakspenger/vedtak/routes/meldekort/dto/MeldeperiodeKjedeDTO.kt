package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak

data class MeldeperiodeKjedeDTO(
    val kjedeId: String,
    val periode: PeriodeDTO,
    val tiltaksnavn: String?,
    val meldeperioder: List<MeldeperiodeDTO>,
)

fun Sak.toMeldeperiodeKjedeDTO(meldeperiodeKjedeId: MeldeperiodeKjedeId): MeldeperiodeKjedeDTO? {
    val meldeperiodeKjede = this.meldeperiodeKjeder.find { it.kjedeId == meldeperiodeKjedeId } ?: return null

    return MeldeperiodeKjedeDTO(
        kjedeId = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        tiltaksnavn = this.hentTiltaksnavnForMeldeperiode(meldeperiodeKjede.periode),
        meldeperioder = meldeperiodeKjede.map { toMeldeperiodeDTO(it) },
    )
}

fun Sak.toMeldeperiodeKjederDTO(): List<MeldeperiodeKjedeDTO> {
    return this.meldeperiodeKjeder.map { meldeperiodeKjede ->
        MeldeperiodeKjedeDTO(
            kjedeId = meldeperiodeKjede.kjedeId.toString(),
            periode = meldeperiodeKjede.periode.toDTO(),
            tiltaksnavn = this.hentTiltaksnavnForMeldeperiode(meldeperiodeKjede.periode),
            meldeperioder = meldeperiodeKjede.map { toMeldeperiodeDTO(it) },
        )
    }
}
