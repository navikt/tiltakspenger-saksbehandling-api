package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak

data class MeldeperiodeKjedeDTO(
    val kjedeId: String,
    val periode: PeriodeDTO,
    val tiltaksnavn: String?,
    val vedtaksPeriode: PeriodeDTO?,
    val meldeperioder: List<MeldeperiodeDTO>,
)

fun Sak.toMeldeperiodeKjedeDTO(meldeperiodeKjedeId: MeldeperiodeKjedeId): MeldeperiodeKjedeDTO? {
    val meldeperiodeKjede = this.meldeperiodeKjeder.find { it.id == meldeperiodeKjedeId } ?: return null

    return MeldeperiodeKjedeDTO(
        kjedeId = meldeperiodeKjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        tiltaksnavn = this.hentTiltaksnavn(),
        vedtaksPeriode = vedtaksperiode?.toDTO(),
        meldeperioder = meldeperiodeKjede.map { this.toMeldeperiodeDTO(it) },
    )
}
