package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak

data class MeldeperiodeOversiktDTO(
    // TODO jah: Rename til meldeperiodeKjedeId?
    val meldeperiodeId: String,
    val periode: PeriodeDTO,
    val tiltaksnavn: String?,
    val vedtaksPeriode: PeriodeDTO?,
    val meldeperioder: List<MeldeperiodeDTO>,
)

fun Sak.toMeldeperiodeOversiktDTO(meldeperiodeKjedeId: MeldeperiodeKjedeId): MeldeperiodeOversiktDTO? {
    val meldeperiodeKjede = this.meldeperiodeKjeder.find { it.id == meldeperiodeKjedeId } ?: return null

    return MeldeperiodeOversiktDTO(
        meldeperiodeId = meldeperiodeKjedeId.toString(),
        periode = meldeperiodeKjede.periode.toDTO(),
        tiltaksnavn = this.hentTiltaksnavn(),
        vedtaksPeriode = vedtaksperiode?.toDTO(),
        meldeperioder = meldeperiodeKjede.map { this.toMeldeperiodeDTO(it) },
    )
}
