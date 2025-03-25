package no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import java.time.Clock

data class MeldeperiodeKjedeDTO(
    val kjedeId: String,
    val periode: PeriodeDTO,
    val tiltaksnavn: List<String>,
    val meldeperioder: List<MeldeperiodeDTO>,
)

fun Sak.toMeldeperiodeKjedeDTO(kjedeId: MeldeperiodeKjedeId, clock: Clock): MeldeperiodeKjedeDTO? {
    val meldeperiodeKjede = this.meldeperiodeKjeder.find { it.kjedeId == kjedeId } ?: return null

    return MeldeperiodeKjedeDTO(
        kjedeId = meldeperiodeKjede.kjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        tiltaksnavn = this.vedtaksliste.valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode).perioderMedVerdi.mapNotNull { it.verdi?.typeNavn },
        meldeperioder = meldeperiodeKjede.map { toMeldeperiodeDTO(it, clock) },
    )
}

fun Sak.toMeldeperiodeKjederDTO(clock: Clock): List<MeldeperiodeKjedeDTO> {
    return this.meldeperiodeKjeder.map { meldeperiodeKjede ->
        MeldeperiodeKjedeDTO(
            kjedeId = meldeperiodeKjede.kjedeId.toString(),
            periode = meldeperiodeKjede.periode.toDTO(),
            tiltaksnavn = this.vedtaksliste.valgteTiltaksdeltakelserForPeriode(meldeperiodeKjede.periode).perioderMedVerdi.mapNotNull { it.verdi?.typeNavn },
            meldeperioder = meldeperiodeKjede.map { toMeldeperiodeDTO(it, clock) },
        )
    }
}
